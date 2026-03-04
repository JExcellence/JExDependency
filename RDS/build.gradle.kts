plugins {
    `maven-publish`
    base
}

group = "com.raindropcentral.rds"
version = "1.0.0"
description = "RDS - Raindrop Shops"

tasks.register("publishLocal") {
    group = "publishing"
    description = "Publishes all RDS modules to local Maven repository"
    dependsOn(
        ":RDS:rds-common:publishMavenPublicationToMavenLocal",
        ":RDS:rds-free:publishShadowPublicationToMavenLocal",
        ":RDS:rds-premium:publishShadowPublicationToMavenLocal",
    )
    doLast {
        println("✓ Published ${project.group}:rds-*:${project.version} to local Maven")
    }
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds Free and Premium shaded jars"
    dependsOn(":RDS:rds-free:shadowJar", ":RDS:rds-premium:shadowJar")
    doLast {
        val free = project(":RDS:rds-free").tasks.named<Jar>("shadowJar").get().archiveFile.get().asFile
        val premium = project(":RDS:rds-premium").tasks.named<Jar>("shadowJar").get().archiveFile.get().asFile
        println("✓ Built Free: $free")
        println("✓ Built Premium: $premium")
    }
}

tasks.register("testAll") {
    group = "verification"
    description = "Runs all tests across all RDS modules"
    dependsOn(
        ":RDS:rds-common:test",
        ":RDS:rds-free:test",
        ":RDS:rds-premium:test",
    )
    doLast {
        println("✓ All RDS tests completed")
    }
}
