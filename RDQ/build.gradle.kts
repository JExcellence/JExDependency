plugins {
    `maven-publish`
    base
}

group = "com.raindropcentral.rdq"
version = "6.0.1"
description = "RDQFree plugin for Raindrop Central"
tasks.register("publishLocal") {
    group = "publishing"
    description = "Publishes all modules to local Maven repository"
    dependsOn(
        ":rdq-common:publishToMavenLocal",
        ":rdq-free:publishToMavenLocal",
        ":rdq-premium:publishToMavenLocal",
    )
    doLast {
        println("✓ Published ${project.group}:rdq-*:${project.version} to local Maven")
    }
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds Free and Premium shaded jars"
    dependsOn(":rdq-free:shadowJar", ":rdq-premium:shadowJar")
    doLast {
        val free = project(":rdq-free").tasks.named<Jar>("shadowJar").get().archiveFile.get().asFile
        val premium = project(":rdq-premium").tasks.named<Jar>("shadowJar").get().archiveFile.get().asFile
        println("✓ Built Free: $free")
        println("✓ Built Premium: $premium")
    }
}