plugins {
    `maven-publish`
    id("raindrop.shadow-conventions")
    id("raindrop.dependencies-yml")
}

group = "de.jexcellence.core"
version = "1.0.0"
description = "JExCore - Clean shared core for JExcellence / Raindrop plugins"

ext["vendor"] = "JExcellence"

subprojects {
    group = rootProject.group
    version = rootProject.version
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds both Free and Premium editions"
    dependsOn(
        ":JExCore:jexcore-free:shadowJar",
        ":JExCore:jexcore-premium:shadowJar"
    )
}

tasks.register("publishLocal") {
    group = "publishing"
    description = "Publishes all JExCore modules to local Maven repository"
    dependsOn(
        ":JExCore:jexcore-api:publishMavenPublicationToMavenLocal",
        ":JExCore:jexcore-common:publishMavenPublicationToMavenLocal",
        ":JExCore:jexcore-free:publishMavenShadowPublicationToMavenLocal",
        ":JExCore:jexcore-premium:publishMavenShadowPublicationToMavenLocal"
    )
    doLast {
        println("Published ${project.group}:jexcore-*:${project.version} to local Maven")
    }
}

afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("maven") {
                artifactId = "jexcore"
            }
        }
    }
}
