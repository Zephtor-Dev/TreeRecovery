plugins {
    kotlin("jvm")
    id("fabric-loom") apply false
}

repositories {
    mavenCentral()
}

allprojects {
    group = "com.zephtor"
    version = "1.0.0"
}

subprojects {
    apply(plugin = "fabric-loom")
    apply(plugin = "kotlin")
}

tasks.create("buildAll") {
    dependsOn(":1.14.X:remapJar", ":1.15.X:remapJar")
}