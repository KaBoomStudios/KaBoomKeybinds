pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        // Minecraft 26.1 and newer ship unobfuscated, so the Loom plugin without the "-remap" suffix is used.
        id("net.fabricmc.fabric-loom") version providers.gradleProperty("loom_version")
    }
}

plugins {
    // Builds the mod for several Minecraft versions from the one source tree in src/. Version specific code is
    // marked with Stonecutter comments, see https://stonecutter.kikugie.dev/
    id("dev.kikugie.stonecutter") version "0.9.8"
}

stonecutter {
    create(rootProject) {
        // Every version becomes a subproject in versions/<version>/ that runs build.gradle.kts with the settings from
        // versions/<version>/gradle.properties.
        versions("26.1.2", "26.2", "26.3")
        // The sources are committed in the state for this version.
        vcsVersion = "26.2"
    }
}

rootProject.name = "kaboomkeybinds"
