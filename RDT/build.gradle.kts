plugins {
    `maven-publish`
    base
}

group = "com.raindropcentral.rdt"
version = "1.0.0"
description = "RDT - Raindrop Towns"

tasks.register("publishLocal") {
    group = "publishing"
    description = "Publishes all RDT modules to local Maven repository"
    dependsOn(
        ":RDT:rdt-common:publishMavenPublicationToMavenLocal",
        ":RDT:rdt-free:publishShadowPublicationToMavenLocal",
        ":RDT:rdt-premium:publishShadowPublicationToMavenLocal",
    )
    doLast {
        println("✓ Published ${project.group}:rdt-*:${project.version} to local Maven")
    }
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds Free and Premium shaded jars"
    dependsOn(":RDT:rdt-free:shadowJar", ":RDT:rdt-premium:shadowJar")
    doLast {
        val free = project(":RDT:rdt-free").tasks.named<Jar>("shadowJar").get().archiveFile.get().asFile
        val premium = project(":RDT:rdt-premium").tasks.named<Jar>("shadowJar").get().archiveFile.get().asFile
        println("✓ Built Free: $free")
        println("✓ Built Premium: $premium")
    }
}

tasks.register("testAll") {
    group = "verification"
    description = "Runs all tests across all RDT modules"
    dependsOn(
        ":RDT:rdt-common:test",
        ":RDT:rdt-free:test",
        ":RDT:rdt-premium:test",
    )
    doLast {
        println("✓ All RDT tests completed")
    }
}
