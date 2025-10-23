import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.Test

plugins {
    java
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.6"
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation(project(":rdq-common")) { isTransitive = false }

    compileOnly(libs.paper.api)
    compileOnly("org.jetbrains:annotations:24.1.0")

    compileOnly(libs.slf4j.api)
    compileOnly(libs.slf4j.jdk14)
    compileOnly(libs.jboss.logging)

    compileOnly(platform(libs.hibernate.platform))
    compileOnly(libs.bundles.hibernate)

    implementation(libs.bundles.jexcellence) { isTransitive = false }
    implementation(libs.bundles.jeconfig) { isTransitive = false }

    // CORRECTED: Changed from 'implementation' to 'compileOnly'.
    // This tells Gradle to use the library for compiling, but NOT to package it into the final JAR.
    compileOnly(libs.bundles.inventory) { isTransitive = false }

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.mockito.inline)
    testImplementation("com.github.seeseemelk:MockBukkit-v1.21:3.133.2")
    testImplementation(libs.adventure.api)
    testImplementation(libs.adventure.minimessage)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("RDQ")
    archiveClassifier.set("Premium")
    archiveVersion.set(project.version.toString())

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

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("mavenShadow") {
            from(components["shadow"])
            groupId = project.group.toString()
            artifactId = "rdq-premium"
            version = project.version.toString()
            pom {
                name.set("RDQ Premium")
                description.set("${project.description} (Premium)")
            }
        }
    }
}