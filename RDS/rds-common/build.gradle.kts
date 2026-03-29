plugins {
    id("raindrop.library-conventions")
    id("raindrop.dependencies-yml")
}

group = "com.raindropcentral.rds"
version = "1.0.0"
description = "RDS Common - Shared library for Raindrop Shops"

dependenciesYml {
    usePaperDependencies()
    generatePaperVariant.set(true)
    generateSpigotVariant.set(true)
}

dependencies {
    compileOnly(project(":JExCommand"))
    compileOnly(libs.paper.api)

    compileOnly(libs.slf4j.api)
    compileOnly(libs.slf4j.jdk14)
    compileOnly(libs.jboss.logging)

    compileOnly(platform(libs.hibernate.platform))
    compileOnly(libs.bundles.hibernate)
    compileOnly(libs.jehibernate)
    compileOnly(libs.adventure.platform.bukkit)
    compileOnly(project(":RPlatform"))

    compileOnly(libs.caffeine)
    compileOnly(libs.jackson.core)
    compileOnly(libs.jackson.databind)
    compileOnly(libs.jackson.annotations)
    compileOnly(libs.jackson.jsr310)

    compileOnly(libs.bundles.jexcellence) {
        isTransitive = false
        exclude(group = "de.jexcellence.hibernate")
        exclude(group = "com.raindropcentral.commands", module = "jexcommand")
    }
    compileOnly(libs.bundles.jeconfig) { isTransitive = false }
    compileOnly(libs.bundles.inventory)
    compileOnly(libs.vault.api) { isTransitive = false }
    compileOnly(libs.placeholderapi)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(project(":JExCommand"))
    testImplementation(project(":RPlatform"))
    testImplementation(libs.paper.api)
    testImplementation(platform(libs.hibernate.platform))
    testImplementation(libs.bundles.hibernate)
    testImplementation(libs.jehibernate)
    testImplementation(libs.jackson.databind)
    testImplementation(libs.bundles.jexcellence) {
        isTransitive = false
        exclude(group = "de.jexcellence.hibernate")
        exclude(group = "com.raindropcentral.commands", module = "jexcommand")
    }
    testImplementation(libs.bundles.jeconfig) { isTransitive = false }
    testImplementation(libs.bundles.inventory)
}

tasks.processResources {
    exclude("plugin.yml", "paper-plugin.yml")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
