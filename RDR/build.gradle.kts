import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("raindrop.shadow-conventions")
    id("raindrop.dependencies-yml")
}

group = "com.raindropcentral.rdr"
version = "5.0.0"
description = "Storage utility plugin for RaindropPlugins"

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
    implementation(libs.jackson.databind)

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

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.paper.api)
    testImplementation(platform(libs.hibernate.platform))
    testImplementation(libs.bundles.hibernate)
    testImplementation(libs.bundles.inventory)
}

tasks.processResources {
    val props = mapOf(
        "version" to "5.0.0",
        "name" to "RDR",
        "description" to project.description,
        "apiVersion" to "1.19"
    )
    inputs.properties(props)
    filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
        expand(props)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("RDR")
    archiveVersion.set(project.version.toString())

    // Jackson 3.x core (tools.jackson namespace)
    relocate("tools.jackson", "com.raindropcentral.remapped.tools.jackson")
    // NOTE: com.fasterxml is NOT relocated - Hibernate expects original Jackson paths

    relocate("com.github.benmanes", "com.raindropcentral.remapped.com.github.benmanes")
    relocate("me.devnatan.inventoryframework", "com.raindropcentral.remapped.me.devnatan.inventoryframework")
    relocate("com.tcoded", "com.raindropcentral.remapped.com.tcoded")
    relocate("com.cryptomorin.xseries", "com.raindropcentral.remapped.com.cryptomorin.xseries")

    configurations = listOf(project.configurations.getByName("runtimeClasspath"))
    mergeServiceFiles()
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.named<Jar>("jar") {
    enabled = false
}

afterEvaluate {
    publishing {
        publications {
            remove(findByName("maven"))
            create<MavenPublication>("mavenShadow") {
                from(components["shadow"])
                groupId = "com.raindropcentral.rdr"
                version = "5.0.0"
                pom {
                    name.set("RDR")
                    description.set("RDR")
                }
            }
        }
    }
}
