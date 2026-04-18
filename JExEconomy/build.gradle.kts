plugins {
    `maven-publish`
    id("raindrop.shadow-conventions")
    id("raindrop.dependencies-yml")
}

group = "de.jexcellence.economy"
version = "3.0.0"
description = "JExEconomy - Multi-currency economy system"

ext["vendor"] = "JExcellence"

subprojects {
    group = rootProject.group
    version = rootProject.version
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds both Free and Premium editions"
    dependsOn(
        ":JExEconomy:jexeconomy-free:shadowJar",
        ":JExEconomy:jexeconomy-premium:shadowJar"
    )
}

tasks.register("publishLocal") {
    group = "publishing"
    description = "Publishes all modules to local Maven repository"
    dependsOn(
        ":JExEconomy:jexeconomy-api:publishMavenPublicationToMavenLocal",
        ":JExEconomy:jexeconomy-common:publishMavenPublicationToMavenLocal",
        ":JExEconomy:jexeconomy-free:publishMavenShadowPublicationToMavenLocal",
        ":JExEconomy:jexeconomy-premium:publishMavenShadowPublicationToMavenLocal",
    )
    doLast {
        println("✓ Published ${project.group}:jexeconomy-*:${project.version} to local Maven")
    }
}

afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("maven") {
                artifactId = "jexeconomy"
            }
        }
    }
}
