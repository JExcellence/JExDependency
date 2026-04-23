plugins {
    id("raindrop.library-conventions")
    id("raindrop.dependencies-yml")
}

group = "de.jexcellence.core"
version = "1.0.0"
description = "JExCore Common - Shared library for JExCore"

dependenciesYml {
    usePaperDependencies()
    generatePaperVariant.set(true)
    generateSpigotVariant.set(true)
}

tasks.processResources {
    exclude("plugin.yml", "paper-plugin.yml")
}

dependencies {
    implementation(project(":JExCore:jexcore-api"))

    compileOnly(libs.paper.api)
    compileOnly(libs.bundles.adventure)

    compileOnly(libs.folialib)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.vault.api) { isTransitive = false }
    compileOnly(libs.luckperms.api)

    compileOnly(libs.slf4j.api)
    compileOnly(libs.slf4j.jdk14)
    compileOnly(libs.jboss.logging)

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
    compileOnly(libs.bundles.jeconfig) { isTransitive = false }
    compileOnly(libs.bundles.inventory)

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

afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("maven") {
                groupId = "de.jexcellence.core"
                artifactId = "jexcore-common"
                version = project.version.toString()
                pom {
                    name.set("JExCore Common")
                    description.set(project.description)
                }
            }
        }
    }
}
