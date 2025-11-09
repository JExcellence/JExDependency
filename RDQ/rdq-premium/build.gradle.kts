import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.Test

plugins {
    id("raindrop.shadow-conventions")
}

version = "6.0.1"

dependencies {
    implementation(project(":RDQ:rdq-common"))

    compileOnly(libs.paper.api)
    compileOnly(libs.slf4j.api)
    compileOnly(libs.slf4j.jdk14)
    compileOnly(libs.jboss.logging)

    compileOnly("com.raindropcentral.core:rcore-common:2.0.0")
    compileOnly("com.raindropcentral.core:rcore-free:2.0.0")
    compileOnly("de.jexcellence.economy:jexeconomy:2.0.0")

    compileOnly(platform(libs.hibernate.platform))
    compileOnly(libs.bundles.hibernate)

    implementation(libs.bundles.jexcellence) { isTransitive = false }
    implementation(libs.bundles.jeconfig) { isTransitive = false }

    compileOnly(libs.bundles.inventory) { isTransitive = false }

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.mockbukkit)
    testImplementation(libs.adventure.api)
    testImplementation(libs.adventure.minimessage)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("RDQ")
    archiveClassifier.set("Premium")
    archiveVersion.set(project.version.toString())

    relocate("com.fasterxml.jackson.core", "de.jexcellence.remapped.com.fasterxml.jackson.core")
    relocate("com.fasterxml.jackson.databind", "de.jexcellence.remapped.com.fasterxml.jackson.databind")
    relocate("com.fasterxml.jackson.annotation", "de.jexcellence.remapped.com.fasterxml.jackson.annotation")
    relocate("com.fasterxml.jackson.datatype", "de.jexcellence.remapped.com.fasterxml.jackson.datatype")

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