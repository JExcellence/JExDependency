plugins {
    id("java-library")
    id("maven-publish")
}

group = "de.jexcellence.dependency"
version = "2.0.0"

val disableExternalJavadocLinks = providers.gradleProperty("jexdependency.disableExternalJavadocLinks")
    .map { it.toBoolean() }
    .getOrElse(false)

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "PaperMC"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains:annotations:24.1.0")

    implementation("org.ow2.asm:asm:9.7.1")
    implementation("org.ow2.asm:asm-commons:9.7.1")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.21:3.133.2")
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
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
        options {
            this as StandardJavadocDocletOptions
            encoding = "UTF-8"
            charSet = "UTF-8"
            if (!disableExternalJavadocLinks) {
                links(
                    "https://docs.oracle.com/en/java/javase/21/docs/api/",
                    "https://jd.papermc.io/paper/1.21/"
                )
            }
            addStringOption("Xdoclint:none", "-quiet")
        }
    }

    jar {
        manifest {
            attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "JExcellence",
                "Built-By" to System.getProperty("user.name"),
                "Built-JDK" to System.getProperty("java.version"),
                "Created-By" to "Gradle ${gradle.gradleVersion}"
            )
        }
    }

    register<Jar>("fatJar") {
        group = "build"
        description = "Creates a fat JAR with all dependencies"
        archiveClassifier.set("all")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        
        from(sourceSets.main.get().output)

        dependsOn(configurations.runtimeClasspath)
        from({
            configurations.runtimeClasspath.get()
                .filter { it.name.endsWith("jar") }
                .map { zipTree(it) }
        })
    }
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "jexdependency"

            pom {
                name.set("JEDependency")
                description.set("Modern dependency management and plugin architecture for Minecraft servers")
                url.set("https://github.com/jexcellence/JEDependency")
                inceptionYear.set("2025")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }
                
                developers {
                    developer {
                        id.set("jexcellence")
                        name.set("JExcellence")
                        email.set("contact@jexcellence.de")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/jexcellence/JEDependency.git")
                    developerConnection.set("scm:git:ssh://github.com/jexcellence/JEDependency.git")
                    url.set("https://github.com/jexcellence/JEDependency")
                }
                
                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/jexcellence/JEDependency/issues")
                }
            }
        }
    }
    
    repositories {
        mavenLocal()
    }
}

tasks.register("printVersion") {
    group = "help"
    description = "Prints the project version"
    doLast {
        println("Version: ${project.version}")
    }
}

tasks.register("printDependencies") {
    group = "help"
    description = "Prints all project dependencies"
    doLast {
        configurations.compileClasspath.get().forEach { println(it.name) }
    }
}
