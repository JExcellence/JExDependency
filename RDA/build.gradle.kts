plugins {
    `maven-publish`
    base
}

group = "com.raindropcentral.rda"
version = "1.0.0"
description = "RDA - Raindrop Abilities"

tasks.register("publishLocal") {
    group = "publishing"
    description = "Publishes all RDA modules to local Maven repository"
    dependsOn(
        ":RDA:rda-common:publishMavenPublicationToMavenLocal",
        ":RDA:rda-free:publishShadowPublicationToMavenLocal",
        ":RDA:rda-premium:publishShadowPublicationToMavenLocal",
    )
    doLast {
        println("✓ Published ${project.group}:rda-*:${project.version} to local Maven")
    }
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds Free and Premium shaded jars"
    dependsOn(":RDA:rda-free:shadowJar", ":RDA:rda-premium:shadowJar")
    doLast {
        val free = project(":RDA:rda-free").tasks.named<Jar>("shadowJar").get().archiveFile.get().asFile
        val premium = project(":RDA:rda-premium").tasks.named<Jar>("shadowJar").get().archiveFile.get().asFile
        println("✓ Built Free: $free")
        println("✓ Built Premium: $premium")
    }
}

tasks.register("testAll") {
    group = "verification"
    description = "Runs all tests across all RDA modules"
    dependsOn(
        ":RDA:rda-common:test",
        ":RDA:rda-free:test",
        ":RDA:rda-premium:test",
    )
    doLast {
        println("✓ All RDA tests completed")
    }
}
