plugins {
    id("raindrop.shadow-conventions")
    id("raindrop.dependencies-yml")
}

dependenciesYml {
    usePaperDependencies()
    generatePaperVariant.set(true)
    generateSpigotVariant.set(true)
}

group = "com.raindropcentral.rds"
version = "1.0.0"
description = "RDS Premium - Premium edition of Raindrop Shops"

dependencies {
    implementation(project(":RDS:rds-common"))
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
        "name" to "RDSPremium",
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
    archiveBaseName.set("RDS")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("Premium")

    relocate("tools.jackson", "com.raindropcentral.remapped.tools.jackson")
    relocate("com.github.benmanes", "com.raindropcentral.remapped.com.github.benmanes")
    relocate("me.devnatan.inventoryframework", "com.raindropcentral.remapped.me.devnatan.inventoryframework")
    relocate("com.tcoded", "com.raindropcentral.remapped.com.tcoded")
    relocate("com.cryptomorin.xseries", "com.raindropcentral.remapped.com.cryptomorin.xseries")

    configurations = listOf(project.configurations.getByName("runtimeClasspath"))
    mergeServiceFiles()

    from(project(":RDS:rds-common").sourceSets.main.get().resources) {
        exclude("plugin.yml", "paper-plugin.yml")
    }
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
            artifactId = "rds-premium"
            version = project.version.toString()
            pom {
                name.set("RDS Premium")
                description.set(project.description)
            }
        }
    }
}
