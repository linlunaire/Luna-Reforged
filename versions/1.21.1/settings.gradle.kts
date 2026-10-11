pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.architectury.dev/")
        maven("https://maven.fabricmc.net/")
        maven("https://neoforged.forgecdn.net/releases/")
        maven("https://maven.neoforged.net/releases/")
    }
}
rootProject.name = "Luna-Reforged-1.21.1"
include("fabric", "neoforge", "lunacore-fabric", "lunacore-neoforge")