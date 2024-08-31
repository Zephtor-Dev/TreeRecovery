dependencies {
    minecraft("com.mojang:minecraft:1.16.5")
    mappings("net.fabricmc:yarn:1.16.5+build.10")
    modImplementation("net.fabricmc:fabric-loader:0.16.3")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.42.0+1.16")
}

tasks.withType<ProcessResources> {
    inputs.property("version", rootProject.version)

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to rootProject.version))
    }
}