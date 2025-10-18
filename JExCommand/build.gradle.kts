plugins {
    id("java-library")
    id("maven-publish")
}

group = "de.jexcellence.command"
version = "1.0.0"
description = "Command base for Bukkit/Paper using Evaluable error handling and Adventure messaging"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "PaperMC"
    }
    mavenLocal()
}

val adventureVersion = "4.17.0"
val paperVersion = "1.21.4-R0.1-SNAPSHOT"

dependencies {
    // Paper API provides Adventure at runtime, but we need Adventure at compile-time too.
    compileOnly("io.papermc.paper:paper-api:$paperVersion")

    // Adventure API (provides net.kyori.adventure.util.Buildable)
    compileOnly("net.kyori:adventure-api:$adventureVersion")

    // Legacy serializer used in BukkitCommand (LegacyComponentSerializer)
    compileOnly("net.kyori:adventure-text-serializer-legacy:$adventureVersion")

    // ComponentSerializer interface is declared in the gson serializer module
    compileOnly("net.kyori:adventure-text-serializer-gson:$adventureVersion")
    compileOnly("net.kyori:adventure-text-minimessage:$adventureVersion")
    compileOnly("net.kyori:adventure-text-serializer-json:$adventureVersion")
    compileOnly("net.kyori:adventure-text-serializer-plain:$adventureVersion")

    // JetBrains annotations
    compileOnly("org.jetbrains:annotations:24.1.0")
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
        options.compilerArgs.addAll(
            listOf(
                "-parameters",
                "-Xlint:unchecked",
                "-Xlint:deprecation"
            )
        )
    }

    javadoc {
        options {
            this as StandardJavadocDocletOptions
            encoding = "UTF-8"
            charSet = "UTF-8"
            links(
                "https://docs.oracle.com/en/java/javase/21/docs/api/",
                "https://jd.papermc.io/paper/1.21/"
            )
            addStringOption("Xdoclint:none", "-quiet")
        }
    }

    jar {
        manifest {
            attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "JExcellence",
                "Built-By" to System.getProperty("user.name"),
                "Built-JDK" to System.getProperty("java.version"),
                "Created-By" to "Gradle ${gradle.gradleVersion}"
            )
        }
    }

    // Helpful diagnostics
    register("printVersion") {
        group = "help"
        description = "Prints the project version"
        doLast { println("Version: ${project.version}") }
    }
    register("printDependencies") {
        group = "help"
        description = "Prints compile classpath coordinates"
        doLast { configurations.compileClasspath.get().forEach { println(it) } }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "jexcommand"
            pom {
                name.set("JExCommand")
                description.set(project.description)
                url.set("https://github.com/jexcellence")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
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
                    url.set("https://github.com/jexcellence")
                }
            }
        }
    }
    repositories {
        mavenLocal()
    }
}