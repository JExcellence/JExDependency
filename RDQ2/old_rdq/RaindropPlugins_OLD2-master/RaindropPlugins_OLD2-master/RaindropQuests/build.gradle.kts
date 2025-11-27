import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.6"
}

group = "com.raindropcentral.rdq"
version = "6.0.0"
description = "RaindropQuests - Advanced quest and progression system for Minecraft"

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
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-legacy:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-json:4.17.0")
    compileOnly("net.kyori:adventure-text-serializer-plain:4.17.0")
    compileOnly("net.kyori:adventure-platform-bukkit:4.3.4")

    implementation("de.jexcellence.polyglot:polyglot-api:1.0.0")
    implementation("com.raindropcentral.commands:rcommands:1.0.0")
    implementation("com.raindropcentral.rplatform:rplatform:1.0.0")
    implementation("de.jexcellence.config:Evaluable:1.0.0")
    implementation("de.jexcellence.config:GPEEE:1.0.0")
    implementation("de.jexcellence.config:ConfigMapper:1.0.0")
    implementation("de.jexcellence.hibernate:JEHibernate:1.0.0")
    implementation("de.jexcellence.dependency:JEDependency:2.0.0")
    compileOnly("com.raindropcentral.rcore:rcore:2.0.0")

    compileOnly(platform("org.hibernate.orm:hibernate-platform:6.6.4.Final"))
    compileOnly("org.hibernate.orm:hibernate-core")
    compileOnly("jakarta.transaction:jakarta.transaction-api")
    compileOnly("com.mysql:mysql-connector-j:9.2.0")
    compileOnly("com.h2database:h2:2.3.232")
    compileOnly("com.tcoded:FoliaLib:0.5.1")

    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("net.luckperms:api:5.5")
    compileOnly("dev.aurelium:auraskills-api-bukkit:2.2.4")
    compileOnly("com.willfp:EcoJobs:3.74.0")
    compileOnly("com.willfp:EcoSkills:3.64.0")
    compileOnly("com.willfp:eco:6.53.0")
    compileOnly("de.jexcellence.currency:jecurrency:2.0.0")

    compileOnly("me.devnatan:inventory-framework-platform-bukkit:3.5.1")
    compileOnly("me.devnatan:inventory-framework-platform-paper:3.5.1")
    compileOnly("me.devnatan:inventory-framework-anvil-input:3.5.1")
    compileOnly("com.github.ben-manes.caffeine:caffeine:3.2.2")
    compileOnly("com.fasterxml.jackson.core:jackson-core:2.20.0")
    compileOnly("com.fasterxml.jackson.core:jackson-databind:2.20.0")
    compileOnly("com.fasterxml.jackson.core:jackson-annotations:3.0-rc5")
    compileOnly("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.20.0")
    compileOnly("com.fasterxml.uuid:java-uuid-generator:5.1.0")
    compileOnly("com.github.cryptomorin:XSeries:13.3.3")
    compileOnly("org.jboss.logging:jboss-logging:3.5.0.Final")
    compileOnly("org.slf4j:slf4j-api:2.0.17")
    compileOnly("org.slf4j:slf4j-jdk14:2.0.17")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
    withJavadocJar()
}

sourceSets {
    val main by getting
    create("free") {
        java.srcDir("src/free/java")
        resources.srcDir("src/free/resources")
        compileClasspath += main.compileClasspath + main.output
        runtimeClasspath += main.runtimeClasspath + main.output
    }
    create("premium") {
        java.srcDir("src/premium/java")
        resources.srcDir("src/premium/resources")
        compileClasspath += main.compileClasspath + main.output
        runtimeClasspath += main.runtimeClasspath + main.output
    }
}

configurations {
    named("freeImplementation") { extendsFrom(configurations["implementation"]) }
    named("premiumImplementation") { extendsFrom(configurations["implementation"]) }
    named("freeRuntimeOnly") { extendsFrom(configurations["runtimeOnly"]) }
    named("premiumRuntimeOnly") { extendsFrom(configurations["runtimeOnly"]) }
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
        options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all", "-Xlint:-processing"))
    }

    jar {
        archiveBaseName.set("raindropquests-core")
        manifest {
            attributes(
                "Implementation-Title" to "RaindropQuests",
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "Raindrop Central"
            )
        }
    }

    javadoc {
        options.encoding = "UTF-8"
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }

    val shadowFree by registering(ShadowJar::class) {
        group = "build"
        description = "Builds the Free version shadow JAR"
        archiveFileName.set("RDQ-Free-${project.version}.jar")
        archiveClassifier.set("free")
        from(sourceSets["main"].output, sourceSets["free"].output)
        configurations = listOf(project.configurations.getByName("freeRuntimeClasspath"))
        mergeServiceFiles()
        manifest {
            attributes(
                "Implementation-Title" to "RaindropQuests Free",
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "Raindrop Central"
            )
        }
    }

    val shadowPremium by registering(ShadowJar::class) {
        group = "build"
        description = "Builds the Premium version shadow JAR"
        archiveFileName.set("RDQ-Premium-${project.version}.jar")
        archiveClassifier.set("premium")
        from(sourceSets["main"].output, sourceSets["premium"].output)
        configurations = listOf(project.configurations.getByName("premiumRuntimeClasspath"))
        mergeServiceFiles()
        manifest {
            attributes(
                "Implementation-Title" to "RaindropQuests Premium",
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "Raindrop Central"
            )
        }

        relocate("com.tcoded.folialib", "de.jexcellence.remapped.com.tcoded.folialib")
        relocate("me.devnatan.inventoryframework", "de.jexcellence.remapped.me.devnatan.inventoryframework")
        relocate("org.h2", "de.jexcellence.remapped.org.h2")
        relocate("com.mysql.cj", "de.jexcellence.remapped.com.mysql.cj")
        relocate("com.fasterxml.jackson.core", "de.jexcellence.remapped.com.fasterxml.jackson.core")
        relocate("com.fasterxml.jackson.datatype", "de.jexcellence.remapped.com.fasterxml.jackson.datatype")
        relocate("com.github.benmanes.caffeine", "de.jexcellence.remapped.com.github.benmanes.caffeine")
    }

    build {
        dependsOn(shadowFree, shadowPremium)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "raindropquests"
            version = project.version.toString()

            pom {
                name.set("RaindropQuests")
                description.set(project.description)
                url.set("https://github.com/raindropcentral/raindrop-plugins")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
            }
        }
    }

    repositories {
        mavenLocal()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/raindropcentral/raindrop-plugins")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: findProperty("gpr.user") as String?
                password = System.getenv("GITHUB_TOKEN") ?: findProperty("gpr.key") as String?
            }
        }
    }
}

tasks.register("publishLocal") {
    group = "publishing"
    description = "Publishes to local Maven repository"
    dependsOn("publishToMavenLocal")
    doLast {
        println("✓ Published ${project.group}:raindropquests:${project.version} to local Maven")
    }
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds all versions (Free and Premium)"
    dependsOn("shadowFree", "shadowPremium")
    doLast {
        println("✓ Built Free: ${tasks.named<ShadowJar>("shadowFree").get().archiveFile.get().asFile}")
        println("✓ Built Premium: ${tasks.named<ShadowJar>("shadowPremium").get().archiveFile.get().asFile}")
    }
}
