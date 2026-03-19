plugins {
    id("raindrop.shadow-conventions")
    id("raindrop.dependencies-yml")
    `maven-publish`
}

group = "de.jexcellence.home"
version = "1.0.0"
description = "JExHome - Home teleportation system"

ext["vendor"] = "JExcellence"

subprojects {
    group = rootProject.group
    version = rootProject.version
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds both Free and Premium editions"
    dependsOn(
        ":JExHome:jexhome-free:shadowJar",
        ":JExHome:jexhome-premium:shadowJar"
    )
}

tasks.register("publishLocal") {
    group = "publishing"
    description = "Publishes all modules to local Maven repository"
    dependsOn(
        ":JExHome:jexhome-common:publishToMavenLocal",
        ":JExHome:jexhome-free:publishToMavenLocal",
        ":JExHome:jexhome-premium:publishToMavenLocal",
    )
    doLast {
        println("✓ Published ${project.group}:jexhome-*:${project.version} to local Maven")
    }
}
