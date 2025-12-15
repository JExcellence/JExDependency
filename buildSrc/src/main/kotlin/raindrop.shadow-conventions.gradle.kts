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
    relocate("tools.jackson.core", "de.jexcellence.remapped.tools.jackson.core")
    relocate("tools.jackson.databind", "de.jexcellence.remapped.tools.jackson.databind")
    relocate("com.fasterxml.jackson.annotation", "de.jexcellence.remapped.com.fasterxml.jackson.annotation")
    relocate("tools.jackson.core.datatype", "de.jexcellence.remapped.tools.jackson.core.datatype")
    relocate("com.github.benmanes", "de.jexcellence.remapped.com.github.benmanes")
    relocate("org.h2", "de.jexcellence.remapped.org.h2")
    relocate("me.devnatan.inventoryframework", "de.jexcellence.remapped.me.devnatan.inventoryframework")
    relocate("com.tcoded", "de.jexcellence.remapped.com.tcoded")
    relocate("com.cryptomorin.xseries", "de.jexcellence.remapped.com.cryptomorin.xseries")
}
