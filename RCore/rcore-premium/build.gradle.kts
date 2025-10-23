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

    // Needed because RCorePremium extends JavaPlugin
    compileOnly(libs.paper.api)

    // Logging & utils
    compileOnly(libs.slf4j.api)
    compileOnly(libs.slf4j.jdk14)
    compileOnly(libs.jboss.logging)

    // DB & platform (compileOnly)
    compileOnly(platform(libs.hibernate.platform))
    compileOnly(libs.bundles.hibernate)

    // Needed because RCorePremium references JEDependency directly
    implementation(libs.bundles.jexcellence) { isTransitive = false }
    implementation(libs.bundles.jeconfig) { isTransitive = false }
    implementation(libs.bundles.inventory) { isTransitive = false }

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.mockito.inline)
    testImplementation(libs.mockbukkit)
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<ShadowJar>("shadowJar") {
    // Name: RCore-premium-<version>.jar
    archiveBaseName.set("RCore")
    archiveClassifier.set("premium")
    archiveVersion.set(project.version.toString())

    configurations = listOf(project.configurations.getByName("runtimeClasspath"))
    mergeServiceFiles()
}

// Always build the shaded jar when running assemble/build
tasks.named("assemble") {
    dependsOn(tasks.named("shadowJar"))
}
tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}

tasks.test {
    useJUnitPlatform()
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
            artifactId = "rcore-premium"
            version = project.version.toString()
            pom {
                name.set("RCore Premium")
                description.set("${project.description} (Premium)")
            }
        }
    }
}