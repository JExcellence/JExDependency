plugins {
    id("raindrop.library-conventions")
}

group = "com.raindropcentral.commands"
version = "1.0.0"
description = "Command base for Bukkit/Paper using Evaluable error handling and Adventure messaging"

ext["vendor"] = "JExcellence"

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.bundles.adventure)
    compileOnly(libs.bundles.jeconfig)
}

afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("maven") {
                artifactId = "jexcommand"
                pom {
                    url.set("https://github.com/jexcellence")
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
    }
}