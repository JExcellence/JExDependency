plugins {
    id("raindrop.shadow-conventions")
}

group = "de.jexcellence.economy"
version = "2.0.0"
description = "JExEconomy"

ext["vendor"] = "JExcellence"

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.slf4j.api)
    compileOnly(libs.slf4j.jdk14)
    compileOnly(libs.jboss.logging)

    compileOnly(platform(libs.hibernate.platform))
    compileOnly(libs.bundles.hibernate)

    compileOnly(libs.rplatform)
    
    implementation(libs.bundles.jexcellence) { isTransitive = false }
    implementation(libs.bundles.jeconfig) { isTransitive = false }
    compileOnly(libs.bundles.inventory)
    compileOnly(libs.vault.api) { isTransitive = false }
    compileOnly(libs.placeholderapi)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.mockbukkit)
    testImplementation(libs.bundles.inventory)
}

tasks {
    jar {
        archiveBaseName.set("JExEconomy")
        manifest {
            attributes["API-Version"] = "2.0"
        }
        exclude("**/*Test.class", "**/*Tests.class", "**/test/**")
    }

    javadoc {
        if (JavaVersion.current().isJava9Compatible) {
            (options as StandardJavadocDocletOptions).apply {
                addBooleanOption("html5", true)
                addStringOption("tag", "apiNote:a:API Note:")
                addStringOption("tag", "implSpec:a:Implementation Requirements:")
                addStringOption("tag", "implNote:a:Implementation Note:")
            }
        }
        options.memberLevel = JavadocMemberLevel.PUBLIC
    }

    test {
        useJUnitPlatform()
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("JExEconomy")
}

publishing {
    publications {
        named<MavenPublication>("maven") {
            artifactId = "jexeconomy"
            pom {
                url.set("https://github.com/jexcellence/jexeconomy")
                developers {
                    developer {
                        id.set("JExcellence")
                        name.set("JExcellence Team")
                        email.set("contact@jexcellence.de")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/jexcellence/jexeconomy.git")
                    developerConnection.set("scm:git:ssh://github.com/jexcellence/jexeconomy.git")
                    url.set("https://github.com/jexcellence/jexeconomy")
                }
            }
        }
    }
}
