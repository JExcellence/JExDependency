plugins {
    `maven-publish`
    id("raindrop.shadow-conventions")
    id("raindrop.dependencies-yml")
}

group = "de.jexcellence.quests"
version = "1.0.0"
description = "JExQuests - Quests, ranks, bounties, perks, and machines for the JExcellence ecosystem"

ext["vendor"] = "JExcellence"

subprojects {
    group = rootProject.group
    version = rootProject.version
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds both Free and Premium editions"
    dependsOn(
        ":JExQuests:jexquests-free:shadowJar",
        ":JExQuests:jexquests-premium:shadowJar"
    )
}

tasks.register("publishLocal") {
    group = "publishing"
    description = "Publishes all JExQuests modules to local Maven repository"
    dependsOn(
        ":JExQuests:jexquests-api:publishMavenPublicationToMavenLocal",
        ":JExQuests:jexquests-common:publishMavenPublicationToMavenLocal",
        ":JExQuests:jexquests-free:publishMavenShadowPublicationToMavenLocal",
        ":JExQuests:jexquests-premium:publishMavenShadowPublicationToMavenLocal"
    )
    doLast {
        println("Published ${project.group}:jexquests-*:${project.version} to local Maven")
    }
}

afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("maven") {
                artifactId = "jexquests"
            }
        }
    }
}
