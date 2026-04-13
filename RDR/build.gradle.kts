plugins {
    `maven-publish`
    base
}

group = "com.raindropcentral.rdr"
version = "5.0.0"
description = "RDR - Raindrop Reserve"

tasks.register("publishLocal") {
    group = "publishing"
    description = "Publishes all RDR modules to local Maven repository"
    dependsOn(
        ":RDR:rdr-common:publishMavenPublicationToMavenLocal",
        ":RDR:rdr-free:publishShadowPublicationToMavenLocal",
        ":RDR:rdr-premium:publishShadowPublicationToMavenLocal",
    )
    doLast {
        println("✓ Published ${project.group}:rdr-*:${project.version} to local Maven")
    }
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds Free and Premium shaded jars"
    dependsOn(":RDR:rdr-free:shadowJar", ":RDR:rdr-premium:shadowJar")
    doLast {
        val free = project(":RDR:rdr-free").tasks.named<Jar>("shadowJar").get().archiveFile.get().asFile
        val premium = project(":RDR:rdr-premium").tasks.named<Jar>("shadowJar").get().archiveFile.get().asFile
        println("✓ Built Free: $free")
        println("✓ Built Premium: $premium")
    }
}

tasks.register("testAll") {
    group = "verification"
    description = "Runs all tests across all RDR modules"
    dependsOn(
        ":RDR:rdr-common:test",
        ":RDR:rdr-free:test",
        ":RDR:rdr-premium:test",
    )
    doLast {
        println("✓ All RDR tests completed")
    }
}
