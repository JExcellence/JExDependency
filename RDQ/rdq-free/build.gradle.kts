plugins {
    id("raindrop.shadow-conventions")
}

group = "com.raindropcentral.rdq"
version = "6.0.0"
description = "RDQ Free - Free edition of RaindropQuests"

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
        "name" to "RDQFree",
        "description" to project.description,
        "apiVersion" to "1.21"
    )
    inputs.properties(props)
    filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
        expand(props)
    }
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("RDQFree")
    archiveClassifier.set("")

    dependencies {
        include(project(":RDQ:rdq-common"))
        // JExcellence libraries
        include(dependency("de.jexcellence.translate:.*"))
        include(dependency("de.jexcellence.hibernate:.*"))
        include(dependency("de.jexcellence.dependency:.*"))
        include(dependency("de.jexcellence.config:.*"))
        // Raindrop libraries
        include(dependency("com.raindropcentral.platform:.*"))
        include(dependency("com.raindropcentral.commands:.*"))
        // Inventory framework
        include(dependency("me.devnatan:.*"))
    }

    // Relocations must be outside dependencies block
    relocate("tools.jackson.core", "de.jexcellence.remapped.tools.jackson.core")
    relocate("tools.jackson.databind", "de.jexcellence.remapped.tools.jackson.databind")
    relocate("com.fasterxml.jackson.annotation", "de.jexcellence.remapped.com.fasterxml.jackson.annotation")
    relocate("tools.jackson.core.datatype", "de.jexcellence.remapped.tools.jackson.core.datatype")
    relocate("com.github.benmanes", "de.jexcellence.remapped.com.github.benmanes")
    relocate("org.h2", "de.jexcellence.remapped.org.h2")
    relocate("me.devnatan.inventoryframework", "de.jexcellence.remapped.me.devnatan.inventoryframework")
    relocate("com.tcoded", "de.jexcellence.remapped.com.tcoded")
    relocate("com.cryptomorin.xseries", "de.jexcellence.remapped.com.cryptomorin.xseries")

    minimize {
        exclude(project(":RDQ:rdq-common"))
    }
    
    // Explicitly include resources from rdq-common (translations, configs, etc.)
    from(project(":RDQ:rdq-common").sourceSets.main.get().resources)
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
            artifactId = "rdq-free"
            version = project.version.toString()
            pom {
                name.set("RDQ Free")
                description.set(project.description)
            }
        }
    }
}
