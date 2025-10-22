import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar

plugins {
    id("java-library")
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.6" apply false
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all", "-Xlint:-processing"))
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "Raindrop Central",
        )
    }
}

tasks.processResources {
    exclude("plugin.yml", "paper-plugin.yml")
}

dependencies {
    // Server API
    compileOnly(libs.paper.api)

    // Adventure APIs
    compileOnly(libs.bundles.adventure)

    // Ecosystem (provided by other plugins)
    compileOnly(libs.folialib)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.vault.api) { isTransitive = false}
    compileOnly(libs.luckperms.api)

    // Logging & utils
    compileOnly(libs.slf4j.api)
    compileOnly(libs.slf4j.jdk14)
    compileOnly(libs.jboss.logging)

    // DB & platform (compileOnly)
    compileOnly(platform(libs.hibernate.platform))
    compileOnly(libs.bundles.hibernate)

    // Misc (compileOnly)
    compileOnly(libs.caffeine)
    compileOnly(libs.jackson.core)
    compileOnly(libs.jackson.databind)
    compileOnly(libs.jackson.annotations)
    compileOnly(libs.jackson.jsr310)
    compileOnly(libs.java.uuid)
    compileOnly(libs.xseries)

    // Internal libraries to be shaded by variants
    implementation(libs.bundles.jexcellence)
    implementation(libs.jehibernate)
    implementation(libs.bundles.jeconfig) { isTransitive = false }
    implementation(libs.bundles.inventory)

    // Example of plugin-provided API used by listeners
    compileOnly(libs.jecurrency)
    compileOnly("com.raindropcentral.rcore:rcore:2.0.0")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
    testImplementation(libs.mockito.inline)
    testImplementation("com.github.seeseemelk:MockBukkit-v1.21:3.133.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "rdq-common"

            pom {
                name.set("RDQ Common")
                description.set("Shared code for RDQ Free/Premium")
                url.set("https://github.com/raindropcentral/raindrop-plugins")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
            }
        }
    }
}