plugins {
    alias(libs.plugins.shadow) apply false
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
        ":rcore-common:publishToMavenLocal",
        ":rcore-free:publishToMavenLocal",
        ":rcore-premium:publishToMavenLocal",
    )
    doLast {
        println("✓ Published ${project.group}:rcore-*:${project.version} to local Maven")
    }
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds Free and Premium shaded jars"
    dependsOn(":rcore-free:shadowJar", ":rcore-premium:shadowJar")
    doLast {
        val free = project(":rcore-free").tasks.named<Jar>("shadowJar").get().archiveFile.get().asFile
        val premium = project(":rcore-premium").tasks.named<Jar>("shadowJar").get().archiveFile.get().asFile
        println("✓ Built Free: $free")
        println("✓ Built Premium: $premium")
    }
}