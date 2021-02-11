plugins {
    java
}

group = "us.deathmarine.luyten"
version = "0.5.4"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.apple:AppleJavaExtensions:1.4")
    arrayOf("core", "expressions", "reflection", "compilertools").forEach {
        implementation("org.bitbucket.mstrobel:procyon-$it:0.5.36")
    }
    implementation("com.fifesoft:rsyntaxtextarea:3.1.2")
    implementation("com.github.weisj:darklaf-core:2.5.5")
    implementation("com.github.weisj:darklaf-theme:2.5.5")
    implementation("com.github.weisj:darklaf-property-loader:2.5.5")
    implementation("com.github.weisj:darklaf-extensions-rsyntaxarea:0.3.4")
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