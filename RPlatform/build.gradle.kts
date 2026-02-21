plugins {
    id("raindrop.library-conventions")
}

group = "com.raindropcentral.platform"
version = "2.0.0"
description = "Modern platform abstraction layer for Spigot/Paper/Folia plugins"

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.adventure.api)
    compileOnly(libs.adventure.minimessage)
    compileOnly(libs.adventure.platform.bukkit)
    
    implementation(libs.jetranslate)
    implementation(libs.bundles.jeconfig)
    compileOnly(libs.jehibernate)
    
    compileOnly(platform(libs.hibernate.platform))
    compileOnly(libs.bundles.hibernate)

    compileOnly(libs.caffeine)
    compileOnly(libs.jackson.core)
    compileOnly(libs.jackson.databind)
    compileOnly(libs.jackson.annotations)
    compileOnly(libs.jackson.jsr310)
    compileOnly(libs.java.uuid)
    compileOnly(libs.xseries)
    
    compileOnly(libs.placeholderapi)
    compileOnly(libs.luckperms.api)
    compileOnly(libs.floodgate.api)
    compileOnly(libs.folialib)
    compileOnly(libs.bundles.inventory)
    compileOnly(libs.vault.api) { isTransitive = false }
    
    // JExEconomy for currency support (runtime via services)
    compileOnly(project(":JExEconomy:jexeconomy-common"))
    
    compileOnly("org.jetbrains:annotations:24.0.1")
}

tasks {
    jar {
        archiveBaseName.set("RPlatform")
    }
}

afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("maven") {
                artifactId = "rplatform"
                pom {
                    url.set("https://github.com/raindropcentral/rplatform")
                    developers {
                        developer {
                            id.set("jexcellence")
                            name.set("JExcellence")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/raindropcentral/rplatform.git")
                        developerConnection.set("scm:git:ssh://github.com/raindropcentral/rplatform.git")
                        url.set("https://github.com/raindropcentral/rplatform")
                    }
                }
            }
        }
        
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/raindropcentral/rplatform")
                credentials {
                    username = System.getenv("GITHUB_ACTOR") ?: findProperty("gpr.user") as String?
                    password = System.getenv("GITHUB_TOKEN") ?: findProperty("gpr.key") as String?
                }
            }
        }
    }
}
