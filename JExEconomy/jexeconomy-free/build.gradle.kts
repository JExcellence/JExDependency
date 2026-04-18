import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("raindrop.shadow-conventions")
    id("raindrop.dependencies-yml")
}

group = "de.jexcellence.economy"
version = "3.0.0"

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
    compileOnly(libs.adventure.platform.bukkit)
    compileOnly(libs.jexplatform)

    // JEHibernate thin JAR bundled so its classes ship in the plugin JAR.
    // Heavy deps (Hibernate 7.x, Jakarta, etc.) are downloaded by JEDependency's
    // PaperPluginLoader and injected via PluginClasspathBuilder — works correctly because
    // has-open-classloader is not set (defaults to false) in this module's paper-plugin.yml.
    implementation(libs.jehibernate) { isTransitive = false }
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
    archiveClassifier.set("Free")
    archiveVersion.set(project.version.toString())

    // Jackson 3.x core (tools.jackson namespace)
    relocate("tools.jackson", "de.jexcellence.remapped.tools.jackson")
    // NOTE: com.fasterxml is NOT relocated - Hibernate expects original Jackson paths

    // NOTE: com.github.benmanes (caffeine) is NOT relocated — JEHibernate (bundled thin JAR)
    // references caffeine under the original package; caffeine is injected at runtime by
    // JEDependency and must remain at its original package name.
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

afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("maven") {
                groupId = "de.jexcellence.economy"
                artifactId = "jexeconomy-free-shadow"
                version = project.version.toString()
                artifact(tasks.named("shadowJar"))
            }
            create<MavenPublication>("mavenShadow") {
                from(components["shadow"])
                groupId = "de.jexcellence.economy"
                artifactId = "jexeconomy-free"
                version = project.version.toString()
                pom {
                    name.set("JExEconomy Free")
                    description.set("JExEconomy Free Edition")
                }
            }
        }
    }
}
