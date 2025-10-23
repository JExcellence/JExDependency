import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    `maven-publish`
    alias(libs.plugins.shadow)
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

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(libs.mockbukkit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.mockito.inline)
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

tasks.test {
    useJUnitPlatform()
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