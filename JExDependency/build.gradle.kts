plugins {
    id("raindrop.library-conventions")
    id("raindrop.dependencies-yml")
}

group = "de.jexcellence.dependency"
version = "2.0.0"
description = "Modern dependency management and plugin architecture for Minecraft servers"

// Configure runtime dependencies.yml generation
dependenciesYml {
    useJExDependencyDependencies()
}

ext["vendor"] = "JExcellence"

dependencies {
    compileOnly(libs.paper.api)
    compileOnly("org.jetbrains:annotations:24.1.0")

    implementation("org.ow2.asm:asm:9.7.1")
    implementation("org.ow2.asm:asm-commons:9.7.1")
}

tasks {
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

publishing {
    publications {
        named<MavenPublication>("maven") {
            artifactId = "jexdependency"
            pom {
                url.set("https://github.com/jexcellence/JEDependency")
                inceptionYear.set("2025")
                developers {
                    developer {
                        id.set("jexcellence")
                        name.set("JExcellence")
                        email.set("contact@jexcellence.de")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/jexcellence/JEDependency.git")
                    developerConnection.set("scm:git:ssh://github.com/jexcellence/JEDependency.git")
                    url.set("https://github.com/jexcellence/JEDependency")
                }
                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/jexcellence/JEDependency/issues")
                }
            }
        }
    }
}
