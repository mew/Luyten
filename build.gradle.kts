plugins {
    java
}

group = "us.deathmarine.luyten"
version = "0.5.4"

repositories {
    mavenCentral()
}

dependencies {
    listOf("core", "expressions", "reflection", "compilertools").forEach {
        implementation("org.bitbucket.mstrobel:procyon-$it:0.5.36")
    }
    listOf("core", "theme", "property-loader", "platform-base", "native-utils", "utils", "windows").forEach {
        implementation("com.github.weisj:darklaf-$it:2.5.5")
    }
    implementation("commons-io:commons-io:2.5")
    implementation("org.benf:cfr:0.151")
    implementation("com.fifesoft:rsyntaxtextarea:3.1.2")
    implementation("com.github.weisj:darklaf-extensions-rsyntaxarea:0.3.4")
    implementation("com.formdev:svgSalamander:1.1.2.3")
    implementation("org.ow2.asm:asm:9.1")
    implementation("org.ow2.asm:asm-tree:9.1")
}

tasks.withType<Jar> {
    manifest {
        attributes(mapOf(
            "Main-Class" to "us.deathmarine.luyten.Luyten"
        ))
    }

    configurations.compileClasspath {
        forEach { dep ->
            if (!configurations.testCompileOnly.contains(dep)) {
                from(zipTree(dep).matching {
                    include("**/*")

                    exclude("META-INF/*")
                    exclude("META-INF/maven/")
                    include("META-INF/services/")

                    exclude("module-info.class")
                })
            }
        }
    }
}