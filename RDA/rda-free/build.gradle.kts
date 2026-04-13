plugins {
    id("raindrop.shadow-conventions")
    id("raindrop.dependencies-yml")
}

dependenciesYml {
    usePaperDependencies()
    generatePaperVariant.set(true)
    generateSpigotVariant.set(true)
}

val versionMajor: String by project.rootProject.extra { findProperty("rda.version.major")?.toString() ?: "undefined" }
val versionMinor: String by project.rootProject.extra { findProperty("rda.version.minor")?.toString() ?: "undefined" }
val versionPatch: String by project.rootProject.extra { findProperty("rda.version.patch")?.toString() ?: "undefined" }
val versionStage: String by project.rootProject.extra { findProperty("rda.version.stage")?.toString() ?: "undefined" }
val versionBuild: String by project.rootProject.extra { findProperty("rda.version.build")?.toString() ?: "undefined" }

val rdaVersion = "$versionMajor.$versionMinor.$versionPatch-$versionStage-Build-$versionBuild"

group = "com.raindropcentral.rda"
version = rdaVersion
description = "RDA Free - Free edition of Raindrop Abilities"

dependencies {
    implementation(project(":RDA:rda-common"))
    implementation(project(":JExCommand"))
    implementation(project(":JExDependency"))
    implementation(project(":RPlatform"))

    compileOnly(libs.paper.api)
    compileOnly(libs.bundles.inventory)
    implementation(platform(libs.hibernate.platform))
    implementation(libs.bundles.hibernate)
    implementation(libs.jehibernate)
    implementation(libs.bundles.jeconfig) { isTransitive = false }

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.paper.api)
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version,
        "name" to "RDAFree",
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
    archiveBaseName.set("RDA")
    archiveVersion.set(rdaVersion)
    archiveClassifier.set("Free")

    relocate("me.devnatan.inventoryframework", "de.jexcellence.remapped.me.devnatan.inventoryframework")

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
            artifactId = "rda-free"
            version = project.version.toString()
            pom {
                name.set("RDA Free")
                description.set(project.description)
            }
        }
    }
}
