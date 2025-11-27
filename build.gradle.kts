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

// ========================================================================
//                    BUILD ORCHESTRATION TASKS
// ========================================================================

/**
 * Publishes all dependencies to Maven Local (required for fresh builds)
 */
tasks.register("publishDependencies") {
    group = "build"
    description = "Publishes all library dependencies to Maven Local"
    
    dependsOn(
        ":JExDependency:publishToMavenLocal",
        ":JExCommand:publishToMavenLocal",
        ":JExTranslate:publishToMavenLocal",
        ":RPlatform:publishToMavenLocal",
        ":JExEconomy:jexeconomy-common:publishToMavenLocal",
        ":RCore:publishLocal"
    )
    
    // Enforce dependency order
    tasks.findByPath(":JExCommand:publishToMavenLocal")?.mustRunAfter(":JExDependency:publishToMavenLocal")
    tasks.findByPath(":JExTranslate:publishToMavenLocal")?.mustRunAfter(":JExCommand:publishToMavenLocal")
    tasks.findByPath(":RPlatform:publishToMavenLocal")?.mustRunAfter(":JExTranslate:publishToMavenLocal")
    tasks.findByPath(":JExEconomy:jexeconomy-common:publishToMavenLocal")?.mustRunAfter(":RPlatform:publishToMavenLocal")
    tasks.findByPath(":RCore:publishLocal")?.mustRunAfter(":JExEconomy:jexeconomy-common:publishToMavenLocal")
    
    doLast {
        println("========================================================================")
        println("✓ All dependencies published to Maven Local")
        println("  Build order: JExDependency → JExCommand → JExTranslate → RPlatform → JExEconomy → RCore")
        println("========================================================================")
    }
}

/**
 * Builds all modules in correct dependency order
 */
tasks.register("buildAll") {
    group = "build"
    description = "Builds all modules in correct dependency order"
    
    dependsOn(
        ":publishDependencies",
        ":RDQ:buildAll"
    )
    
    // Enforce build order
    tasks.findByPath(":RDQ:buildAll")?.mustRunAfter(":publishDependencies")
    
    doLast {
        println("========================================================================")
        println("                    BUILD COMPLETE")
        println("========================================================================")
        println("✓ All modules built successfully!")
        println()
        println("Build artifacts:")
        val rcoreFree = file("RCore/rcore-free/build/libs/RCore-Free-2.0.0.jar")
        val rcorePremium = file("RCore/rcore-premium/build/libs/RCore-Premium-2.0.0.jar")
        val rdqFree = file("RDQ/rdq-free/build/libs/RDQ-Free-6.0.1.jar")
        val rdqPremium = file("RDQ/rdq-premium/build/libs/RDQ-Premium-6.0.1.jar")
        
        if (rcoreFree.exists()) println("  - ${rcoreFree.absolutePath}")
        if (rcorePremium.exists()) println("  - ${rcorePremium.absolutePath}")
        if (rdqFree.exists()) println("  - ${rdqFree.absolutePath}")
        if (rdqPremium.exists()) println("  - ${rdqPremium.absolutePath}")
        println("========================================================================")
    }
}

/**
 * Cleans all modules
 */
tasks.register("cleanAll") {
    group = "build"
    description = "Cleans all modules"
    
    dependsOn(
        ":JExCommand:clean",
        ":JExDependency:clean",
        ":JExEconomy:clean",
        ":JExTranslate:clean",
        ":RPlatform:clean",
        ":RCore:clean",
        ":RDQ:clean"
    )
}

/**
 * Publishes all library modules to Maven Local (alias for publishDependencies)
 */
tasks.register("publishAllToMavenLocal") {
    group = "publishing"
    description = "Publishes all library modules to Maven Local"
    
    dependsOn(":publishDependencies")
}

/**
 * Deploys premium JARs to specified plugin directory
 */
tasks.register("deployPremium") {
    group = "deployment"
    description = "Deploys premium JARs to plugin directory (use -PpluginDir=<path>)"
    
    dependsOn(":buildAll")
    
    doLast {
        val pluginDir = project.findProperty("pluginDir") as String?
        
        if (pluginDir == null) {
            println("========================================================================")
            println("No plugin directory specified!")
            println("Usage: ./gradlew deployPremium -PpluginDir=\"C:\\path\\to\\plugins\"")
            println("========================================================================")
            return@doLast
        }
        
        val targetDir = file(pluginDir)
        if (!targetDir.exists()) {
            println("Creating plugin directory: ${targetDir.absolutePath}")
            targetDir.mkdirs()
        }
        
        println("========================================================================")
        println("                    DEPLOYING PREMIUM JARS")
        println("========================================================================")
        println("Target directory: ${targetDir.absolutePath}")
        println()
        
        val jarsToDeploy = listOf(
            file("RCore/rcore-premium/build/libs/RCore-Premium-2.0.0.jar"),
            file("RDQ/rdq-premium/build/libs/RDQ-Premium-6.0.1.jar")
        )
        
        jarsToDeploy.forEach { jar ->
            if (jar.exists()) {
                val target = file("${targetDir.absolutePath}/${jar.name}")
                jar.copyTo(target, overwrite = true)
                println("✓ Deployed: ${jar.name}")
            } else {
                println("✗ Not found: ${jar.absolutePath}")
            }
        }
        
        println("========================================================================")
    }
}