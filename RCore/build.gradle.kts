import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("raindrop.shadow-conventions")
    id("raindrop.dependencies-yml")
    `maven-publish`
}

// Configure runtime dependencies.yml generation
dependenciesYml {
    useRCoreDependencies()
}

// ===========================================
// Dynamic Version Configuration
// ===========================================
val versionMajor: String by project.rootProject.extra { findProperty("rcore.version.major")?.toString() ?: "2" }
val versionMinor: String by project.rootProject.extra { findProperty("rcore.version.minor")?.toString() ?: "0" }
val versionPatch: String by project.rootProject.extra { findProperty("rcore.version.patch")?.toString() ?: "0" }
val versionStage: String by project.rootProject.extra { findProperty("rcore.version.stage")?.toString() ?: "Alpha" }
val versionBuild: String by project.rootProject.extra { findProperty("rcore.version.build")?.toString() ?: "4" }

val rcoreVersion = "$versionMajor.$versionMinor.$versionPatch-$versionStage-Build-$versionBuild"

group = "com.raindropcentral.core"
version = "2.0.0"
description = "Core plugin providing shared functionality for Raindrop plugins"

dependencies {
    compileOnly(libs.paper.api)
    implementation(project(":RPlatform"))
    compileOnly("com.velocitypowered:velocity-api:3.4.0")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0")

    // Adventure APIs (core APIs are compileOnly as Paper provides them)
    compileOnly(libs.adventure.api)
    compileOnly(libs.adventure.minimessage)
    compileOnly(libs.adventure.serializer.legacy)
    compileOnly(libs.adventure.serializer.json)
    compileOnly(libs.adventure.serializer.plain)
    compileOnly(libs.adventure.platform.bukkit)    

    compileOnly(libs.folialib)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.vault.api) { isTransitive = false}
    compileOnly(libs.luckperms.api)
    compileOnly(libs.bundles.inventory)
    compileOnly(libs.jehibernate)

    compileOnly(libs.slf4j.api)
    compileOnly(libs.slf4j.jdk14)
    compileOnly(libs.jboss.logging)

    compileOnly(platform(libs.hibernate.platform))
    compileOnly(libs.bundles.hibernate)

    compileOnly(libs.caffeine)
    compileOnly(libs.jackson.core)
    compileOnly(libs.jackson.databind)
    compileOnly(libs.jackson.annotations)
    compileOnly(libs.jackson.jsr310)
    compileOnly(libs.java.uuid)
    compileOnly(libs.xseries)

    implementation(libs.bundles.jexcellence) {
        exclude(group = "de.jexcellence.hibernate")
        exclude(group = "com.raindropcentral.platform", module = "rplatform")
        isTransitive = false
    }
    implementation(libs.bundles.jeconfig) { isTransitive = false }
    // InventoryFramework is provided at runtime through JExDependency/Paper loader.
    // Packaging a second relocated copy into RCore splits class ownership and breaks ViewFrame feature installation.
    compileOnly(libs.bundles.inventory)

    compileOnly(libs.jexeconomy)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(project(":RPlatform"))
    testImplementation(libs.paper.api)
    testCompileOnly(libs.jackson.annotations)
}

tasks.processResources {
    val props = mapOf(
        "version" to rcoreVersion,
        "name" to "RCore",
        "description" to project.description,
        "apiVersion" to "1.21"
    )
    inputs.properties(props)
    filesMatching(listOf("plugin.yml", "paper-plugin.yml", "velocity-plugin.json")) {
        expand(props)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("RCore")
    archiveClassifier.set("")
    archiveVersion.set(rcoreVersion)

    relocate("com.github.benmanes", "de.jexcellence.remapped.com.github.benmanes")

    configurations = listOf(project.configurations.getByName("runtimeClasspath"))
    mergeServiceFiles()
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

publishing {
    publications {
        afterEvaluate {
            publications.removeIf { it.name == "maven" }
        }
        create<MavenPublication>("shadow") {
            artifact(tasks.named("shadowJar"))
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))
            groupId = project.group.toString()
            artifactId = "rcore"
            version = project.version.toString()
            pom {
                name.set("RCore")
                description.set(project.description)
            }
        }
    }
}

tasks.register("publishLocal") {
    group = "publishing"
    description = "Publishes RCore to local Maven repository"
    dependsOn("publishShadowPublicationToMavenLocal")
    doLast {
        println("✓ Published ${project.group}:rcore:${project.version} to local Maven")
    }
}
