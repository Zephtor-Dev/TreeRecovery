plugins {
    id ("org.jetbrains.kotlin.jvm")
    id ("fabric-loom")
}

val name: String = "TreeRecovery"
version = "1.17"

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

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}
java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
tasks.named<Jar>("jar") {
    archiveFileName.set("$name-${project.version}.jar")
    archiveClassifier.set("")
}