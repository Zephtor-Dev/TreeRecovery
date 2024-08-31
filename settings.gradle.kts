pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/")
    }
    plugins {
        val kotlinVersion: String by settings
        val loomVersion: String by settings

        kotlin("jvm") version kotlinVersion
        id("fabric-loom") version loomVersion
    }
}

include("1.14.X")
findProject(":1.14.X")?.name = "1.14.X"

include("1.15.X")
findProject(":1.15.X")?.name = "1.15.X"
