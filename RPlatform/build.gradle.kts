plugins {
    java
    `maven-publish`
}

group = "com.raindropcentral.platform"
version = "2.0.0"
description = "Modern platform abstraction layer for Spigot/Paper/Folia plugins"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://nexus.neetgames.com/repository/maven-releases/")
    maven("https://repo.tcoded.com/releases")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")
    compileOnly("net.kyori:adventure-platform-bukkit:4.3.4")
    
    implementation("de.jexcellence.translate:jextranslate:3.0.0")
    implementation("de.jexcellence.config:Evaluable:1.0.0")
    implementation("de.jexcellence.config:GPEEE:1.0.0")
    implementation("de.jexcellence.config:ConfigMapper:1.0.0")
    implementation("de.jexcellence.hibernate:JEHibernate:1.0.0")
    
    compileOnly(platform("org.hibernate.orm:hibernate-platform:6.6.4.Final"))
    compileOnly("org.hibernate.orm:hibernate-core")
    compileOnly("jakarta.transaction:jakarta.transaction-api")
    compileOnly("com.mysql:mysql-connector-j:9.2.0")
    compileOnly("com.h2database:h2:2.4.240")
    
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("net.luckperms:api:5.5")
    compileOnly("com.tcoded:FoliaLib:0.5.1")

    compileOnly("me.devnatan:inventory-framework-platform-bukkit:3.5.1")
    compileOnly("me.devnatan:inventory-framework-platform-paper:3.5.1")
    compileOnly("me.devnatan:inventory-framework-anvil-input:3.5.1")
    
    compileOnly("org.jetbrains:annotations:24.0.1")
    
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito:mockito-inline:5.5.0")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.20:3.6.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
        options.compilerArgs.addAll(listOf(
            "-parameters",
            "-Xlint:unchecked",
            "-Xlint:deprecation"
        ))
    }

    jar {
        archiveBaseName.set("RPlatform")
        archiveVersion.set(project.version.toString())
        
        manifest {
            attributes(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to "Raindrop Central",
                "Built-By" to System.getProperty("user.name"),
            )
        }
    }

    javadoc {
        options.encoding = "UTF-8"
        (options as StandardJavadocDocletOptions).apply {
            addStringOption("Xdoclint:none", "-quiet")
            links(
                "https://docs.oracle.com/en/java/javase/21/docs/api/",
                "https://jd.papermc.io/paper/1.21/"
            )
        }
    }

    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    register("publishLocal") {
        group = "publishing"
        description = "Publishes to local Maven repository"
        dependsOn("publishToMavenLocal")
        
        doLast {
            println("✓ Published ${project.group}:${project.name}:${project.version} to local Maven")
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            
            groupId = project.group.toString()
            artifactId = "rplatform"
            version = project.version.toString()
            
            pom {
                name.set("RPlatform")
                description.set(project.description)
                url.set("https://github.com/raindropcentral/rplatform")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        id.set("jexcellence")
                        name.set("JExcellence")
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/raindropcentral/rplatform.git")
                    developerConnection.set("scm:git:ssh://github.com/raindropcentral/rplatform.git")
                    url.set("https://github.com/raindropcentral/rplatform")
                }
            }
        }
    }
    
    repositories {
        mavenLocal()
        
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/raindropcentral/rplatform")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: findProperty("gpr.user") as String?
                password = System.getenv("GITHUB_TOKEN") ?: findProperty("gpr.key") as String?
            }
        }
    }
}
