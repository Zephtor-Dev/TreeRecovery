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

include("1.16.X")
findProject(":1.16.X")?.name = "1.16.X"

include("1.17.X")
findProject(":1.17.X")?.name = "1.17.X"

include("1.18.X")
findProject(":1.18.X")?.name = "1.18.X"

include("1.19.X")
findProject(":1.19.X")?.name = "1.19.X"

include("1.20.X-4")
findProject(":1.20.X-4")?.name = "1.20.X-4"

include("1.20.X-6")
findProject(":1.20.X-6")?.name = "1.20.X-6"