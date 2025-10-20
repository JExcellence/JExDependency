import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.6"
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    implementation(project(":rcore-common"))

    // Needed because RCoreFree extends JavaPlugin
    compileOnly(libs.paper.api)

    // Logging & utils
    compileOnly(libs.slf4j.api)
    compileOnly(libs.slf4j.jdk14)
    compileOnly(libs.jboss.logging)

    // DB & platform (compileOnly)
    compileOnly(platform(libs.hibernate.platform))
    compileOnly(libs.bundles.hibernate)

    // Needed because RCoreFree references JEDependency directly
    implementation(libs.bundles.jexcellence) { isTransitive = false }
    implementation(libs.bundles.jeconfig) { isTransitive = false }
    implementation(libs.bundles.inventory) { isTransitive = false }

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.21:3.133.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("RCore")
    archiveClassifier.set("free")
    archiveVersion.set(project.version.toString())

    relocate("com.github.benmanes", "de.jexcellence.remapped.com.github.benmanes")
    relocate("org.h2", "de.jexcellence.remapped.org.h2")

    configurations = listOf(project.configurations.getByName("runtimeClasspath"))
    mergeServiceFiles()
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}

// Optionally disable the plain jar to avoid confusion
tasks.named<Jar>("jar") {
    enabled = false
}

publishing {
    publications {
        create<MavenPublication>("mavenShadow") {
            from(components["shadow"])
            groupId = project.group.toString()
            artifactId = "rcore-free"
            version = project.version.toString()
            pom {
                name.set("RCore Free")
                description.set("${project.description} (Free)")
            }
        }
    }
}