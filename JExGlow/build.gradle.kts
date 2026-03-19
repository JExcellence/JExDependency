import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("raindrop.shadow-conventions")
    id("raindrop.dependencies-yml")
}

group = "de.jexcellence.glow"
version = "1.0.0"
description = "Glow Plugin"

dependenciesYml {
    usePaperDependencies()
    generatePaperVariant.set(true)
    generateSpigotVariant.set(true)
}

dependencies {
    compileOnly(libs.paper.api)

    compileOnly(libs.slf4j.api)
    compileOnly(libs.slf4j.jdk14)
    compileOnly(libs.jboss.logging)

    compileOnly(platform(libs.hibernate.platform))
    compileOnly(libs.bundles.hibernate)
    compileOnly(libs.jehibernate)
    compileOnly(libs.adventure.platform.bukkit)
    compileOnly(libs.rplatform)

    implementation(libs.bundles.jexcellence) {
        isTransitive = false
        exclude(group = "de.jexcellence.hibernate")
    }
    implementation(libs.bundles.jeconfig) { isTransitive = false }
    compileOnly(libs.bundles.inventory)
    compileOnly(libs.vault.api) { isTransitive = false }
    compileOnly(libs.placeholderapi)
}

tasks.processResources {
    val props = mapOf(
        "version" to "1.0.0",
        "name" to "JExGlow",
        "description" to project.description,
        "apiVersion" to "1.19"
    )
    inputs.properties(props)
    filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
        expand(props)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("JExGlow")
    archiveVersion.set(project.version.toString())

    // Jackson 3.x core (tools.jackson namespace)
    relocate("tools.jackson", "de.jexcellence.remapped.tools.jackson")
    // NOTE: com.fasterxml is NOT relocated - Hibernate expects original Jackson paths

    relocate("com.github.benmanes", "de.jexcellence.remapped.com.github.benmanes")
    relocate("me.devnatan.inventoryframework", "de.jexcellence.remapped.me.devnatan.inventoryframework")
    relocate("com.tcoded", "de.jexcellence.remapped.com.tcoded")
    relocate("com.cryptomorin.xseries", "de.jexcellence.remapped.com.cryptomorin.xseries")

    configurations = listOf(project.configurations.getByName("runtimeClasspath"))
    mergeServiceFiles()
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}

tasks.named<Jar>("jar") {
    enabled = false
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("shadow") {
                artifact(tasks.named("shadowJar"))
                artifact(tasks.named("sourcesJar"))
                artifact(tasks.named("javadocJar"))
                groupId = "de.jexcellence.glow"
                version = "2.0.0"
                pom {
                    name.set("JExGlow")
                    description.set("JExGlow")
                }
            }
        }
    }
}