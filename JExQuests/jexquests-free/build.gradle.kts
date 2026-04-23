import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("raindrop.shadow-conventions")
    id("raindrop.dependencies-yml")
}

group = "de.jexcellence.quests"
version = "1.0.0"

dependenciesYml {
    usePaperDependencies()
    generatePaperVariant.set(true)
    generateSpigotVariant.set(true)
}

dependencies {
    implementation(project(":JExQuests:jexquests-common"))
    implementation(project(":JExQuests:jexquests-api"))

    compileOnly(libs.paper.api)
    compileOnly(libs.slf4j.api)
    compileOnly(libs.slf4j.jdk14)
    compileOnly(libs.jboss.logging)

    compileOnly(platform(libs.hibernate.platform))
    compileOnly(libs.bundles.hibernate)
    compileOnly(libs.adventure.platform.bukkit)
    compileOnly(libs.jexplatform)

    implementation(libs.jehibernate) { isTransitive = false }
    implementation(libs.bundles.jexcellence) {
        isTransitive = false
        exclude(group = "de.jexcellence.hibernate")
    }
    implementation(libs.bundles.jeconfig) { isTransitive = false }
    compileOnly(libs.bundles.inventory)
    compileOnly(libs.vault.api) { isTransitive = false }
    compileOnly(libs.placeholderapi)
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("JExQuests")
    archiveClassifier.set("Free")
    archiveVersion.set(project.version.toString())

    relocate("tools.jackson", "de.jexcellence.remapped.tools.jackson")
    relocate("me.devnatan.inventoryframework", "de.jexcellence.remapped.me.devnatan.inventoryframework")
    relocate("com.tcoded", "de.jexcellence.remapped.com.tcoded")
    relocate("com.cryptomorin.xseries", "de.jexcellence.remapped.com.cryptomorin.xseries")

    configurations = listOf(project.configurations.getByName("runtimeClasspath"))
    mergeServiceFiles()
}

tasks.named("build") { dependsOn(tasks.named("shadowJar")) }
tasks.named<Test>("test") { useJUnitPlatform() }
tasks.named<Jar>("jar") { enabled = false }

afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("maven") {
                groupId = "de.jexcellence.quests"
                artifactId = "jexquests-free-shadow"
                version = project.version.toString()
                artifact(tasks.named("shadowJar"))
            }
            create<MavenPublication>("mavenShadow") {
                from(components["shadow"])
                groupId = "de.jexcellence.quests"
                artifactId = "jexquests-free"
                version = project.version.toString()
                pom {
                    name.set("JExQuests Free")
                    description.set("JExQuests Free Edition")
                }
            }
        }
    }
}
