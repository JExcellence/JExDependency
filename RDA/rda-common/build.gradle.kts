plugins {
    id("raindrop.library-conventions")
    id("raindrop.dependencies-yml")
}

group = "com.raindropcentral.rda"
version = "1.0.0"
description = "RDA Common - Shared library for Raindrop Abilities"

dependenciesYml {
    usePaperDependencies()
    generatePaperVariant.set(true)
    generateSpigotVariant.set(true)
}

tasks.processResources {
    exclude("plugin.yml", "paper-plugin.yml")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.bundles.jeconfig) { isTransitive = false }
    compileOnly(libs.bundles.inventory)
    compileOnly(project(":JExCommand"))

    compileOnly(platform(libs.hibernate.platform))
    compileOnly(libs.bundles.hibernate)
    compileOnly(libs.jehibernate)
    compileOnly(libs.jetranslate)
    compileOnly(project(":RPlatform"))

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(project(":RPlatform"))
    testImplementation(project(":JExCommand"))
    testImplementation(libs.paper.api)
    testImplementation(libs.bundles.jeconfig) { isTransitive = false }
    testImplementation(libs.bundles.inventory)
    testImplementation(libs.jetranslate)
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation(platform(libs.hibernate.platform))
    testImplementation(libs.bundles.hibernate)
    testImplementation(libs.jehibernate)
}
