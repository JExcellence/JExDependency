plugins {
    id("raindrop.library-conventions")
}

group = "de.jexcellence.platform"
version = "1.0.0"
description = "Lean platform infrastructure for Spigot/Paper/Folia plugins"

dependencies {
    // Server API
    compileOnly(libs.paper.api)

    // Adventure (provided by Paper at runtime)
    compileOnly(libs.adventure.api)
    compileOnly(libs.adventure.minimessage)
    compileOnly(libs.adventure.platform.bukkit)

    // Internal libraries
    implementation(libs.jetranslate)
    compileOnly(libs.jehibernate)
    compileOnly(libs.bundles.jeconfig)

    // Hibernate (compile-only, provided by consumer)
    compileOnly(platform(libs.hibernate.platform))
    compileOnly(libs.bundles.hibernate)

    // View framework (compile-only, provided by consumer)
    compileOnly(libs.bundles.inventory)

    // Jackson (provided by Paper at runtime)
    compileOnly(libs.jackson.databind)
    compileOnly(libs.jackson.annotations)

    // Annotations
    compileOnly("org.jetbrains:annotations:24.0.1")

    // Testing
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockito.core)
    testCompileOnly(libs.paper.api)
}

tasks {
    jar {
        archiveBaseName.set("JExPlatform")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("maven") {
                groupId = "de.jexcellence.platform"
                artifactId = "jexplatform"
                pom {
                    url.set("https://github.com/raindropcentral/jexplatform")
                    developers {
                        developer {
                            id.set("jexcellence")
                            name.set("JExcellence")
                        }
                    }
                    scm {
                        connection.set("scm:git:git://github.com/raindropcentral/jexplatform.git")
                        developerConnection.set("scm:git:ssh://github.com:raindropcentral/jexplatform.git")
                        url.set("https://github.com/raindropcentral/jexplatform")
                    }
                }
            }
        }
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/raindropcentral/jexplatform")
                credentials {
                    username = System.getenv("GITHUB_ACTOR") ?: findProperty("gpr.user") as String?
                    password = System.getenv("GITHUB_TOKEN") ?: findProperty("gpr.key") as String?
                }
            }
        }
    }
}
