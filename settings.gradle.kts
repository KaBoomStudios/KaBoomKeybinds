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

rootProject.name = "kaboomkeybinds"
