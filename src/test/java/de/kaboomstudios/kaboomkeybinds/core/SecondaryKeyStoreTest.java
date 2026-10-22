package de.kaboomstudios.kaboomkeybinds.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SecondaryKeyStoreTest {
    @TempDir
    Path dir;

    private Path file() {
        return dir.resolve(SecondaryKeyStore.FILE_NAME);
    }

    private Path brokenFile() {
        return dir.resolve(SecondaryKeyStore.FILE_NAME + SecondaryKeyStore.BROKEN_SUFFIX);
    }

    private void writeFile(String content) throws IOException {
        Files.writeString(file(), content, StandardCharsets.UTF_8);
    }

    private String readFile() throws IOException {
        return Files.readString(file(), StandardCharsets.UTF_8);
    }

    private void assertNoLeftovers() throws IOException {
        try (var files = Files.list(dir)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")), "temporary file left behind");
        }
    }

    @Test
    void missingFileYieldsAnEmptyStore() {
        SecondaryKeyStore store = SecondaryKeyStore.load(file());

        assertTrue(store.all().isEmpty());
        assertEquals(Optional.empty(), store.get("key.jump"));
        assertEquals(file(), store.file());
    }

    @Test
    void entriesSurviveSaveAndLoad() throws IOException {
        SecondaryKeyStore store = SecondaryKeyStore.load(file());
        assertTrue(store.set("key.inventory", "key.mouse.5"));
        assertTrue(store.set("key.jump", "key.keyboard.v"));
        assertTrue(store.save());

        SecondaryKeyStore reloaded = SecondaryKeyStore.load(file());

        assertEquals(Map.of("key.inventory", "key.mouse.5", "key.jump", "key.keyboard.v"), reloaded.all());
        assertEquals(Optional.of("key.mouse.5"), reloaded.get("key.inventory"));
        assertEquals(Optional.empty(), reloaded.get("key.sneak"));
        assertFalse(Files.exists(brokenFile()));
        assertNoLeftovers();
    }

    @Test
    void writtenFileHasTheDocumentedFormatWithSortedEntries() throws IOException {
        SecondaryKeyStore store = SecondaryKeyStore.empty(file());
        store.set("key.sneak", "key.keyboard.c");
        store.set("key.attack", "key.mouse.4");
        store.save();

        String content = readFile();
        JsonObject root = JsonParser.parseString(content).getAsJsonObject();

        assertEquals(SecondaryKeyStore.FORMAT_VERSION, root.get("version").getAsInt());
        assertEquals("key.mouse.4", root.getAsJsonObject("keys").get("key.attack").getAsString());
        assertEquals("key.keyboard.c", root.getAsJsonObject("keys").get("key.sneak").getAsString());
        assertTrue(content.indexOf("key.attack") < content.indexOf("key.sneak"), "entries are sorted by name");
    }

    @Test
    void saveCreatesMissingDirectories() {
        Path nested = dir.resolve("config").resolve("nested").resolve(SecondaryKeyStore.FILE_NAME);
        SecondaryKeyStore store = SecondaryKeyStore.empty(nested);
        store.set("key.jump", "key.keyboard.v");

        assertTrue(store.save());
        assertEquals(Optional.of("key.keyboard.v"), SecondaryKeyStore.load(nested).get("key.jump"));
    }

    @Test
    void setReportsWhetherSomethingChanged() {
        SecondaryKeyStore store = SecondaryKeyStore.empty(file());

        assertTrue(store.set("key.jump", "key.keyboard.v"));
        assertFalse(store.set("key.jump", "key.keyboard.v"), "same value again");
        assertTrue(store.set("key.jump", "key.keyboard.b"));
        assertTrue(store.remove("key.jump"));
        assertFalse(store.remove("key.jump"), "already removed");
        assertFalse(store.set("key.jump", null), "removing a missing entry");
    }

    @Test
    void blankOrNullKeyNameRemovesTheEntry() {
        SecondaryKeyStore store = SecondaryKeyStore.empty(file());
        store.set("key.jump", "key.keyboard.v");
        store.set("key.sneak", "key.keyboard.c");

        assertTrue(store.set("key.jump", null));
        assertTrue(store.set("key.sneak", "  "));

        assertTrue(store.all().isEmpty());
    }

    @Test
    void unknownMappingsAreKeptAcrossSaves() throws IOException {
        writeFile("{\"version\": 1, \"keys\": {\"key.othermod.zoom\": \"key.keyboard.z\"}}");

        SecondaryKeyStore store = SecondaryKeyStore.load(file());
        store.set("key.jump", "key.keyboard.v");
        store.save();

        SecondaryKeyStore reloaded = SecondaryKeyStore.load(file());
        assertEquals(Map.of("key.othermod.zoom", "key.keyboard.z", "key.jump", "key.keyboard.v"), reloaded.all());
    }

    @Test
    void truncatedFileIsKeptAsideOnTheNextSave() throws IOException {
        String broken = "{\"version\": 1, \"keys\": {\"key.jump\": \"key.keyboard.v\"";
        writeFile(broken);

        SecondaryKeyStore store = SecondaryKeyStore.load(file());
        assertTrue(store.all().isEmpty(), "nothing is read from a broken file");
        assertFalse(Files.exists(brokenFile()), "the file is only moved when something is written");

        store.set("key.sneak", "key.keyboard.c");
        assertTrue(store.save());

        assertEquals(broken, Files.readString(brokenFile(), StandardCharsets.UTF_8));
        assertEquals(Optional.of("key.keyboard.c"), SecondaryKeyStore.load(file()).get("key.sneak"));
        assertNoLeftovers();
    }

    @Test
    void emptyFileCountsAsBroken() throws IOException {
        writeFile("");

        SecondaryKeyStore store = SecondaryKeyStore.load(file());
        assertTrue(store.all().isEmpty());
        store.save();

        assertTrue(Files.exists(brokenFile()));
    }

    @Test
    void fileWithoutAnObjectAtTheTopCountsAsBroken() throws IOException {
        writeFile("[1, 2, 3]");

        SecondaryKeyStore store = SecondaryKeyStore.load(file());
        assertTrue(store.all().isEmpty());
        store.save();

        assertEquals("[1, 2, 3]", Files.readString(brokenFile(), StandardCharsets.UTF_8));
    }

    @Test
    void keysThatAreNotAnObjectCountAsBroken() throws IOException {
        writeFile("{\"version\": 1, \"keys\": [\"key.jump\"]}");

        SecondaryKeyStore store = SecondaryKeyStore.load(file());
        assertTrue(store.all().isEmpty());
        store.save();

        assertTrue(Files.exists(brokenFile()));
    }

    @Test
    void unreadableVersionCountsAsBroken() throws IOException {
        writeFile("{\"version\": \"one\", \"keys\": {\"key.jump\": \"key.keyboard.v\"}}");

        SecondaryKeyStore store = SecondaryKeyStore.load(file());
        assertTrue(store.all().isEmpty());
        store.save();

        assertTrue(Files.exists(brokenFile()));
    }

    @Test
    void fileWithoutKeysIsIncompleteButNotBroken() throws IOException {
        writeFile("{\"version\": 1}");

        SecondaryKeyStore store = SecondaryKeyStore.load(file());
        assertTrue(store.all().isEmpty());
        store.set("key.jump", "key.keyboard.v");
        store.save();

        assertFalse(Files.exists(brokenFile()), "nothing worth keeping");
        assertEquals(Optional.of("key.keyboard.v"), SecondaryKeyStore.load(file()).get("key.jump"));
    }

    @Test
    void fileWithoutVersionIsRead() throws IOException {
        writeFile("{\"keys\": {\"key.jump\": \"key.keyboard.v\"}}");

        SecondaryKeyStore store = SecondaryKeyStore.load(file());

        assertEquals(Optional.of("key.keyboard.v"), store.get("key.jump"));
    }

    @Test
    void newerFormatIsReadAsFarAsPossibleAndKeptAside() throws IOException {
        String newer = "{\"version\": 2, \"keys\": {\"key.jump\": \"key.keyboard.v\"}, \"future\": true}";
        writeFile(newer);

        SecondaryKeyStore store = SecondaryKeyStore.load(file());
        assertEquals(Optional.of("key.keyboard.v"), store.get("key.jump"));
        store.save();

        assertEquals(newer, Files.readString(brokenFile(), StandardCharsets.UTF_8));
        JsonObject root = JsonParser.parseString(readFile()).getAsJsonObject();
        assertEquals(SecondaryKeyStore.FORMAT_VERSION, root.get("version").getAsInt());
    }

    @Test
    void entriesWithInvalidValuesAreSkipped() throws IOException {
        writeFile("{\"version\": 1, \"keys\": {"
                + "\"key.number\": 5,"
                + "\"key.blank\": \"\","
                + "\"key.object\": {\"x\": 1},"
                + "\"key.array\": [\"key.keyboard.v\"],"
                + "\"key.null\": null,"
                + "\"key.bool\": true,"
                + "\"key.valid\": \"key.mouse.5\""
                + "}}");

        SecondaryKeyStore store = SecondaryKeyStore.load(file());

        assertEquals(Map.of("key.valid", "key.mouse.5"), store.all());
        store.save();
        assertFalse(Files.exists(brokenFile()), "invalid entries do not make the whole file broken");
    }

    @Test
    void saveReportsFailureWhenTheFileCannotBeWritten() throws IOException {
        Path blocker = dir.resolve("blocker");
        Files.writeString(blocker, "not a directory");
        SecondaryKeyStore store = SecondaryKeyStore.empty(blocker.resolve(SecondaryKeyStore.FILE_NAME));
        store.set("key.jump", "key.keyboard.v");

        assertFalse(store.save());
    }

    @Test
    void repeatedSavesReplaceTheFileCleanly() throws IOException {
        SecondaryKeyStore store = SecondaryKeyStore.empty(file());
        store.set("key.jump", "key.keyboard.v");
        assertTrue(store.save());
        store.set("key.jump", "key.keyboard.b");
        assertTrue(store.save());

        assertEquals(Optional.of("key.keyboard.b"), SecondaryKeyStore.load(file()).get("key.jump"));
        assertNoLeftovers();
    }

    @Test
    void allIsAReadOnlyView() {
        SecondaryKeyStore store = SecondaryKeyStore.empty(file());
        store.set("key.jump", "key.keyboard.v");
        Map<String, String> view = store.all();

        store.set("key.sneak", "key.keyboard.c");

        assertEquals(2, view.size(), "the view follows later changes");
        assertThrows(UnsupportedOperationException.class, () -> view.put("x", "y"));
    }
}
