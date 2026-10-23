package de.kaboomstudios.kaboomkeybinds.core;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import de.kaboomstudios.kaboomkeybinds.KaBoomKeybinds;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import org.slf4j.Logger;

/**
 * The alternative keys of all key mappings, persisted in the mod's own JSON file. The vanilla options file is
 * never touched, so removing the mod leaves the normal controls as they are.
 *
 * <p>File format, version 1:
 * <pre>
 * {
 *   "version": 1,
 *   "keyNames": "sdl",
 *   "keys": {
 *     "key.inventory": "key.mouse.5"
 *   }
 * }
 * </pre>
 * Entries map a key mapping name to a Minecraft key name. The store treats both as opaque strings: it keeps entries
 * for mappings that do not currently exist (for example from a mod that was removed) so they return when that mod is
 * installed again, and it leaves the meaning of a key name to the client layer, which resolves it with Minecraft.
 * The optional {@code keyNames} entry names the key naming scheme the entries follow, see
 * {@link #migrateKeyNames(String, Map)}. Files without it follow the scheme of Minecraft 26.2 and older.
 *
 * <p>A file that cannot be read never crashes the game. The store logs a warning, starts empty and moves the
 * unreadable file aside as {@code kaboomkeybinds.json.broken} before it writes for the first time, so nothing is lost.
 * Saving writes a temporary file next to the target and moves it into place atomically.
 */
public final class SecondaryKeyStore {
    /** File name inside the game's config directory. */
    public static final String FILE_NAME = "kaboomkeybinds.json";
    /** Suffix of the copy that is kept when a file could not be used. */
    public static final String BROKEN_SUFFIX = ".broken";
    /** Version written by this build. Files with a higher version are read as far as understood and kept aside. */
    public static final int FORMAT_VERSION = 1;

    private static final String VERSION_FIELD = "version";
    private static final String KEY_NAMES_FIELD = "keyNames";
    private static final String KEYS_FIELD = "keys";
    private static final String TEMP_SUFFIX = ".tmp";
    private static final Logger LOGGER = KaBoomKeybinds.LOGGER;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final Path file;
    /** Sorted so that the written file is stable and easy to compare. */
    private final SortedMap<String, String> keys = new TreeMap<>();
    /** Key naming scheme of the entries, {@code null} when the file names none. Kept as read until a migration. */
    private String keyNames;
    /** Set when the existing file could not be used. It is moved aside before the next save overwrites it. */
    private boolean keepExistingFileAside;

    private SecondaryKeyStore(Path file) {
        this.file = file;
    }

    /** A store without entries that will write to the given file. */
    public static SecondaryKeyStore empty(Path file) {
        return new SecondaryKeyStore(Objects.requireNonNull(file, "file"));
    }

    /**
     * Reads the file. Never throws: a missing file yields an empty store, an unusable file yields an empty store
     * with a warning in the log, invalid entries are skipped with a warning.
     */
    public static SecondaryKeyStore load(Path file) {
        SecondaryKeyStore store = empty(file);
        if (!Files.exists(file)) {
            LOGGER.info("No alternative key file at {}, starting without alternative keys", file);
            return store;
        }

        JsonElement root;
        try (Reader reader = Files.newBufferedReader(file, UTF_8)) {
            root = JsonParser.parseReader(reader);
        } catch (IOException | JsonParseException e) {
            store.markUnusable("could not be read", e);
            return store;
        }

        if (!root.isJsonObject()) {
            store.markUnusable("does not contain a JSON object", null);
            return store;
        }
        JsonObject object = root.getAsJsonObject();

        JsonElement version = object.get(VERSION_FIELD);
        if (version != null) {
            if (!isInteger(version)) {
                store.markUnusable("has an unreadable \"" + VERSION_FIELD + "\" entry", null);
                return store;
            }
            if (version.getAsInt() > FORMAT_VERSION) {
                LOGGER.warn("{} was written by a newer version of the mod (format {}). Reading what is understood,"
                        + " the original is kept as {}{} on the next save", file, version.getAsInt(),
                        file.getFileName(), BROKEN_SUFFIX);
                store.keepExistingFileAside = true;
            }
        }

        JsonElement keyNames = object.get(KEY_NAMES_FIELD);
        if (keyNames != null) {
            if (isString(keyNames) && !keyNames.getAsString().isBlank()) {
                store.keyNames = keyNames.getAsString();
            } else {
                LOGGER.warn("Ignoring \"{}\" entry in {}: expected the name of a key naming scheme, found {}",
                        KEY_NAMES_FIELD, file, keyNames);
            }
        }

        JsonElement keysElement = object.get(KEYS_FIELD);
        if (keysElement == null) {
            LOGGER.warn("{} has no \"{}\" entry, starting without alternative keys", file, KEYS_FIELD);
            return store;
        }
        if (!keysElement.isJsonObject()) {
            store.markUnusable("has a \"" + KEYS_FIELD + "\" entry that is not an object", null);
            return store;
        }

        for (Map.Entry<String, JsonElement> entry : keysElement.getAsJsonObject().entrySet()) {
            JsonElement value = entry.getValue();
            if (!isString(value) || value.getAsString().isBlank()) {
                LOGGER.warn("Ignoring entry \"{}\" in {}: expected a key name, found {}", entry.getKey(), file, value);
                continue;
            }
            store.keys.put(entry.getKey(), value.getAsString());
        }
        LOGGER.info("Loaded {} alternative key(s) from {}", store.keys.size(), file);
        return store;
    }

