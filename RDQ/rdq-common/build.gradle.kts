plugins {
    id("raindrop.library-conventions")
    id("raindrop.dependencies-yml")
}

group = "com.raindropcentral.rdq"
version = "6.0.0"
description = "RDQ Common - Shared library for RaindropQuests"

dependenciesYml {
    usePaperDependencies()
    generatePaperVariant.set(true)
    generateSpigotVariant.set(true)
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    exclude("plugin.yml", "paper-plugin.yml")
}

dependencies {
    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    // Paper
    compileOnly(libs.paper.api)
    compileOnly(libs.bundles.adventure)
    compileOnly(project(":JExEconomy:jexeconomy-common"))
    compileOnly(libs.folialib)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.vault.api) { isTransitive = false }
    compileOnly(libs.luckperms.api)

    // Logging & utils
    compileOnly(libs.slf4j.api)
    compileOnly(libs.slf4j.jdk14)
    compileOnly(libs.jboss.logging)

    // DB & platform (compileOnly - provided by JExHibernate)
    compileOnly(platform(libs.hibernate.platform))
    compileOnly(libs.bundles.hibernate)
    compileOnly(libs.jehibernate)

    compileOnly(libs.caffeine)
    compileOnly(libs.jackson.core)
    compileOnly(libs.jackson.databind)
    compileOnly(libs.jackson.annotations)
    compileOnly(libs.jackson.jsr310)
    compileOnly(libs.java.uuid)
    compileOnly(libs.xseries)

    compileOnly(libs.bundles.jexcellence) {
        exclude(group = "de.jexcellence.hibernate")
        isTransitive = false
    }
    compileOnly(project(":RCore"))
    compileOnly(libs.bundles.jeconfig) { isTransitive = false }
    compileOnly(libs.bundles.inventory)

    // Test dependencies
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
    testImplementation(libs.caffeine)
    testImplementation(platform(libs.hibernate.platform))
    testImplementation(libs.bundles.hibernate)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf(
        "--enable-preview"
    ))
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-preview")
}
