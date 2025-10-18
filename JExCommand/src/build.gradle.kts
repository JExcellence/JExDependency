plugins {
    java
    `maven-publish`
}

group = "com.raindropcentral.commands"
version = "1.0.0"
description = "Command framework for Raindrop plugins"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
}

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

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
    withJavadocJar()
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
        options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all", "-Xlint:-processing"))
    }

    jar {
        archiveBaseName.set("rcommands")
        manifest {
            attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "Raindrop Central"
            )
        }
    }

    javadoc {
        options.encoding = "UTF-8"
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "rcommands"
            version = project.version.toString()

            pom {
                name.set("RCommands")
                description.set(project.description)
                url.set("https://github.com/raindropcentral/raindrop-plugins")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
            }
        }
    }

    repositories {
        mavenLocal()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/raindropcentral/raindrop-plugins")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: findProperty("gpr.user") as String?
                password = System.getenv("GITHUB_TOKEN") ?: findProperty("gpr.key") as String?
            }
        }
    }
}

tasks.register("publishLocal") {
    group = "publishing"
    description = "Publishes to local Maven repository"
    dependsOn("publishToMavenLocal")
    doLast {
        println("✓ Published ${project.group}:rcommands:${project.version} to local Maven")
    }
}
