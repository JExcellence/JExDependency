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

data class ModuleBuildConfig(
    val name: String,
    val directory: String,
    val buildPropertyKey: String,
)

val trackedModuleBuilds = listOf(
    ModuleBuildConfig("RCore", "RCore", "rcore.version.build"),
    ModuleBuildConfig("RDQ", "RDQ", "rdq.version.build"),
    ModuleBuildConfig("RDR", "RDR", "rdr.version.build"),
    ModuleBuildConfig("RDS", "RDS", "rds.version.build"),
    ModuleBuildConfig("RDT", "RDT", "rdt.version.build"),
)

fun parseSimpleProperties(content: String): Map<String, String> {
    val properties = linkedMapOf<String, String>()
    content.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#")) {
            return@forEach
        }

        val separatorIndex = rawLine.indexOf('=')
        if (separatorIndex <= 0) {
            return@forEach
        }

        val key = rawLine.substring(0, separatorIndex).trim()
        val value = rawLine.substring(separatorIndex + 1).trim()
        properties[key] = value
    }
    return properties
}

fun updateSimplePropertiesFile(file: File, updates: Map<String, String>) {
    val remainingUpdates = updates.toMutableMap()
    val updatedLines = file.readLines().map { rawLine ->
        val trimmed = rawLine.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return@map rawLine
        }

        val separatorIndex = rawLine.indexOf('=')
        if (separatorIndex <= 0) {
            return@map rawLine
        }

        val key = rawLine.substring(0, separatorIndex).trim()
        val newValue = remainingUpdates.remove(key) ?: return@map rawLine
        "$key=$newValue"
    }.toMutableList()

    if (remainingUpdates.isNotEmpty()) {
        if (updatedLines.isNotEmpty() && updatedLines.last().isNotBlank()) {
            updatedLines.add("")
        }
        remainingUpdates.forEach { (key, value) ->
            updatedLines.add("$key=$value")
        }
    }

    val newline = System.lineSeparator()
    file.writeText(updatedLines.joinToString(newline, postfix = newline))
}

