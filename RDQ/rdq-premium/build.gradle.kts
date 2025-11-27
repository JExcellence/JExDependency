plugins {
    id("raindrop.shadow-conventions")
}

group = "com.raindropcentral.rdq"
version = "6.0.0"
description = "RDQ Premium - Premium edition of RaindropQuests"

dependencies {
    // Include common module
    implementation(project(":RDQ:rdq-common"))

    // Server API
    compileOnly(libs.paper.api)

    // Adventure APIs
    compileOnly(libs.bundles.adventure)

    // Ecosystem (provided by other plugins)
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

    // Internal libraries to shade
    implementation(libs.bundles.jexcellence) { isTransitive = false }
    implementation(libs.bundles.jeconfig) { isTransitive = false }

    // Inventory framework
    implementation(libs.bundles.inventory)
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version,
        "name" to "RDQPremium",
        "description" to project.description,
        "apiVersion" to "1.21"
    )
    inputs.properties(props)
    filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
        expand(props)
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("RDQPremium")
    archiveClassifier.set("")

    // Include all runtime dependencies (not just specific patterns)
    configurations = listOf(project.configurations.getByName("runtimeClasspath"))
    mergeServiceFiles()

    // Relocations must be outside dependencies block
    relocate("com.fasterxml.jackson.core", "de.jexcellence.remapped.com.fasterxml.jackson.core")
    relocate("com.fasterxml.jackson.databind", "de.jexcellence.remapped.com.fasterxml.jackson.databind")
    relocate("com.fasterxml.jackson.annotation", "de.jexcellence.remapped.com.fasterxml.jackson.annotation")
    relocate("com.fasterxml.jackson.datatype", "de.jexcellence.remapped.com.fasterxml.jackson.datatype")
    relocate("com.github.benmanes", "de.jexcellence.remapped.com.github.benmanes")
    relocate("org.h2", "de.jexcellence.remapped.org.h2")
    relocate("me.devnatan.inventoryframework", "de.jexcellence.remapped.me.devnatan.inventoryframework")
    relocate("com.tcoded", "de.jexcellence.remapped.com.tcoded")
    relocate("com.cryptomorin.xseries", "de.jexcellence.remapped.com.cryptomorin.xseries")

    minimize {
        exclude(project(":RDQ:rdq-common"))
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

// Disable the regular jar task since we use shadowJar
tasks.named<Jar>("jar") {
    enabled = false
}

// Create a shadow publication instead of using the default maven one
publishing {
    publications {
        create<MavenPublication>("shadow") {
            artifact(tasks.named("shadowJar"))
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))
            groupId = project.group.toString()
            artifactId = "rdq-premium"
            version = project.version.toString()
            pom {
                name.set("RDQ Premium")
                description.set(project.description)
            }
        }
    }
}
