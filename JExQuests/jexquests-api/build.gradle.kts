plugins {
    id("raindrop.library-conventions")
}

group = "de.jexcellence.quests"
version = "1.0.0"
description = "JExQuests API - Public API for third-party plugin integration"

dependencies {
    compileOnly(libs.paper.api)
    // Jackson polymorphic-serialisation annotations for sealed
    // QuestObjective / (future) public sealed hierarchies. Annotations
    // only — consumers that don't use Jackson pay zero runtime cost.
    compileOnly(libs.jackson.annotations)
}

afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("maven") {
                groupId = "de.jexcellence.quests"
                artifactId = "jexquests-api"
                version = project.version.toString()
                pom {
                    name.set("JExQuests API")
                    description.set("Public API for JExQuests gameplay systems")
                }
            }
        }
    }
}
