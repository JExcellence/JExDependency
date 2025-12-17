import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.Test

plugins {
    id("raindrop.shadow-conventions")
    id("raindrop.dependencies-yml")
}

version = "2.0.0"

dependenciesYml {
    usePaperDependencies()
    generatePaperVariant.set(true)
    generateSpigotVariant.set(true)
}

dependencies {
    implementation(project(":JExEconomy:jexeconomy-common"))

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
    }
    implementation(libs.bundles.jeconfig) { isTransitive = false }
    compileOnly(libs.bundles.inventory)
    compileOnly(libs.vault.api) { isTransitive = false }
    compileOnly(libs.placeholderapi)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.mockbukkit)
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("JExEconomy")
    archiveClassifier.set("Premium")
    archiveVersion.set(project.version.toString())

    // Jackson 3.x core (tools.jackson namespace)
    relocate("tools.jackson", "de.jexcellence.remapped.tools.jackson")
    // NOTE: com.fasterxml is NOT relocated - Hibernate expects original Jackson paths

    relocate("com.github.benmanes", "de.jexcellence.remapped.com.github.benmanes")
    relocate("me.devnatan.inventoryframework", "de.jexcellence.remapped.me.devnatan.inventoryframework")
    relocate("com.tcoded", "de.jexcellence.remapped.com.tcoded")
    relocate("com.cryptomorin.xseries", "de.jexcellence.remapped.com.cryptomorin.xseries")

    configurations = listOf(project.configurations.getByName("runtimeClasspath"))
    mergeServiceFiles()
}


tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

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
            artifactId = "jexceconomy-premium"
            version = project.version.toString()
            pom {
                name.set("JExEconomy Premium")
                description.set("${project.description} (Premium)")
            }
        }
    }
}