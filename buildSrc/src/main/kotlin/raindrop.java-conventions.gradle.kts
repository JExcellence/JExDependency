/**
 * Common Java configuration for all Raindrop modules
 */
plugins {
    java
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

    javadoc {
        options.encoding = "UTF-8"
        (options as StandardJavadocDocletOptions).apply {
            addStringOption("Xdoclint:none", "-quiet")
            addBooleanOption("Xdoclint:none", true)
            addStringOption("Xmaxwarns", "0")
            addStringOption("Xmaxerrs", "0")
            // Ignore missing references and network errors
            addBooleanOption("-ignore-source-errors", true)
            isFailOnError = false
            // Skip external links to avoid network dependency
            // links() intentionally omitted for offline builds
        }
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
