import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("raindrop.shadow-conventions")
    id("raindrop.dependencies-yml")
}

group = "de.jexcellence.home"
version = "1.0.0"
description = "JExHome Free - Free edition of JExHome"

dependenciesYml {
    usePaperDependencies()
    generatePaperVariant.set(true)
    generateSpigotVariant.set(true)
}

dependencies {
    implementation(project(":JExHome:jexhome-common"))

    // Server API
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

    // Logging
    compileOnly(libs.slf4j.api)
    compileOnly(libs.slf4j.jdk14)
    compileOnly(libs.jboss.logging)

    // DB (compileOnly - provided by JEHibernate)
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

    // Test dependencies
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.mockbukkit)
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version,
        "name" to "JExHome",
        "description" to project.description,
        "apiVersion" to "1.21"
    )
    inputs.properties(props)
    filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
        expand(props)
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("JExHome")
    archiveClassifier.set("Free")
    archiveVersion.set(project.version.toString())

    // Jackson 3.x core (tools.jackson namespace)
    relocate("tools.jackson", "de.jexcellence.remapped.tools.jackson")

    relocate("com.github.benmanes", "de.jexcellence.remapped.com.github.benmanes")
    relocate("me.devnatan.inventoryframework", "de.jexcellence.remapped.me.devnatan.inventoryframework")
    relocate("com.tcoded", "de.jexcellence.remapped.com.tcoded")
    relocate("com.cryptomorin.xseries", "de.jexcellence.remapped.com.cryptomorin.xseries")

    configurations = listOf(project.configurations.getByName("runtimeClasspath"))
    mergeServiceFiles()

    from(project(":JExHome:jexhome-common").sourceSets.main.get().resources)
}

tasks.build {
    dependsOn(tasks.shadowJar)
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
            artifactId = "jexhome-free"
            version = project.version.toString()
            pom {
                name.set("JExHome Free")
                description.set("JExHome Free Edition")
            }
        }
    }
}
