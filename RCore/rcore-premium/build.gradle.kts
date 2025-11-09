import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("raindrop.shadow-conventions")
}

dependencies {
    implementation(project(":RCore:rcore-common"))

    compileOnly(libs.paper.api)
    compileOnly(libs.slf4j.api)
    compileOnly(libs.slf4j.jdk14)
    compileOnly(libs.jboss.logging)

    compileOnly(platform(libs.hibernate.platform))
    compileOnly(libs.bundles.hibernate)

    implementation(libs.bundles.jexcellence) { isTransitive = false }
    implementation(libs.bundles.jeconfig) { isTransitive = false }
    implementation(libs.bundles.inventory) { isTransitive = false }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("RCore")
    archiveClassifier.set("premium")
    archiveVersion.set(project.version.toString())

    configurations = listOf(project.configurations.getByName("runtimeClasspath"))
    mergeServiceFiles()
}

tasks.named("assemble") {
    dependsOn(tasks.named("shadowJar"))
}
tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<Jar>("jar") {
    enabled = false
}

publishing {
    publications {
        create<MavenPublication>("mavenShadow") {
            artifact(tasks.named("shadowJar"))
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

// Disable the default 'maven' publication tasks since we only want mavenShadow
tasks.withType<PublishToMavenRepository>().configureEach {
    if (name.contains("MavenPublication")) {
        enabled = false
    }
}