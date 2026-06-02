plugins {
    id("raindrop.library-conventions")
    id("raindrop.dependencies-yml")
}

group = "de.jexcellence.dependency"
version = "2.1.0"
description = "Modern dependency management and plugin architecture for Minecraft servers"

// Configure runtime dependencies.yml generation
dependenciesYml {
    useJExDependencyDependencies()
}

ext["vendor"] = "JExcellence"

dependencies {
    // ── Compile-only: Server API ──
    compileOnly(libs.paper.api)
    compileOnly(libs.jetbrains.annotations)

    // ── Implementation ──
    implementation(libs.asm)
    implementation(libs.asm.commons)

    // ── Test ──
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks {
    test {
        useJUnitPlatform()
        // Required for ClasspathInjector tests that exercise URLClassLoader.addURL via reflection
        jvmArgs("--add-opens", "java.base/java.net=ALL-UNNAMED")
    }

    register<Jar>("fatJar") {
        group = "build"
        description = "Creates a fat JAR with all dependencies"
        archiveClassifier.set("all")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        
        from(sourceSets.main.get().output)

        dependsOn(configurations.runtimeClasspath)
        from({
            configurations.runtimeClasspath.get()
                .filter { it.name.endsWith("jar") }
                .map { zipTree(it) }
        })
    }
}

afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("maven") {
                artifactId = "jexdependency"
                pom {
                    name.set("JExDependency")
                    description.set("Runtime dependency management & relocation for Paper/Spigot plugins.")
                    url.set("https://github.com/JExcellence/JExDependency")
                    inceptionYear.set("2025")
                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://github.com/JExcellence/JExDependency/blob/main/LICENSE")
                            distribution.set("repo")
                        }
                    }
                    developers {
                        developer {
                            id.set("jexcellence")
                            name.set("JExcellence")
                            email.set("contact@jexcellence.de")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/JExcellence/JExDependency.git")
                        developerConnection.set("scm:git:ssh://github.com/JExcellence/JExDependency.git")
                        url.set("https://github.com/JExcellence/JExDependency")
                    }
                    issueManagement {
                        system.set("GitHub Issues")
                        url.set("https://github.com/JExcellence/JExDependency/issues")
                    }
                }
            }
        }
    }
}
