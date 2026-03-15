plugins {
    id("raindrop.shadow-conventions")
    id("raindrop.dependencies-yml")
}

dependenciesYml {
    usePaperDependencies()
    generatePaperVariant.set(true)
    generateSpigotVariant.set(true)
}

val versionMajor: String by project.rootProject.extra { findProperty("rdr.version.major")?.toString() ?: "undefined" }
val versionMinor: String by project.rootProject.extra { findProperty("rdr.version.minor")?.toString() ?: "undefined" }
val versionPatch: String by project.rootProject.extra { findProperty("rdr.version.patch")?.toString() ?: "undefined" }
val versionStage: String by project.rootProject.extra { findProperty("rdr.version.stage")?.toString() ?: "undefined" }
val versionBuild: String by project.rootProject.extra { findProperty("rdr.version.build")?.toString() ?: "undefined" }

val rdrVersion = "$versionMajor.$versionMinor.$versionPatch-$versionStage-Build-$versionBuild"

group = "com.raindropcentral.rdr"
version = rdrVersion
description = "RDR Free - Free edition of Raindrop Distributed Resources"

dependencies {
    implementation(project(":RDR:rdr-common"))
    implementation(project(":JExCommand"))

    compileOnly(libs.paper.api)

    compileOnly(libs.slf4j.api)
    compileOnly(libs.slf4j.jdk14)
    compileOnly(libs.jboss.logging)

    compileOnly(platform(libs.hibernate.platform))
    compileOnly(libs.bundles.hibernate)
    compileOnly(libs.jehibernate)
    compileOnly(libs.adventure.platform.bukkit)
    compileOnly(libs.rplatform)

    implementation(libs.bundles.jexcellence) {
        isTransitive = false
        exclude(group = "de.jexcellence.hibernate")
        exclude(group = "com.raindropcentral.commands", module = "jexcommand")
    }
    implementation(libs.bundles.jeconfig) { isTransitive = false }
    compileOnly(libs.bundles.inventory)
    compileOnly(libs.vault.api) { isTransitive = false }
    compileOnly(libs.placeholderapi)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.paper.api)
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version,
        "name" to "RDRFree",
        "description" to project.description,
        "apiVersion" to "1.19"
    )
    inputs.properties(props)
    filesMatching(listOf("plugin.yml", "paper-plugin.yml")) {
        expand(props)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("RDR")
    archiveVersion.set(rdrVersion)
    archiveClassifier.set("Free")

    configurations = listOf(project.configurations.getByName("runtimeClasspath"))
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.named<Jar>("jar") {
    enabled = false
}

publishing {
    publications {
        create<MavenPublication>("shadow") {
            artifact(tasks.named("shadowJar"))
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))
            groupId = project.group.toString()
            artifactId = "rdr-free"
            version = project.version.toString()
            pom {
                name.set("RDR Free")
                description.set(project.description)
            }
        }
    }
}
