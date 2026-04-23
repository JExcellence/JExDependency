plugins {
    id("raindrop.library-conventions")
}

group = "de.jexcellence.core"
version = "1.0.0"
description = "JExCore API - Public API for third-party plugin integration"

dependencies {
    compileOnly(libs.paper.api)
    // Jackson polymorphic-serialisation annotations — annotations only,
    // no runtime cost for consumers that don't use Jackson.
    compileOnly(libs.jackson.annotations)
}

afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("maven") {
                groupId = "de.jexcellence.core"
                artifactId = "jexcore-api"
                version = project.version.toString()
                pom {
                    name.set("JExCore API")
                    description.set("Public API for JExCore shared core functionality")
                }
            }
        }
    }
}
