plugins {
    id("raindrop.library-conventions")
}

group = "com.raindropcentral.commands"
version = "2.0.0"
description = "JExCommand - Annotation-driven command discovery and registration for Bukkit/Paper"

ext["vendor"] = "JExcellence"

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.slf4j.api)

    compileOnly(libs.bundles.adventure)
    compileOnly(libs.bundles.jeconfig)
    compileOnly(libs.jetranslate)
}

tasks {
    jar {
        archiveBaseName.set("jexcommand")
        manifest {
            attributes["API-Version"] = "2.0"
        }
    }

    javadoc {
        (options as StandardJavadocDocletOptions).apply {
            if (JavaVersion.current().isJava9Compatible) {
                addBooleanOption("html5", true)
            }
        }
        options.memberLevel = JavadocMemberLevel.PUBLIC
    }
}

afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("maven") {
                artifactId = "jexcommand"
                pom {
                    url.set("https://github.com/jexcellence/jexcommand")
                    developers {
                        developer {
                            id.set("jexcellence")
                            name.set("JExcellence Team")
                            email.set("contact@jexcellence.de")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/jexcellence/jexcommand.git")
                        developerConnection.set("scm:git:ssh://github.com/jexcellence/jexcommand.git")
                        url.set("https://github.com/jexcellence/jexcommand")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/jexcellence/jexcommand")
                credentials {
                    username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                    password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }
}
