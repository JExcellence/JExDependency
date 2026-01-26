plugins {
    `maven-publish`
    id("raindrop.shadow-conventions")
    id("raindrop.dependencies-yml")
}

group = "de.jexcellence.economy"
version = "2.0.0"
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
        ":JExEconomy:jexeconomy-common:publishToMavenLocal",
        ":JExEconomy:jexeconomy-free:publishToMavenLocal",
        ":JExEconomy:jexeconomy-premium:publishToMavenLocal",
    )
    doLast {
        println("✓ Published ${project.group}:jexeconomy-*:${project.version} to local Maven")
    }
}
