plugins {
    id("raindrop.library-conventions")
}

group = "com.raindropcentral.core"
version = "2.0.0"
description = "Core plugin providing shared functionality for Raindrop plugins (common)"

tasks.processResources {
    exclude("plugin.yml", "paper-plugin.yml")
}

dependencies {
    compileOnly(libs.paper.api)

    compileOnly(libs.bundles.adventure)

    compileOnly(libs.folialib)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.vault.api) { isTransitive = false}
    compileOnly(libs.luckperms.api)
    compileOnly(libs.bundles.inventory)

    compileOnly(libs.slf4j.api)
    compileOnly(libs.slf4j.jdk14)
    compileOnly(libs.jboss.logging)

    compileOnly(platform(libs.hibernate.platform))
    compileOnly(libs.bundles.hibernate)

    compileOnly(libs.caffeine)
    compileOnly(libs.jackson.core)
    compileOnly(libs.jackson.databind)
    compileOnly(libs.jackson.annotations)
    compileOnly(libs.jackson.jsr310)
    compileOnly(libs.java.uuid)
    compileOnly(libs.xseries)

    implementation(libs.bundles.jexcellence) { isTransitive = false }
    implementation(libs.bundles.jeconfig) { isTransitive = false }
    implementation(libs.bundles.inventory) { isTransitive = false }

    compileOnly(libs.jexeconomy)
}