package de.kaboomstudios.kaboomkeybinds.client;

import com.mojang.blaze3d.platform.InputConstants;
import de.kaboomstudios.kaboomkeybinds.KaBoomKeybinds;
import de.kaboomstudios.kaboomkeybinds.core.KeyPair;
import de.kaboomstudios.kaboomkeybinds.core.KeySlot;
import de.kaboomstudios.kaboomkeybinds.core.SecondaryKeyStore;
import java.nio.file.Path;
//? if <26.3
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
//? if <26.3
import net.minecraft.util.Util;
import org.slf4j.Logger;

/**
 * Bridge between the version independent {@link SecondaryKeyStore} and Minecraft's key mappings. The store works
 * with plain strings, this class translates them to {@link InputConstants.Key} objects and back using Minecraft's
 * own key names, so stored names stay valid as long as Minecraft keeps its naming scheme.
 *
 * <p>Every change made through this class is written to the file at once and re-registers the key lookup table,
 * so the new key works immediately and survives a restart.
 */
public final class AlternativeKeys {
    private static final Logger LOGGER = KaBoomKeybinds.LOGGER;
    // Since 26.3 Minecraft reads the keyboard through SDL instead of GLFW. Key names still stand for the same physical
    // keys, except for three whose old name now means another key, one that desktop systems hardly ever report:
    // the keypad decimal point (GLFW KP_DECIMAL, SDL KP_PERIOD; the old name is now SDL KP_DECIMAL), the context menu
    // key (GLFW MENU, SDL APPLICATION; the old name is now SDL MENU, a separate key on a few Linux keyboards) and the
    // extra key next to left shift on ISO keyboards, "<" on German ones (GLFW reports it as WORLD_2 on Windows, SDL as
    // NONUSBACKSLASH, which Minecraft names world.1; world.2 is now SDL NONUSHASH). The build for 26.3 renames files
    // written by older versions once when they are loaded and marks them with the SDL scheme. The builds for older
    // versions rename marked files back, so that one game folder can be shared between 26.3 and older versions, see
    // SecondaryKeyStore.migrateKeyNames and revertKeyNames.
    private static final String SDL_KEY_NAMES = "sdl";
    /** GLFW name to SDL name of the keys that have another name since 26.3. */
    private static final Map<String, String> SDL_RENAMED_KEYS = Map.of(
            "key.keyboard.keypad.decimal", "key.keyboard.keypad.period",
            "key.keyboard.menu", "key.keyboard.application",
            "key.keyboard.world.2", "key.keyboard.world.1");
    private static SecondaryKeyStore store;

    private AlternativeKeys() {
    }

