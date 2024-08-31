dependencies {
    minecraft("com.mojang:minecraft:1.19.4")
    mappings("net.fabricmc:yarn:1.19.4+build.2")
    modImplementation("net.fabricmc:fabric-loader:0.16.3")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.87.2+1.19.4")
}

tasks.withType<ProcessResources> {
    inputs.property("version", rootProject.version)

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to rootProject.version))
    }
}