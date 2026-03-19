plugins {
    `maven-publish`
    id("raindrop.shadow-conventions")
    id("raindrop.dependencies-yml")
}

group = "de.jexcellence.oneblock"
version = "1.0.3"
description = "JExOneblock - Oneblock System"

ext["vendor"] = "JExcellence"

subprojects {
    group = rootProject.group
    version = rootProject.version
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds both Free and Premium editions"
    dependsOn(
        ":JExOneblock:jexoneblock-free:shadowJar",
        ":JExOneblock:jexoneblock-premium:shadowJar"
    )
}

tasks.register("publishLocal") {
    group = "publishing"
    description = "Publishes all modules to local Maven repository"
    dependsOn(
        ":JExOneblock:jexoneblock-common:publishToMavenLocal",
        ":JExOneblock:jexoneblock-free:publishToMavenLocal",
        ":JExOneblock:jexoneblock-premium:publishToMavenLocal",
    )
    doLast {
        println("✓ Published ${project.group}:jexoneblock-*:${project.version} to local Maven")
    }
}
