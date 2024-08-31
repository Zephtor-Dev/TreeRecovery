dependencies {
    minecraft("com.mojang:minecraft:1.18.2")
    mappings("net.fabricmc:yarn:1.18.2+build.4")
    modImplementation("net.fabricmc:fabric-loader:0.16.3")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.77.0+1.18.2")
}

tasks.withType<ProcessResources> {
    inputs.property("version", rootProject.version)

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to rootProject.version))
    }
}