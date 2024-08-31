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
    dependsOn(
        ":1.14.X:remapJar",
        ":1.15.X:remapJar",
        ":1.16.X:remapJar",
        ":1.17.X:remapJar",
        ":1.18.X:remapJar",
        ":1.19.X:remapJar",
        ":1.20.X-4:remapJar",
        ":1.20.X-6:remapJar",
    )

    doLast {
        copy {
            from(
                ":1.14.X:build/libs",
                ":1.15.X:build/libs",
                ":1.16.X:build/libs",
                ":1.17.X:build/libs",
                ":1.18.X:build/libs",
                ":1.19.X:build/libs",
                ":1.20.X-4:build/libs",
                ":1.20.X-6:build/libs"
            )
            into("$rootDir/artifacts")
        }
    }
}
