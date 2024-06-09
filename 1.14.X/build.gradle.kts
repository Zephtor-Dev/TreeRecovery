plugins {
    id ("org.jetbrains.kotlin.jvm")
    id ("fabric-loom")
}

repositories {
    mavenCentral()
}

dependencies {
    val minecraftVersion:  String by project
    val mappingsVersion:  String by project
    val loaderVersion:  String by project
    val fabricVersion:  String by project

    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$mappingsVersion")
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")
}