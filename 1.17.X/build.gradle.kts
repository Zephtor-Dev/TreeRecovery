dependencies {
    minecraft("com.mojang:minecraft:1.17.1")
    mappings("net.fabricmc:yarn:1.17.1+build.65")
    modImplementation("net.fabricmc:fabric-loader:0.16.3")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.46.1+1.17")
}

tasks.withType<ProcessResources> {
    inputs.property("version", rootProject.version)

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to rootProject.version))
    }
}