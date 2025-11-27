plugins {
    `maven-publish`
    base
}

group = "com.raindropcentral.rdq"
version = "6.0.0"
description = "RDQ - RaindropQuests plugin providing ranks, bounties, and perks"

tasks.register("publishLocal") {
    group = "publishing"
    description = "Publishes all modules to local Maven repository"
    dependsOn(
        ":RDQ:rdq-common:publishMavenPublicationToMavenLocal",
        ":RDQ:rdq-free:publishShadowPublicationToMavenLocal",
        ":RDQ:rdq-premium:publishShadowPublicationToMavenLocal",
    )
    doLast {
        println("✓ Published ${project.group}:rdq-*:${project.version} to local Maven")
    }
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds Free and Premium shaded jars"
    dependsOn(":RDQ:rdq-free:shadowJar", ":RDQ:rdq-premium:shadowJar")
    doLast {
        val free = project(":RDQ:rdq-free").tasks.named<Jar>("shadowJar").get().archiveFile.get().asFile
        val premium = project(":RDQ:rdq-premium").tasks.named<Jar>("shadowJar").get().archiveFile.get().asFile
        println("✓ Built Free: $free")
        println("✓ Built Premium: $premium")
    }
}

tasks.register("testAll") {
    group = "verification"
    description = "Runs all tests across all modules"
    dependsOn(":RDQ:rdq-common:test")
    doLast {
        println("✓ All tests completed")
    }
}
