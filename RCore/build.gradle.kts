plugins {
    `maven-publish`
    base
}

group = "com.raindropcentral.core"
version = "2.0.0"
description = "Core plugin providing shared functionality for Raindrop plugins"

tasks.register("publishLocal") {
    group = "publishing"
    description = "Publishes all modules to local Maven repository"
    dependsOn(
        ":RCore:rcore-common:publishToMavenLocal",
        ":RCore:rcore-free:publishToMavenLocal",
        ":RCore:rcore-premium:publishToMavenLocal",
    )
    doLast {
        println("✓ Published ${project.group}:rcore-*:${project.version} to local Maven")
    }
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds Free and Premium shaded jars"
    dependsOn(":RCore:rcore-free:shadowJar", ":RCore:rcore-premium:shadowJar")
    doLast {
        val free = project(":RCore:rcore-free").tasks.named<Jar>("shadowJar").get().archiveFile.get().asFile
        val premium = project(":RCore:rcore-premium").tasks.named<Jar>("shadowJar").get().archiveFile.get().asFile
        println("✓ Built Free: $free")
        println("✓ Built Premium: $premium")
    }
}
