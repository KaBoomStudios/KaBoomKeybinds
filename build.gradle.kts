plugins {
    id("net.fabricmc.fabric-loom")
}

val minecraftVersion = providers.gradleProperty("minecraft_version").get()
val fabricApiVersion = providers.gradleProperty("fabric_api_version").get()

// The Minecraft version is appended as build metadata so that jars for several game versions can live side by side,
// for example kaboomkeybinds-0.1.0+26.2.jar next to a later kaboomkeybinds-0.1.0+26.3.jar.
version = "${providers.gradleProperty("mod_version").get()}+$minecraftVersion"
group = providers.gradleProperty("maven_group").get()

base {
    archivesName = providers.gradleProperty("archives_base_name")
}

repositories {
    // Loom registers the Minecraft and Fabric repositories itself. Nothing else is needed.
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
}

fabricApi {
    configureTests {
        // Client game tests live in src/gametest and run through the "runClientGameTest" task.
        createSourceSet = true
        modId = "kaboomkeybinds-gametest"
        // Server side game tests make no sense for a client only mod.
        enableGameTests = false
        // Required for the automated client to start. By enabling this you agree to the Minecraft EULA.
        eula = true
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

    // Fabric API is deliberately not a compile dependency of the mod: players only need Fabric Loader and this jar.
    // It is loaded in the development client so that other mods' keybinds can be tried, and the game test
    // source set compiles against it for the client game test framework.
    runtimeOnly("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    "gametestImplementation"("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    testImplementation(platform("org.junit:junit-bom:${providers.gradleProperty("junit_version").get()}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<ProcessResources>().configureEach {
    val modVersion = project.version.toString()
    inputs.property("version", modVersion)

    filesMatching("fabric.mod.json") {
        expand("version" to modVersion)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    options.encoding = "UTF-8"
}

tasks.test {
    useJUnitPlatform()

    // Minecraft's logging configuration is on the test class path and writes logs/latest.log relative to the
    // working directory. Running the tests inside the build directory keeps that output out of the project root.
    val testWorkingDir = layout.buildDirectory.dir("test-run")
    workingDir = testWorkingDir.get().asFile
    doFirst {
        testWorkingDir.get().asFile.mkdirs()
    }

    testLogging {
        events("passed", "skipped", "failed")
    }
}
