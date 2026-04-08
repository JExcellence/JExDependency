/**
 * Common Java configuration for all Raindrop modules
 */

plugins {
    java
    checkstyle
    `maven-publish`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
    withJavadocJar()
}

configure<CheckstyleExtension> {
    toolVersion = "10.17.0"
    configFile = rootProject.file("config/checkstyle/google-javadoc-checks.xml")
    maxWarnings = 0
    isShowViolations = true
}

val verifyProjectPackageDocs = tasks.register("verifyPublicApiPackageDocs") {
    group = "verification"
    description = "Verifies package-info.java exists for packages that declare public top-level API types."

    doLast {
        val sourceRoot = layout.projectDirectory.dir("src/main/java").asFile
        if (!sourceRoot.exists()) {
            return@doLast
        }

        val packagePattern = Regex("""(?m)^\s*package\s+([a-zA-Z0-9_.]+)\s*;""")
        val publicTopLevelTypePattern = Regex(
            """(?m)^public\s+(?:abstract\s+|final\s+|sealed\s+|non-sealed\s+|strictfp\s+)*(?:class|interface|enum|record|@interface)\b"""
        )
        val publicApiPackages = mutableSetOf<String>()

        fileTree(sourceRoot) {
            include("**/*.java")
            exclude("**/package-info.java")
            exclude("**/module-info.java")
        }.files.forEach { javaFile ->
            val content = javaFile.readText()
            if (!publicTopLevelTypePattern.containsMatchIn(content)) {
                return@forEach
            }

            val packageName = packagePattern.find(content)?.groupValues?.get(1) ?: return@forEach
            publicApiPackages.add(packageName)
        }

        val missingPackages = publicApiPackages
            .sorted()
            .filter { packageName ->
                val packageInfoPath = "src/main/java/${packageName.replace('.', '/')}/package-info.java"
                !file(packageInfoPath).isFile
            }

        if (missingPackages.isNotEmpty()) {
            throw GradleException(
                "Missing package-info.java for public API packages in ${project.path}:\n" +
                    missingPackages.joinToString(separator = "\n") { "- $it" }
            )
        }
    }
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
        options.compilerArgs.addAll(
            listOf(
                "-parameters",
                "-Xlint:unchecked",
                "-Xlint:deprecation"
            )
        )
    }

    withType<Javadoc>().configureEach {
        options.encoding = "UTF-8"
        options.memberLevel = JavadocMemberLevel.PUBLIC
        isFailOnError = true
        (options as StandardJavadocDocletOptions).apply {
            addBooleanOption("Werror", true)
            addBooleanOption("Xdoclint:all,-missing", true)
            addStringOption("tag", "apiNote:a:API Note:")
            addStringOption("tag", "implSpec:a:Implementation Requirements:")
            addStringOption("tag", "implNote:a:Implementation Note:")
        }
    }

    withType<Checkstyle>().configureEach {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    named<Checkstyle>("checkstyleMain") {
        source = fileTree("src/main/java") {
            include("**/*.java")
        }
    }

    named("checkstyleTest") {
        enabled = false
    }

    jar {
        manifest {
            attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to (project.findProperty("vendor") ?: "Raindrop Central"),
                "Built-By" to System.getProperty("user.name"),
                "Built-JDK" to System.getProperty("java.version"),
                "Created-By" to "Gradle ${gradle.gradleVersion}"
            )
        }
    }
}

if (rootProject.tasks.findByName("verifyPublicApiPackageDocs") == null) {
    rootProject.tasks.register("verifyPublicApiPackageDocs") {
        group = "verification"
        description = "Verifies package-info.java coverage for all public API packages."
    }
}

if (rootProject.tasks.findByName("verifyGoogleJavaStyle") == null) {
    rootProject.tasks.register("verifyGoogleJavaStyle") {
        group = "verification"
        description = "Runs Google-style Javadoc and public API package documentation verification."
        dependsOn(rootProject.tasks.named("verifyPublicApiPackageDocs"))
    }
}

rootProject.tasks.named("verifyPublicApiPackageDocs") {
    dependsOn(verifyProjectPackageDocs)
}

rootProject.tasks.named("verifyGoogleJavaStyle") {
    dependsOn(tasks.named("checkstyleMain"))
    dependsOn(tasks.named("javadoc"))
    dependsOn(verifyProjectPackageDocs)
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://nexus.neetgames.com/repository/maven-releases/")
    maven("https://repo.auxilor.io/repository/maven-public/")
    maven("https://repo.tcoded.com/releases")
    maven("https://jitpack.io")
    maven("https://repo.opencollab.dev/main/") // Geyser/Floodgate repository
}
