plugins {
    id("raindrop.library-conventions")
}

group = "de.jexcellence.economy"
version = "3.0.0"
description = "JExEconomy API - Public API for third-party plugin integration"

dependencies {
    compileOnly(libs.paper.api)
}

afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("maven") {
                groupId = "de.jexcellence.economy"
                artifactId = "jexeconomy-api"
                version = project.version.toString()
                pom {
                    name.set("JExEconomy API")
                    description.set("Public API for JExEconomy multi-currency economy system")
                }
            }
        }
    }
}
