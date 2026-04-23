plugins {
    id("raindrop.library-conventions")
}

group = "de.jexcellence.core"
version = "1.0.0"
description = "JExCore Stats - Statistics delivery engine for JExCore"

dependencies {
    implementation(project(":JExCore:jexcore-api"))
    compileOnly(project(":JExCore:jexcore-common"))

    compileOnly(libs.paper.api)
    compileOnly(libs.bundles.adventure)

    compileOnly(libs.slf4j.api)
    compileOnly(libs.slf4j.jdk14)
    compileOnly(libs.jackson.core)
    compileOnly(libs.jackson.databind)
    compileOnly(libs.jackson.annotations)

    compileOnly(libs.bundles.jexcellence) {
        exclude(group = "de.jexcellence.hibernate")
        isTransitive = false
    }

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockito.core)
    testImplementation(project(":JExCore:jexcore-common"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("maven") {
                groupId = "de.jexcellence.core"
                artifactId = "jexcore-stats"
                version = project.version.toString()
                pom {
                    name.set("JExCore Stats")
                    description.set(project.description)
                }
            }
        }
    }
}
