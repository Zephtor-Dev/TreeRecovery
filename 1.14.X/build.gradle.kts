dependencies {
    minecraft("com.mojang:minecraft:1.14.4")
    mappings("net.fabricmc:yarn:1.14.4+build.14")
    modImplementation("net.fabricmc:fabric-loader:0.16.3")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.28.5+1.14")
}

tasks.withType<ProcessResources> {
    inputs.property("version", rootProject.version)

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to rootProject.version))
    }
}