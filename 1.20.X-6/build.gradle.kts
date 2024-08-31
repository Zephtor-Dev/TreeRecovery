dependencies {
    minecraft("com.mojang:minecraft:1.20.6")
    mappings("net.fabricmc:yarn:1.20.6+build.3")
    modImplementation("net.fabricmc:fabric-loader:0.16.3")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.100.8+1.20.6")
}

tasks.withType<ProcessResources> {
    inputs.property("version", rootProject.version)

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to rootProject.version))
    }
}