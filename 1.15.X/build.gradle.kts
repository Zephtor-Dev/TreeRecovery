dependencies {
    minecraft("com.mojang:minecraft:1.15.2")
    mappings("net.fabricmc:yarn:1.15.2+build.17")
    modImplementation("net.fabricmc:fabric-loader:0.16.3")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.28.5+1.15")
}

tasks.withType<ProcessResources> {
    inputs.property("version", rootProject.version)

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to rootProject.version))
    }
}