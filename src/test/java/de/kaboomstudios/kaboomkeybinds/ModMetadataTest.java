package de.kaboomstudios.kaboomkeybinds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
    void languageFilesHaveTheSameKeys() throws IOException {
        JsonObject english = readJson("/assets/kaboomkeybinds/lang/en_us.json");
        JsonObject german = readJson("/assets/kaboomkeybinds/lang/de_de.json");

        assertEquals(english.keySet(), german.keySet(), "en_us and de_de must translate the same keys");
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
