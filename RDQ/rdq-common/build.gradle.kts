plugins {
    id("raindrop.library-conventions")
}

version = "6.0.1"

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    exclude("plugin.yml", "paper-plugin.yml")
}

dependencies {
    // Server API
    compileOnly(libs.paper.api)

    // Adventure APIs
    compileOnly(libs.bundles.adventure)

    compileOnly("com.raindropcentral.core:rcore-common:2.0.0")
    compileOnly("com.raindropcentral.core:rcore-free:2.0.0")
    compileOnly("de.jexcellence.economy:jexeconomy:2.0.0")

    // Ecosystem (provided by other plugins)
    compileOnly(libs.folialib)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.vault.api) { isTransitive = false}
    compileOnly(libs.luckperms.api)

    // Logging & utils
    compileOnly(libs.slf4j.api)
    compileOnly(libs.slf4j.jdk14)
    compileOnly(libs.jboss.logging)

    // DB & platform (compileOnly)
    compileOnly(platform(libs.hibernate.platform))
    compileOnly(libs.bundles.hibernate)

    // Misc (compileOnly)
    compileOnly(libs.caffeine)
    compileOnly(libs.jackson.core)
    compileOnly(libs.jackson.databind)
    compileOnly(libs.jackson.annotations)
    compileOnly(libs.jackson.jsr310)
    compileOnly(libs.java.uuid)
    compileOnly(libs.xseries)

    // Internal libraries to be shaded by variants
    implementation(libs.bundles.jexcellence)
    implementation(libs.jehibernate)
    implementation(libs.bundles.jeconfig) { isTransitive = false }
    implementation(libs.bundles.inventory)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.mockbukkit)
    testImplementation(libs.jackson.databind)
    testImplementation(libs.adventure.api)
    testImplementation(libs.adventure.minimessage)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}