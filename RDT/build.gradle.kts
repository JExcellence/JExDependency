import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("raindrop.shadow-conventions")
    id("raindrop.dependencies-yml")
    `maven-publish`
}

// Configure runtime dependencies.yml generation
dependenciesYml {
    usePaperDependencies()
}

group = "com.raindropcentral.rdt"
version = "1.0.0"
description = "Core plugin providing shared functionality for Raindrop plugins"

dependencies {
    compileOnly(libs.paper.api)

    compileOnly(libs.adventure.api)
    compileOnly(libs.adventure.minimessage)
    compileOnly(libs.adventure.serializer.legacy)
    compileOnly(libs.adventure.serializer.json)
    compileOnly(libs.adventure.serializer.plain)
    compileOnly(libs.adventure.platform.bukkit)

    compileOnly(libs.folialib)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.vault.api) { isTransitive = false }
    compileOnly(libs.luckperms.api)

    compileOnly("com.raindropcentral.core:rcore:2.0.0")

    // Logging
    compileOnly(libs.slf4j.api)
    compileOnly(libs.slf4j.jdk14)
    compileOnly(libs.jboss.logging)

    // DB (compileOnly - provided by JExHibernate)
    compileOnly(platform(libs.hibernate.platform))
    compileOnly(libs.bundles.hibernate)

    // Caching & JSON
    compileOnly(libs.caffeine)
    compileOnly(libs.jackson.core)
    compileOnly(libs.jackson.databind)
    compileOnly(libs.jackson.annotations)
    compileOnly(libs.jackson.jsr310)

    // Version compatibility
    compileOnly(libs.xseries)
    compileOnly(libs.jehibernate)

    // Internal libraries to shade
    implementation(libs.bundles.jexcellence) {
        exclude(group = "de.jexcellence.hibernate")
        isTransitive = false
    }
    implementation(libs.bundles.jeconfig) { isTransitive = false }

    // Inventory framework
    compileOnly(libs.bundles.inventory)
}

tasks.processResources {
    val props = mapOf(
        "version" to "1.0.0",
        "name" to "RDT",
        "description" to project.description,
        "apiVersion" to "1.19"
    )
    inputs.properties(props)
    filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
        expand(props)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("RDT")
    archiveClassifier.set("")

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
            artifactId = "rdt"
            version = project.version.toString()
            pom {
                name.set("RDT")
                description.set(project.description)
            }
        }
    }
}

tasks.register("publishLocal") {
    group = "publishing"
    description = "Publishes RDT to local Maven repository"
    dependsOn("publishShadowPublicationToMavenLocal")
    doLast {
        println("✓ Published ${project.group}:rdt:${project.version} to local Maven")
    }
}
