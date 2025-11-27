plugins {
    base
}

allprojects {
    group = findProperty("group") as String? ?: "com.raindropcentral"
    version = findProperty("version") as String? ?: "1.0.0"

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

tasks.register("publishAllLocal") {
    group = "publishing"
    description = "Publishes all projects to local Maven repository"
    dependsOn(
        ":RCommands:publishLocal",
        ":RPlatform:publishLocal",
        ":R18n:publishLocal",
        ":RCore:publishLocal",
        ":JECurrency:publishLocal",
        ":RaindropQuests:publishLocal"
    )
    doLast {
        println("=" .repeat(60))
        println("✓ All projects published to local Maven repository")
        println("=" .repeat(60))
    }
}

tasks.register("buildAllPlugins") {
    group = "build"
    description = "Builds all plugin JARs (Free and Premium versions)"
    dependsOn(
        ":RCore:buildAll",
        ":JECurrency:buildAll",
        ":RaindropQuests:buildAll"
    )
    doLast {
        println("=" .repeat(60))
        println("✓ All plugin JARs built successfully")
        println("=" .repeat(60))
    }
}

tasks.register("cleanAll") {
    group = "build"
    description = "Cleans all projects"
    dependsOn(subprojects.map { ":${it.name}:clean" })
    doLast {
        println("✓ All projects cleaned")
    }
}

tasks.register("buildLibraries") {
    group = "build"
    description = "Builds all library projects"
    dependsOn(
        ":RCommands:build",
        ":RPlatform:build",
        ":R18n:build"
    )
    doLast {
        println("✓ All libraries built")
    }
}

tasks.register("status") {
    group = "help"
    description = "Shows project structure and versions"
    doLast {
        println("=" .repeat(60))
        println("Raindrop Plugins - Project Status")
        println("=" .repeat(60))
        println("\nLibraries:")
        println("  - RCommands: ${project(":RCommands").version}")
        println("  - RPlatform: ${project(":RPlatform").version}")
        println("  - R18n: ${project(":R18n").version}")
        println("\nPlugins:")
        println("  - RCore: ${project(":RCore").version}")
        println("  - JECurrency: ${project(":JECurrency").version}")
        println("  - RaindropQuests: ${project(":RaindropQuests").version}")
        println("\nUseful tasks:")
        println("  - buildLibraries: Build all library projects")
        println("  - buildAllPlugins: Build all plugin JARs")
        println("  - publishAllLocal: Publish all to local Maven")
        println("  - cleanAll: Clean all projects")
        println("=" .repeat(60))
    }
}
