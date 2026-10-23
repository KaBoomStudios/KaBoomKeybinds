plugins {
    id("net.fabricmc.fabric-loom")
}

// This script is shared by all Minecraft versions: Stonecutter runs it once for every subproject in versions/.
// Settings that are the same for all versions come from gradle.properties in the project root, version specific
// ones from versions/<version>/gradle.properties. Gradle exposes the latter as project properties only, not through
// providers.gradleProperty(), which is why all settings are read with property().
fun setting(name: String): String = property(name).toString()

val minecraftVersion = sc.current.version
val modVersion = setting("mod_version")
val fabricApiVersion = setting("fabric_api_version")

// The Minecraft version is appended as build metadata so that jars for several game versions can live side by side,
// for example kaboomkeybinds-1.0.0+26.2.jar next to kaboomkeybinds-1.0.0+26.3.jar.
version = "$modVersion+$minecraftVersion"
group = setting("maven_group")

base {
    archivesName = setting("archives_base_name")
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

loom {
    runs.named("client") {
        // The development clients of all Minecraft versions share one folder in the project root, so options,
        // worlds and the alternative key file carry over when switching versions.
        runDirectory = rootDir.resolve("run")
    }
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
    implementation("net.fabricmc:fabric-loader:${setting("loader_version")}")

    // Fabric API is deliberately not a compile dependency of the mod: players only need Fabric Loader and this jar.
    // It is loaded in the development client so that other mods' keybinds can be tried, and the game test
    // source set compiles against it for the client game test framework.
    runtimeOnly("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
    "gametestImplementation"("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    testImplementation(platform("org.junit:junit-bom:${setting("junit_version")}"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<ProcessResources>().configureEach {
    val properties = mapOf(
        "version" to project.version.toString(),
        "minecraft_dependency" to setting("minecraft_dependency"),
    )
    inputs.properties(properties)

    filesMatching("fabric.mod.json") {
        expand(properties)
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

// The client game tests check that alternative keys are loaded at startup, so the test client starts with a
// prepared file in its config directory. The file is copied before every run to undo changes made by the tests.
// It binds three keys that were renamed with Minecraft 26.3 by the names of the other naming scheme, which tests both
// directions of the conversion: 26.3 starts with GLFW names written by older versions, the older versions start with
// SDL names written by 26.3.
// Loom applies the run directory only when the task executes, so it is taken from the run configuration here.
// Only plain files are captured by the task action, which keeps the configuration cache happy.
val gameTestFixture = rootDir.resolve(
    "src/gametest/fixtures/" + if (sc.current.parsed >= "26.3") "glfw-key-names.json" else "sdl-key-names.json")
tasks.configureEach {
    if (name == "runClientGameTest") {
        val fixture = gameTestFixture
        val runDir = loom.runs.getByName("clientGameTest").runDirectory.get().asFile
        inputs.file(fixture)
        doFirst {
            val configDir = runDir.resolve("config")
            configDir.mkdirs()
            fixture.copyTo(configDir.resolve("kaboomkeybinds.json"), overwrite = true)
        }
    }
}

// Loom clears the game test run directory through "deleteGameTestRunDir" and hooks that task into "configureLaunch",
// which every run task depends on. A plain "runClient" would therefore wipe the screenshots of the last game test run.
// A clean directory is only needed before the game tests themselves, so the delete task is skipped in every other build.
val gameTestTaskPath = "$path:runClientGameTest"
gradle.taskGraph.whenReady {
    val runsClientGameTests = hasTask(gameTestTaskPath)
    tasks.matching { it.name == "deleteGameTestRunDir" }.configureEach {
        onlyIf("the client game tests are part of this build") { runsClientGameTests }
    }
}

// Builds and tests this version and copies its jar to build/libs/<mod version>/ in the project root, next to the jars
// of the other Minecraft versions. Called from the project root, Gradle runs it for every version.
tasks.register<Copy>("buildAndCollect") {
    group = "build"
    description = "Builds and tests this version and copies the mod jar to build/libs/$modVersion/ in the project root."
    dependsOn(tasks.named("build"))
    from(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    into(rootDir.resolve("build/libs/$modVersion"))
}
