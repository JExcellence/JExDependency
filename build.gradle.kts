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
        ":RDQ:buildAll",
    )
    
    // Enforce build order
    tasks.findByPath(":RDQ:buildAll")?.mustRunAfter(":publishDependencies")
    
    doLast {
        val major = findProperty("rdq.version.major") ?: "6"
        val minor = findProperty("rdq.version.minor") ?: "0"
        val patch = findProperty("rdq.version.patch") ?: "0"
        val stage = findProperty("rdq.version.stage") ?: "Alpha"
        val build = findProperty("rdq.version.build") ?: "1"
        val rdqVersion = "$major.$minor.$patch-$stage-Build-$build"
        
        println("========================================================================")
        println("                    BUILD COMPLETE")
        println("========================================================================")
        println("✓ All modules built successfully!")
        println("  RDQ Version: $rdqVersion")
        println()
        println("Build artifacts:")
        
        // Find RDQ jars dynamically
        val rdqFreeDir = file("RDQ/rdq-free/build/libs")
        val rdqPremiumDir = file("RDQ/rdq-premium/build/libs")
        
        rdqFreeDir.listFiles()?.filter { it.name.endsWith(".jar") && !it.name.contains("sources") && !it.name.contains("javadoc") }?.forEach {
            println("  - ${it.absolutePath}")
        }
        rdqPremiumDir.listFiles()?.filter { it.name.endsWith(".jar") && !it.name.contains("sources") && !it.name.contains("javadoc") }?.forEach {
            println("  - ${it.absolutePath}")
        }
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
        ":RDQ:clean",
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
 * Increments the RDQ build number in gradle.properties
 */
tasks.register("incrementBuildNumber") {
    group = "versioning"
    description = "Increments the RDQ build number"
    
    doLast {
        val propsFile = file("gradle.properties")
        val props = java.util.Properties()
        propsFile.inputStream().use { props.load(it) }
        
        val currentBuild = (props.getProperty("rdq.version.build") ?: "0").toInt()
        val newBuild = currentBuild + 1
        props.setProperty("rdq.version.build", newBuild.toString())
        
        propsFile.outputStream().use { props.store(it, null) }
        
        val major = props.getProperty("rdq.version.major") ?: "6"
        val minor = props.getProperty("rdq.version.minor") ?: "0"
        val patch = props.getProperty("rdq.version.patch") ?: "0"
        val stage = props.getProperty("rdq.version.stage") ?: "Alpha"
        
        println("========================================================================")
        println("✓ Build number incremented: $currentBuild → $newBuild")
        println("  New version: RDQ-$major.$minor.$patch-$stage-Build-$newBuild")
        println("========================================================================")
    }
}

/**
 * Sets the RDQ version stage (Alpha, Beta, RC, Release)
 * Usage: ./gradlew setVersionStage -Pstage=Beta
 */
tasks.register("setVersionStage") {
    group = "versioning"
    description = "Sets the RDQ version stage (Alpha, Beta, RC, Release)"
    
    doLast {
        val newStage = project.findProperty("stage") as String?
        if (newStage == null) {
            println("Usage: ./gradlew setVersionStage -Pstage=<Alpha|Beta|RC|Release>")
            return@doLast
        }
        
        val validStages = listOf("Alpha", "Beta", "RC", "Release")
        if (newStage !in validStages) {
            println("Invalid stage: $newStage. Valid stages: $validStages")
            return@doLast
        }
        
        val propsFile = file("gradle.properties")
        val props = java.util.Properties()
        propsFile.inputStream().use { props.load(it) }
        
        props.setProperty("rdq.version.stage", newStage)
        // Reset build number when changing stage
        props.setProperty("rdq.version.build", "1")
        
        propsFile.outputStream().use { props.store(it, null) }
        
        val major = props.getProperty("rdq.version.major") ?: "6"
        val minor = props.getProperty("rdq.version.minor") ?: "0"
        val patch = props.getProperty("rdq.version.patch") ?: "0"
        
        println("========================================================================")
        println("✓ Version stage set to: $newStage")
        println("  New version: RDQ-$major.$minor.$patch-$newStage-Build-1")
        println("========================================================================")
    }
}

/**
 * Bumps the RDQ version (major, minor, or patch)
 * Usage: ./gradlew bumpVersion -Pbump=minor
 */
tasks.register("bumpVersion") {
    group = "versioning"
    description = "Bumps the RDQ version (major, minor, or patch)"
    
    doLast {
        val bump = project.findProperty("bump") as String? ?: "patch"
        
        val propsFile = file("gradle.properties")
        val props = java.util.Properties()
        propsFile.inputStream().use { props.load(it) }
        
        var major = (props.getProperty("rdq.version.major") ?: "6").toInt()
        var minor = (props.getProperty("rdq.version.minor") ?: "0").toInt()
        var patch = (props.getProperty("rdq.version.patch") ?: "0").toInt()
        
        when (bump) {
            "major" -> { major++; minor = 0; patch = 0 }
            "minor" -> { minor++; patch = 0 }
            "patch" -> { patch++ }
            else -> {
                println("Invalid bump type: $bump. Use: major, minor, or patch")
                return@doLast
            }
        }
        
        props.setProperty("rdq.version.major", major.toString())
        props.setProperty("rdq.version.minor", minor.toString())
        props.setProperty("rdq.version.patch", patch.toString())
        props.setProperty("rdq.version.build", "1") // Reset build number
        
        propsFile.outputStream().use { props.store(it, null) }
        
        val stage = props.getProperty("rdq.version.stage") ?: "Alpha"
        
        println("========================================================================")
        println("✓ Version bumped ($bump): $major.$minor.$patch")
        println("  New version: RDQ-$major.$minor.$patch-$stage-Build-1")
        println("========================================================================")
    }
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
        
        // Find and deploy premium JARs dynamically
        val rcorePremiumDir = file("RCore/rcore-premium/build/libs")
        val rdqPremiumDir = file("RDQ/rdq-premium/build/libs")
        
        val jarsToDeploy = mutableListOf<File>()
        
        rcorePremiumDir.listFiles()?.filter { 
            it.name.endsWith(".jar") && it.name.contains("Premium") && 
            !it.name.contains("sources") && !it.name.contains("javadoc") 
        }?.forEach { jarsToDeploy.add(it) }
        
        rdqPremiumDir.listFiles()?.filter { 
            it.name.endsWith(".jar") && it.name.contains("Premium") && 
            !it.name.contains("sources") && !it.name.contains("javadoc") 
        }?.forEach { jarsToDeploy.add(it) }
        
        jarsToDeploy.forEach { jar ->
            val target = file("${targetDir.absolutePath}/${jar.name}")
            jar.copyTo(target, overwrite = true)
            println("✓ Deployed: ${jar.name}")
        }
        
        if (jarsToDeploy.isEmpty()) {
            println("✗ No premium JARs found to deploy")
        }
        
        println("========================================================================")
    }
}