    /** The mod's own file inside the game's config directory. */
    public static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve(SecondaryKeyStore.FILE_NAME);
    }

    /**
     * The store, loaded on first use. Key mappings are created while the game options are constructed, before any
     * mod entry point runs, so the store cannot be loaded from the entry point. Key names that a Minecraft version
     * with the other naming scheme wrote for a key that is named differently here are renamed and saved with the
     * next change.
     */
    public static synchronized SecondaryKeyStore store() {
        if (store == null) {
            store = SecondaryKeyStore.load(file());
            //? if >=26.3 {
            /*store.migrateKeyNames(SDL_KEY_NAMES, SDL_RENAMED_KEYS);
            *///?} else {
            store.revertKeyNames(SDL_KEY_NAMES, glfwNamesOfRenamedKeys());
            //?}
        }
        return store;
    }

    //? if <26.3 {
    // SDL name to GLFW name, the table above read backwards for this platform. GLFW names the ISO key world.2 on
    // Windows only, on Linux and macOS it is world.1 as in SDL and keeps its name. SDL's menu, the separate Linux key,
    // has no GLFW name of its own and keeps its name as well, which GLFW reads as the context menu key.
    private static Map<String, String> glfwNamesOfRenamedKeys() {
        Map<String, String> glfwNames = new HashMap<>();
        SDL_RENAMED_KEYS.forEach((glfwName, sdlName) -> glfwNames.put(sdlName, glfwName));
        if (Util.getPlatform() != Util.OS.WINDOWS) {
            glfwNames.remove("key.keyboard.world.1");
        }
        return glfwNames;
    }
    //?}

    /**
     * The stored alternative key for a key mapping name. {@link InputConstants#UNKNOWN} if nothing is stored or the
     * stored name is not a key Minecraft knows. An unknown name stays in the file untouched, it only has no effect.
     */
    public static InputConstants.Key stored(String mappingName) {
        return store().get(mappingName).map(keyName -> parse(mappingName, keyName)).orElse(InputConstants.UNKNOWN);
    }

    private static InputConstants.Key parse(String mappingName, String keyName) {
        try {
            return InputConstants.getKey(keyName);
        } catch (IllegalArgumentException | StringIndexOutOfBoundsException e) {
            // InputConstants.getKey throws for names outside its scheme and for unparsable key numbers.
            LOGGER.warn("Ignoring alternative key \"{}\" for {}: not a key name Minecraft knows", keyName, mappingName);
            return InputConstants.UNKNOWN;
        }
    }

    /** The mapping seen through the interface its mixin implements. */
    public static AlternativeKeyMapping of(KeyMapping mapping) {
        return (AlternativeKeyMapping) mapping;
    }

    /** The alternative key of a mapping, {@link InputConstants#UNKNOWN} when none is bound. */
    public static InputConstants.Key get(KeyMapping mapping) {
        return of(mapping).kaboomkeybinds$getAlternativeKey();
    }

    /** Both keys of a mapping, unbound slots as {@code null}. */
    public static KeyPair<InputConstants.Key> keys(KeyMapping mapping) {
        return of(mapping).kaboomkeybinds$keys();
    }

    /**
     * Binds the alternative key of a mapping, saves the file and updates the key lookup table. {@code null} or
     * {@link InputConstants#UNKNOWN} unbinds. If the key is the mapping's primary key, the primary key is unbound,
     * because one key must not trigger the same mapping twice.
     */
    public static void setAlternative(KeyMapping mapping, InputConstants.Key key) {
        apply(mapping, keys(mapping).with(KeySlot.SECONDARY, orNull(key)));
    }

    /**
     * Binds the primary key of a mapping with the same rule in the other direction: if the key is the mapping's
     * alternative key, the alternative key is unbound. The primary key itself is still saved by Minecraft in its
     * options file, only the alternative key goes to the mod's file.
     */
    public static void setPrimary(KeyMapping mapping, InputConstants.Key key) {
        apply(mapping, keys(mapping).with(KeySlot.PRIMARY, orNull(key)));
    }

    /** Unbinds the alternative key of a mapping and saves the file. */
    public static void clearAlternative(KeyMapping mapping) {
        setAlternative(mapping, null);
    }

    /**
     * Unbinds the alternative keys of all given mappings, used by "Reset All" in the controls menu. The file is
     * written once and the lookup table rebuilt once, instead of once per mapping. Entries of mappings that are not
     * present (for example from a mod that is not installed) stay in the file. Entries of present mappings are
     * removed even if their stored key name could not be read, because the player asked for a clean slate.
     */
    public static void clearAll(Iterable<KeyMapping> mappings) {
        boolean changed = false;
        for (KeyMapping mapping : mappings) {
            of(mapping).kaboomkeybinds$setAlternativeKey(null);
            changed |= store().remove(mapping.getName());
        }
        if (changed) {
            store().save();
        }
        KeyMapping.resetMapping();
    }

    private static void apply(KeyMapping mapping, KeyPair<InputConstants.Key> keys) {
        mapping.setKey(orUnknown(keys.primary()));
        of(mapping).kaboomkeybinds$setAlternativeKey(keys.secondary());
        String keyName = keys.secondary() == null ? null : keys.secondary().getName();
        if (store().set(mapping.getName(), keyName)) {
            store().save();
        }
        // Rebuilds the lookup table from the primary keys and, through the mixin, the alternative keys.
        KeyMapping.resetMapping();
    }

    private static InputConstants.Key orNull(InputConstants.Key key) {
        return key == null || InputConstants.UNKNOWN.equals(key) ? null : key;
    }

    private static InputConstants.Key orUnknown(InputConstants.Key key) {
        return key == null ? InputConstants.UNKNOWN : key;
    }
}
