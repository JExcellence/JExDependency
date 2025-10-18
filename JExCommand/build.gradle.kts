plugins {
    id("java-library")
    id("maven-publish")
}

group = "com.raindropcentral.commands"
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
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
}

val adventureVersion = "4.17.0"
val paperVersion = "1.21.4-R0.1-SNAPSHOT"

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-json:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-gson:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-plain:4.17.0")
    compileOnly("net.kyori:adventure-platform-bukkit:4.3.4")
    compileOnly("net.kyori:adventure-nbt:4.17.0")
    compileOnly("net.kyori:adventure-key:4.17.0")
    compileOnly("net.kyori:option:1.1.0")
    compileOnly("de.jexcellence.config:Evaluable:1.0.0")
    compileOnly("de.jexcellence.config:GPEEE:1.0.0")
    compileOnly("de.jexcellence.config:ConfigMapper:1.0.0")
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