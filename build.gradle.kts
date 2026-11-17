plugins {
    `java-library`
    `maven-publish`
}

group = "de.jexcellence.dependency"
version = "2.2.0"
description = "Runtime dependency loading, verification and relocation for Spigot, Paper and Folia plugins"

/** Bytecode level of the published jar. Servers must run at least this Java version. */
val targetJava = 21

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.jetbrains.annotations)

    implementation(libs.asm)
    implementation(libs.asm.commons)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.jetbrains.annotations)
    testImplementation(libs.paper.api)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release.set(targetJava)
        options.encoding = "UTF-8"
    }

    jar {
        manifest {
            attributes(
                "Automatic-Module-Name" to "de.jexcellence.dependency",
                "Implementation-Title" to "JExDependency",
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "JExcellence"
            )
        }
    }

    javadoc {
        val docletOptions = options as StandardJavadocDocletOptions
        docletOptions.encoding = "UTF-8"
        docletOptions.addStringOption("Xdoclint:all,-missing", "-quiet")
        docletOptions.links("https://docs.oracle.com/en/java/javase/21/docs/api/")
    }

    test {
        useJUnitPlatform()
        // ClasspathInjector tests exercise URLClassLoader.addURL via reflection.
        jvmArgs("--add-opens", "java.base/java.net=ALL-UNNAMED")
        // Keep the shared cache of downloader tests inside the build folder.
        systemProperty("jedependency.cache.dir", layout.buildDirectory.dir("test-cache").get().asFile.absolutePath)
    }

    register<Jar>("fatJar") {
        group = "build"
        description = "Creates a jar that also contains ASM, for consumers that do not resolve transitive dependencies"
        archiveClassifier.set("all")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest.from(jar.get().manifest)

        from(sourceSets.main.get().output)
        dependsOn(configurations.runtimeClasspath)
        from({
            configurations.runtimeClasspath.get()
                .filter { it.name.endsWith(".jar") }
                .map { zipTree(it) }
        })
    }
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            artifactId = "jexdependency"
            from(components["java"])

            pom {
                name.set("JExDependency")
                description.set(project.description)
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
                    connection.set("scm:git:https://github.com/JExcellence/JExDependency.git")
                    developerConnection.set("scm:git:ssh://git@github.com/JExcellence/JExDependency.git")
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
