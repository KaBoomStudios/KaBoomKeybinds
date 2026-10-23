plugins {
    id("dev.kikugie.stonecutter")
    // Loom is put on the class path of the root project once, so that all version subprojects share one copy of it.
    id("net.fabricmc.fabric-loom") apply false
}

// The version the sources in src/ are currently written for. IntelliJ IDEA compiles and highlights src/ against it.
// Switch it with the "Set active project to ..." tasks, and reset it to the version in settings.gradle.kts before
// committing.
stonecutter active "26.2"

// Gradle itself has to run on Java 25, because Loom 1.18 does not start on older Java. The daemon JVM criteria in
// gradle/gradle-daemon-jvm.properties make Gradle pick an installed JDK 25 for its daemon, whatever Java the shell
// or JAVA_HOME points at. The file is updated with "gradlew updateDaemonJvm". Download URLs are not generated,
// because the build has no toolchain download repository: a JDK 25 has to be installed, and Gradle finds it through
// its usual detection (JAVA_HOME, IntelliJ, the operating system's known locations).
tasks.named<UpdateDaemonJvm>("updateDaemonJvm") {
    languageVersion = JavaLanguageVersion.of(25)
    toolchainDownloadUrls.empty()
}
