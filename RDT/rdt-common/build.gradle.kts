plugins {
    id("raindrop.library-conventions")
    id("raindrop.dependencies-yml")
}

group = "com.raindropcentral.rdt"
version = "1.0.0"
description = "RDT Common - Shared library for Raindrop Towns"

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
    compileOnly(libs.jackson.core)
    compileOnly(libs.jackson.databind)
    compileOnly(libs.jackson.annotations)
    compileOnly(libs.jackson.jsr310)

    compileOnly(platform(libs.hibernate.platform))
    compileOnly(libs.bundles.hibernate)
    compileOnly(libs.jehibernate)
    compileOnly(libs.adventure.platform.bukkit)
    compileOnly(project(":RPlatform"))

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
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(project(":JExCommand"))
    testImplementation(project(":RPlatform"))
    testImplementation(libs.paper.api)
    testCompileOnly(libs.jackson.annotations)
    testImplementation(platform(libs.hibernate.platform))
    testImplementation(libs.bundles.hibernate)
    testImplementation(libs.jehibernate)
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