    /** The file this store reads from and writes to. */
    public Path file() {
        return file;
    }

    /** The alternative key name for a key mapping name, if one is stored. */
    public Optional<String> get(String mappingName) {
        return Optional.ofNullable(keys.get(mappingName));
    }

    /**
     * Stores the alternative key name for a key mapping name. A {@code null} or blank key name removes the entry.
     *
     * @return whether the store changed
     */
    public boolean set(String mappingName, String keyName) {
        Objects.requireNonNull(mappingName, "mappingName");
        if (keyName == null || keyName.isBlank()) {
            return remove(mappingName);
        }
        return !keyName.equals(keys.put(mappingName, keyName));
    }

    /**
     * Removes the entry for a key mapping name.
     *
     * @return whether the store changed
     */
    public boolean remove(String mappingName) {
        return keys.remove(mappingName) != null;
    }

    /** All entries, sorted by key mapping name. The view is read only and reflects later changes. */
    public Map<String, String> all() {
        return Collections.unmodifiableMap(keys);
    }

    /** The key naming scheme the entries follow, empty if the file names none. */
    public Optional<String> keyNames() {
        return Optional.ofNullable(keyNames);
    }

    /**
     * Renames key names written under an older naming scheme, for game versions that gave some keys new names. Runs
     * once per file: a store that already follows {@code scheme} is left as it is, so a name written under the new
     * scheme is never renamed, even if it equals an old name. Afterwards the store follows {@code scheme}.
     *
     * <p>Nothing is written here. The new names and the scheme reach the file with the next save, so a game version
     * that still uses the old names can go on reading the file as long as nothing is changed.
     *
     * @param scheme the naming scheme of the running game
     * @param renames old key name to new key name
     * @return whether an entry was renamed
     */
    public boolean migrateKeyNames(String scheme, Map<String, String> renames) {
        Objects.requireNonNull(scheme, "scheme");
        Objects.requireNonNull(renames, "renames");
        if (scheme.equals(keyNames)) {
            return false;
        }
        boolean renamed = false;
        for (Map.Entry<String, String> entry : keys.entrySet()) {
            String newName = renames.get(entry.getValue());
            if (newName != null) {
                LOGGER.info("Renaming alternative key {} of {} to {}, the key names changed with this game version",
                        entry.getValue(), entry.getKey(), newName);
                entry.setValue(newName);
                renamed = true;
            }
        }
        keyNames = scheme;
        return renamed;
    }

    /**
     * Writes the store to its file. The content goes to a temporary file first, which then replaces the target
     * atomically, so a crash while writing cannot leave a half written file behind.
     *
     * @return whether the file was written; failures are logged
     */
    public boolean save() {
        Path temp = file.resolveSibling(file.getFileName() + TEMP_SUFFIX);
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (keepExistingFileAside) {
                moveAside();
                keepExistingFileAside = false;
            }
            try (Writer writer = Files.newBufferedWriter(temp, UTF_8)) {
                GSON.toJson(toJson(), writer);
                writer.write(System.lineSeparator());
            }
            replace(temp, file);
            return true;
        } catch (IOException | JsonIOException e) {
            LOGGER.error("Could not save alternative keys to {}", file, e);
            deleteQuietly(temp);
            return false;
        }
    }

    private JsonObject toJson() {
        JsonObject keysObject = new JsonObject();
        keys.forEach(keysObject::addProperty);
        JsonObject root = new JsonObject();
        root.addProperty(VERSION_FIELD, FORMAT_VERSION);
        if (keyNames != null) {
            root.addProperty(KEY_NAMES_FIELD, keyNames);
        }
        root.add(KEYS_FIELD, keysObject);
        return root;
    }

    private void markUnusable(String problem, Exception cause) {
        keepExistingFileAside = true;
        String message = "{} {}. Starting without alternative keys, the file is kept as {}{} on the next save";
        if (cause == null) {
            LOGGER.warn(message, file, problem, file.getFileName(), BROKEN_SUFFIX);
        } else {
            LOGGER.warn(message, file, problem, file.getFileName(), BROKEN_SUFFIX, cause);
        }
    }

    private void moveAside() {
        Path broken = file.resolveSibling(file.getFileName() + BROKEN_SUFFIX);
        try {
            Files.move(file, broken, REPLACE_EXISTING);
            LOGGER.warn("Kept the unusable alternative key file as {}", broken);
        } catch (NoSuchFileException e) {
            // Nothing to keep, the file disappeared since it was read.
        } catch (IOException e) {
            LOGGER.warn("Could not keep the unusable alternative key file as {}, it will be overwritten", broken, e);
        }
    }

    private static void replace(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, ATOMIC_MOVE, REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            // Some file systems cannot move atomically. A plain replace is still better than no save at all.
            Files.move(temp, target, REPLACE_EXISTING);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            LOGGER.debug("Could not delete {}", path, e);
        }
    }

    private static boolean isString(JsonElement element) {
        return element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
    }

    private static boolean isInteger(JsonElement element) {
        if (!element.isJsonPrimitive()) {
            return false;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (!primitive.isNumber()) {
            return false;
        }
        try {
            primitive.getAsInt();
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
