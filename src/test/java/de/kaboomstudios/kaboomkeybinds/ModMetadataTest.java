package de.kaboomstudios.kaboomkeybinds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Guards the mod metadata that Fabric Loader reads at startup. A typo in one of these files only shows up
 * when the game is launched, so the unit tests check the processed resources instead.
 */
class ModMetadataTest {
    private static JsonObject readJson(String resource) throws IOException {
        try (InputStream in = ModMetadataTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, "resource missing on the test class path: " + resource);
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void modJsonMatchesConstantsAndIsClientOnly() throws IOException {
        JsonObject modJson = readJson("/fabric.mod.json");

        assertEquals(KaBoomKeybinds.MOD_ID, modJson.get("id").getAsString());
        assertEquals("client", modJson.get("environment").getAsString());
        assertFalse(modJson.get("version").getAsString().contains("${"), "version placeholder was not expanded");
    }

    @Test
    void fabricApiIsNotARequiredDependency() throws IOException {
        JsonObject depends = readJson("/fabric.mod.json").getAsJsonObject("depends");

        assertFalse(depends.has("fabric-api"), "players must only need Fabric Loader and this jar");
        assertTrue(depends.has("fabricloader"));
        assertTrue(depends.has("minecraft"));
        assertFalse(depends.get("minecraft").getAsString().contains("${"),
                "Minecraft version placeholder was not expanded");
    }

    @Test
    void entryPointClassExists() throws IOException {
        JsonObject entrypoints = readJson("/fabric.mod.json").getAsJsonObject("entrypoints");
        String className = entrypoints.getAsJsonArray("client").get(0).getAsString();

        assertTrue(classExists(className), "entry point class not found: " + className);
    }

    @Test
    void mixinConfigIsReferencedAndPointsToMixinPackage() throws IOException {
        JsonObject modJson = readJson("/fabric.mod.json");
        String configName = modJson.getAsJsonArray("mixins").get(0).getAsString();
        JsonObject mixinConfig = readJson("/" + configName);

        assertEquals("de.kaboomstudios.kaboomkeybinds.mixin", mixinConfig.get("package").getAsString());
        assertEquals("JAVA_25", mixinConfig.get("compatibilityLevel").getAsString());
        assertTrue(mixinConfig.get("required").getAsBoolean());
    }

    @Test
    void listedMixinClassesExist() throws IOException {
        JsonObject mixinConfig = readJson("/kaboomkeybinds.mixins.json");
        String mixinPackage = mixinConfig.get("package").getAsString();

        assertFalse(mixinConfig.getAsJsonArray("client").isEmpty(), "the mod needs at least one mixin to work");
        for (JsonElement mixin : mixinConfig.getAsJsonArray("client")) {
            String className = mixinPackage + "." + mixin.getAsString();
            assertTrue(classExists(className), "mixin class not found: " + className);
        }
    }

    @Test
    void languageFilesHaveTheSameKeys() throws IOException {
        JsonObject english = readJson("/assets/kaboomkeybinds/lang/en_us.json");
        JsonObject german = readJson("/assets/kaboomkeybinds/lang/de_de.json");

        assertEquals(english.keySet(), german.keySet(), "en_us and de_de must translate the same keys");
    }

    /**
     * The jar must run with Fabric Loader alone. Fabric API is on the development class path, so a stray import would
     * compile fine and only fail for players. The compiled main classes are therefore scanned for references to its
     * packages. Fabric Loader itself lives in net.fabricmc.api and net.fabricmc.loader, which are allowed.
     */
    @Test
    void mainClassesDoNotReferenceFabricApi() throws Exception {
        Path classes = Paths.get(KaBoomKeybinds.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        assertTrue(Files.isDirectory(classes), "expected the directory with the main classes, got " + classes);

        try (Stream<Path> files = Files.walk(classes)) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".class"))::iterator) {
                String content = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
                assertFalse(content.contains("net/fabricmc/fabric/"),
                        "Fabric API reference in " + classes.relativize(file));
            }
        }
    }

    private static boolean classExists(String className) {
        try {
            Class.forName(className, false, ModMetadataTest.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
