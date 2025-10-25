import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.6"
}

group = "de.jexcellence.economy"
version = "2.0.0"
description = "JExEconomy"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.tcoded.com/releases")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly(libs.paper.api)

    compileOnly(libs.slf4j.api)
    compileOnly(libs.slf4j.jdk14)
    compileOnly(libs.jboss.logging)

    compileOnly(platform(libs.hibernate.platform))
    compileOnly(libs.bundles.hibernate)

    implementation(libs.bundles.jexcellence) { isTransitive = false }
    implementation(libs.bundles.jeconfig) { isTransitive = false }
    compileOnly(libs.bundles.inventory)
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") { isTransitive = false }
    compileOnly("me.clip:placeholderapi:2.11.6")

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.mockito.inline)
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
        options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all", "-Xlint:-processing", "-Xlint:-serial"))
    }

    jar {
        archiveBaseName.set("JExEconomy")
        manifest {
            attributes(
                "Implementation-Title" to "JExEconomy",
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "JExcellence Team",
                "API-Version" to "2.0"
            )
        }
        exclude("**/*Test.class", "**/*Tests.class", "**/test/**")
    }

    javadoc {
        options.encoding = "UTF-8"
        if (JavaVersion.current().isJava9Compatible) {
            (options as StandardJavadocDocletOptions).apply {
                addBooleanOption("html5", true)
                addStringOption("Xdoclint:none", "-quiet")
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

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("JExEconomy")
    archiveVersion.set(project.version.toString()
    )

    // Relocate all core Jackson packages to maintain type safety
    relocate("com.fasterxml.jackson.core", "de.jexcellence.remapped.com.fasterxml.jackson.core")
    relocate("com.fasterxml.jackson.databind", "de.jexcellence.remapped.com.fasterxml.jackson.databind")
    relocate("com.fasterxml.jackson.annotation", "de.jexcellence.remapped.com.fasterxml.jackson.annotation")
    relocate("com.fasterxml.jackson.datatype", "de.jexcellence.remapped.com.fasterxml.jackson.datatype")

    // Other relocations
    relocate("com.github.benmanes", "de.jexcellence.remapped.com.github.benmanes")
    relocate("org.h2", "de.jexcellence.remapped.org.h2")
    relocate("me.devnatan.inventoryframework", "de.jexcellence.remapped.me.devnatan.inventoryframework")
    relocate("com.tcoded", "de.jexcellence.remapped.com.tcoded")
    relocate("com.cryptomorin.xseries", "de.jexcellence.remapped.com.cryptomorin.xseries")

    configurations = listOf(project.configurations.getByName("runtimeClasspath"))
    mergeServiceFiles()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "jexeconomy"
            version = project.version.toString()

            pom {
                name.set("JExEconomy")
                description.set(project.description)
                url.set("https://github.com/jexcellence/jexeconomy")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
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
