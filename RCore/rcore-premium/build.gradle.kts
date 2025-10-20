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

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.21:3.133.2")
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