/**
 * Convention for modules that create shaded/fat JARs
 */
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("raindrop.library-conventions")
    id("com.gradleup.shadow")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    mergeServiceFiles()
    
    // Common relocations for JExcellence dependencies
    relocate("com.fasterxml.jackson.core", "de.jexcellence.remapped.com.fasterxml.jackson.core")
    relocate("com.fasterxml.jackson.databind", "de.jexcellence.remapped.com.fasterxml.jackson.databind")
    relocate("com.fasterxml.jackson.annotation", "de.jexcellence.remapped.com.fasterxml.jackson.annotation")
    relocate("com.fasterxml.jackson.datatype", "de.jexcellence.remapped.com.fasterxml.jackson.datatype")
    relocate("com.github.benmanes", "de.jexcellence.remapped.com.github.benmanes")
    relocate("org.h2", "de.jexcellence.remapped.org.h2")
    relocate("me.devnatan.inventoryframework", "de.jexcellence.remapped.me.devnatan.inventoryframework")
    relocate("com.tcoded", "de.jexcellence.remapped.com.tcoded")
    relocate("com.cryptomorin.xseries", "de.jexcellence.remapped.com.cryptomorin.xseries")
}
