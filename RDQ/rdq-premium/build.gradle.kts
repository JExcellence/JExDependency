plugins {
    id("raindrop.shadow-conventions")
    id("raindrop.dependencies-yml")
}

dependenciesYml {
    usePaperDependencies()
    generatePaperVariant.set(true)
    generateSpigotVariant.set(true)
}

val versionMajor: String by project.rootProject.extra { findProperty("rdq.version.major")?.toString() ?: "6" }
val versionMinor: String by project.rootProject.extra { findProperty("rdq.version.minor")?.toString() ?: "0" }
val versionPatch: String by project.rootProject.extra { findProperty("rdq.version.patch")?.toString() ?: "0" }
val versionStage: String by project.rootProject.extra { findProperty("rdq.version.stage")?.toString() ?: "Alpha" }
val versionBuild: String by project.rootProject.extra { findProperty("rdq.version.build")?.toString() ?: "4" }

val rdqVersion = "$versionMajor.$versionMinor.$versionPatch-$versionStage-Build-$versionBuild"

group = "com.raindropcentral.rdq"
version = rdqVersion
description = "RDQ Premium - Premium edition of RaindropQuests"

dependencies {
    implementation(project(":RDQ:rdq-common"))
    implementation(project(":JExCommand"))

    compileOnly(libs.paper.api)

    compileOnly(libs.adventure.api)
    compileOnly(libs.adventure.minimessage)
    compileOnly(libs.adventure.serializer.legacy)
    compileOnly(libs.adventure.serializer.json)
    compileOnly(libs.adventure.serializer.plain)
    compileOnly(libs.adventure.platform.bukkit)
    compileOnly(libs.jexeconomy) { isTransitive = false}
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
        exclude(group = "com.raindropcentral.commands", module = "jexcommand")
        isTransitive = false
    }
    implementation(libs.bundles.jeconfig) { isTransitive = false }

    // Inventory framework
    compileOnly(libs.bundles.inventory)
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
    archiveBaseName.set("RDQ")
    archiveVersion.set(rdqVersion)
    archiveClassifier.set("Premium")

    relocate("com.github.benmanes", "de.jexcellence.remapped.com.github.benmanes")
    relocate("me.devnatan.inventoryframework", "de.jexcellence.remapped.me.devnatan.inventoryframework")
    relocate("com.tcoded", "de.jexcellence.remapped.com.tcoded")
    relocate("com.cryptomorin.xseries", "de.jexcellence.remapped.com.cryptomorin.xseries")

    configurations = listOf(project.configurations.getByName("runtimeClasspath"))
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.named<Jar>("jar") {
    enabled = false
}

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
