package de.kaboomstudios.kaboomkeybinds;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * Guards the mod metadata that Fabric Loader reads at startup. A typo in one of these files only shows up
 * when the game is launched, so the unit tests check the processed resources instead.
 */
class ModMetadataTest {
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

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
        for (String configName : new String[] {"/kaboomkeybinds.mixins.json", "/kaboomkeybinds.controlling.mixins.json"}) {
            JsonObject mixinConfig = readJson(configName);
            String mixinPackage = mixinConfig.get("package").getAsString();

            assertFalse(mixinConfig.getAsJsonArray("client").isEmpty(), configName + " lists no mixins");
            for (JsonElement mixin : mixinConfig.getAsJsonArray("client")) {
                String className = mixinPackage + "." + mixin.getAsString();
                assertTrue(classExists(className), "mixin class not found: " + className);
            }
        }
    }

    /**
     * The mixins for the Controlling mod must never stop the game: they apply only when Controlling is installed (the
     * plugin decides), and a Controlling version they no longer fit must not crash it. Mixin crashes for a failing
     * mixin of a required configuration and for an injector that misses its required number of matches.
     */
    @Test
    void controllingMixinsAreOptional() throws IOException {
        JsonObject modJson = readJson("/fabric.mod.json");
        boolean listed = false;
        for (JsonElement config : modJson.getAsJsonArray("mixins")) {
            listed |= config.getAsString().equals("kaboomkeybinds.controlling.mixins.json");
        }
        assertTrue(listed, "fabric.mod.json lists the Controlling mixin configuration");

        JsonObject mixinConfig = readJson("/kaboomkeybinds.controlling.mixins.json");
        assertFalse(mixinConfig.get("required").getAsBoolean(), "a failing Controlling mixin must not stop the game");
        assertEquals(0, mixinConfig.getAsJsonObject("injectors").get("defaultRequire").getAsInt(),
                "an injector that misses a changed Controlling must not stop the game");
        String plugin = mixinConfig.get("plugin").getAsString();
        assertTrue(classExists(plugin), "mixin plugin not found: " + plugin);
        // The Controlling package lies inside the main mixin package, so checking the outer one covers both.
        String mainMixinPackage = readJson("/kaboomkeybinds.mixins.json").get("package").getAsString();
        assertTrue(mixinConfig.get("package").getAsString().startsWith(mainMixinPackage + "."));
        assertFalse(plugin.startsWith(mainMixinPackage + "."), "Mixin refuses to load classes from a mixin package");
    }

    /**
     * Mod lists in the game and Modrinth show the license. The SPDX identifier in fabric.mod.json must name the license
     * that the LICENSE file in the project root grants, which the build also puts into the jar.
     */
    @Test
    void licenseMatchesLicenseFile() throws IOException {
        JsonObject modJson = readJson("/fabric.mod.json");
        assertEquals("MIT", modJson.get("license").getAsString());

        String licenseFile = System.getProperty("kaboomkeybinds.licenseFile");
        assertNotNull(licenseFile, "the build passes the path of the LICENSE file");
        List<String> lines = Files.readAllLines(Path.of(licenseFile), StandardCharsets.UTF_8);
        assertEquals("MIT License", lines.getFirst());
        String author = modJson.getAsJsonArray("authors").get(0).getAsString();
        assertTrue(lines.stream().anyMatch(line -> line.matches("Copyright \\(c\\) \\d{4} " + Pattern.quote(author))),
                "the copyright line names the author from fabric.mod.json");
    }

    @Test
    void contactLinksPointToTheRepository() throws IOException {
        JsonObject contact = readJson("/fabric.mod.json").getAsJsonObject("contact");
        String sources = contact.get("sources").getAsString();

        assertTrue(sources.startsWith("https://github.com/"), "unexpected source link: " + sources);
        assertEquals(sources + "/issues", contact.get("issues").getAsString());
    }

    /**
     * Mod lists in the game show the icon. Fabric Loader expects a square PNG at the given path inside the jar and
     * recommends a power of two for its size.
     */
    @Test
    void iconIsASquarePng() throws IOException {
        String icon = readJson("/fabric.mod.json").get("icon").getAsString();
        byte[] data;
        try (InputStream in = ModMetadataTest.class.getResourceAsStream("/" + icon)) {
            assertNotNull(in, "icon missing on the test class path: " + icon);
            data = in.readAllBytes();
        }

        assertArrayEquals(PNG_SIGNATURE, Arrays.copyOf(data, PNG_SIGNATURE.length), "icon is not a PNG file");
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
        assertNotNull(image, "icon cannot be decoded");
        assertEquals(image.getWidth(), image.getHeight(), "icon is not square");
        assertEquals(1, Integer.bitCount(image.getWidth()), "icon size is not a power of two: " + image.getWidth());
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