fun Project.runGitCommand(vararg args: String): String {
    val process = ProcessBuilder(listOf("git", *args))
        .directory(rootProject.projectDir)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }.trim()
    val exitCode = process.waitFor()

    if (exitCode != 0) {
        throw GradleException(
            "Git command failed: git ${args.joinToString(" ")}" +
                if (output.isBlank()) "" else "\n$output"
        )
    }

    return output
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
        ":syncBuildNumbersFromCommitCounter",
        ":publishDependencies",
        ":RDQ:buildAll",
        ":RDR:buildAll",
        ":RDS:buildAll",
        ":RDT:buildAll",
    )
    
    // Enforce build order
    tasks.findByPath(":RDQ:buildAll")?.mustRunAfter(":publishDependencies")
    tasks.findByPath(":RDR:buildAll")?.mustRunAfter(":publishDependencies")
    tasks.findByPath(":RDS:buildAll")?.mustRunAfter(":publishDependencies")
    tasks.findByPath(":RDT:buildAll")?.mustRunAfter(":publishDependencies")
    
    doLast {
        val major = findProperty("rdq.version.major") ?: "Undefined"
        val minor = findProperty("rdq.version.minor") ?: "Undefined"
        val patch = findProperty("rdq.version.patch") ?: "Undefined"
        val stage = findProperty("rdq.version.stage") ?: "Undefined"
        val build = findProperty("rdq.version.build") ?: "Undefined"
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
        val rdrFreeDir = file("RDR/rdr-free/build/libs")
        val rdrPremiumDir = file("RDR/rdr-premium/build/libs")
        val rdsFreeDir = file("RDS/rds-free/build/libs")
        val rdsPremiumDir = file("RDS/rds-premium/build/libs")
        val rdtFreeDir = file("RDT/rdt-free/build/libs")
        val rdtPremiumDir = file("RDT/rdt-premium/build/libs")
        
        rdqFreeDir.listFiles()?.filter { it.name.endsWith(".jar") && !it.name.contains("sources") && !it.name.contains("javadoc") }?.forEach {
            println("  - ${it.absolutePath}")
        }
        rdqPremiumDir.listFiles()?.filter { it.name.endsWith(".jar") && !it.name.contains("sources") && !it.name.contains("javadoc") }?.forEach {
            println("  - ${it.absolutePath}")
        }
        rdrFreeDir.listFiles()?.filter { it.name.endsWith(".jar") && !it.name.contains("sources") && !it.name.contains("javadoc") }?.forEach {
            println("  - ${it.absolutePath}")
        }
        rdrPremiumDir.listFiles()?.filter { it.name.endsWith(".jar") && !it.name.contains("sources") && !it.name.contains("javadoc") }?.forEach {
            println("  - ${it.absolutePath}")
        }
        rdsFreeDir.listFiles()?.filter { it.name.endsWith(".jar") && !it.name.contains("sources") && !it.name.contains("javadoc") }?.forEach {
            println("  - ${it.absolutePath}")
        }
        rdsPremiumDir.listFiles()?.filter { it.name.endsWith(".jar") && !it.name.contains("sources") && !it.name.contains("javadoc") }?.forEach {
            println("  - ${it.absolutePath}")
        }
        rdtFreeDir.listFiles()?.filter { it.name.endsWith(".jar") && !it.name.contains("sources") && !it.name.contains("javadoc") }?.forEach {
            println("  - ${it.absolutePath}")
        }
        rdtPremiumDir.listFiles()?.filter { it.name.endsWith(".jar") && !it.name.contains("sources") && !it.name.contains("javadoc") }?.forEach {
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
        ":RDR:clean",
        ":RDS:clean",
        ":RDT:clean",
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
 * Validates that the commit counter in gradle.properties never decreases.
 */
tasks.register("validateCommitCounter") {
    group = "verification"
    description = "Validates that gradle.properties commit is not lower than HEAD"

    doLast {
        val propsFile = file("gradle.properties")
        val currentProperties = parseSimpleProperties(propsFile.readText())
        val currentCommit = currentProperties["commit"]?.toIntOrNull()
            ?: throw GradleException("Missing or invalid integer property: commit")

        val committedProperties = runCatching {
            parseSimpleProperties(runGitCommand("show", "HEAD:gradle.properties"))
        }.getOrDefault(emptyMap())

        val committedCommit = committedProperties["commit"]?.toIntOrNull() ?: return@doLast
        if (currentCommit < committedCommit) {
            throw GradleException(
                "commit cannot be lowered (HEAD=$committedCommit, current=$currentCommit)."
            )
        }
    }
}

/**
 * Synchronizes module build numbers when the commit counter increases.
 *
 * This task only updates:
 * - rcore.version.build
 * - rdq.version.build
 * - rdr.version.build
 * - rds.version.build
 * - rdt.version.build
 */
tasks.register("syncBuildNumbersFromCommitCounter") {
    group = "versioning"
    description = "Updates module build numbers from the commit counter delta"

    doLast {
        val propsFile = file("gradle.properties")
        val currentProperties = parseSimpleProperties(propsFile.readText())
        val currentCommit = currentProperties["commit"]?.toIntOrNull()
            ?: throw GradleException("Missing or invalid integer property: commit")

        val committedProperties = runCatching {
            parseSimpleProperties(runGitCommand("show", "HEAD:gradle.properties"))
        }.getOrDefault(emptyMap())

        val committedCommit = committedProperties["commit"]?.toIntOrNull()
        if (committedCommit == null) {
            println("No committed commit baseline found; skipping module build sync.")
            return@doLast
        }

        if (currentCommit < committedCommit) {
            throw GradleException(
                "commit cannot be lowered (HEAD=$committedCommit, current=$currentCommit)."
            )
        }

        if (currentCommit == committedCommit) {
            println("commit unchanged ($currentCommit); module build numbers were not modified.")
            return@doLast
        }

        val delta = currentCommit - committedCommit
        val commitHashes = runGitCommand("rev-list", "--max-count=$delta", "HEAD")
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()

        if (commitHashes.size < delta) {
            throw GradleException(
                "commit increased by $delta, but only ${commitHashes.size} commits are available in HEAD history."
            )
        }

        val changedFilesByCommit = commitHashes.associateWith { hash ->
            runGitCommand("show", "--pretty=format:", "--name-only", "--first-parent", hash)
                .lineSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toSet()
        }

        val updates = linkedMapOf<String, String>()
        val summaryLines = mutableListOf<String>()

        trackedModuleBuilds.forEach { module ->
            val committedBuild = committedProperties[module.buildPropertyKey]?.toIntOrNull()
                ?: throw GradleException("Missing or invalid integer property in HEAD: ${module.buildPropertyKey}")

            val affectedCommitCount = changedFilesByCommit.count { (_, changedFiles) ->
                changedFiles.any { filePath ->
                    filePath == module.directory || filePath.startsWith("${module.directory}/")
                }
            }

            val newBuild = committedBuild + affectedCommitCount
            updates[module.buildPropertyKey] = newBuild.toString()
            summaryLines.add(
                "${module.name}: ${module.buildPropertyKey} $committedBuild -> $newBuild (+$affectedCommitCount)"
            )
        }

        updateSimplePropertiesFile(propsFile, updates)

        println("========================================================================")
        println("commit advanced: $committedCommit -> $currentCommit (delta=$delta)")
        summaryLines.forEach { println("  $it") }
        println("========================================================================")
    }
}

tasks.named("check") {
    dependsOn("validateCommitCounter")
}

/**
 * Configures Git to use the repository hooks in scripts/git-hooks.
 */
tasks.register("installGitHooks") {
    group = "versioning"
    description = "Configures core.hooksPath to use scripts/git-hooks"

    doLast {
        val hookFile = file("scripts/git-hooks/pre-commit")
        if (!hookFile.exists()) {
            throw GradleException("Missing hook file: ${hookFile.path}")
        }

        hookFile.setExecutable(true)
        runGitCommand("config", "core.hooksPath", "scripts/git-hooks")
        println("Configured core.hooksPath to scripts/git-hooks")
    }
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
        val rdrPremiumDir = file("RDR/rdr-premium/build/libs")
        val rdsPremiumDir = file("RDS/rds-premium/build/libs")
        
        val jarsToDeploy = mutableListOf<File>()
        
        rcorePremiumDir.listFiles()?.filter { 
            it.name.endsWith(".jar") && it.name.contains("Premium") && 
            !it.name.contains("sources") && !it.name.contains("javadoc") 
        }?.forEach { jarsToDeploy.add(it) }
        
        rdqPremiumDir.listFiles()?.filter { 
            it.name.endsWith(".jar") && it.name.contains("Premium") && 
            !it.name.contains("sources") && !it.name.contains("javadoc") 
        }?.forEach { jarsToDeploy.add(it) }

        rdrPremiumDir.listFiles()?.filter {
            it.name.endsWith(".jar") && it.name.contains("Premium") &&
            !it.name.contains("sources") && !it.name.contains("javadoc")
        }?.forEach { jarsToDeploy.add(it) }

        rdsPremiumDir.listFiles()?.filter {
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
