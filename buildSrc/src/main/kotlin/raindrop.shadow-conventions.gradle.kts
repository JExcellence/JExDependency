/**
 * Convention for modules that create shaded/fat JARs
 */
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("raindrop.library-conventions")
    id("com.gradleup.shadow")
}

/**
 * Common relocation prefix for all remapped dependencies.
 * This MUST match what JExDependency's RemappingDependencyManager uses.
 */
val RELOCATION_PREFIX = "de.jexcellence.remapped"

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    mergeServiceFiles()
    
    // ===========================================
    // Common relocations for JExcellence dependencies
    // These MUST match the auto-relocation in JExDependency
    // ===========================================
    
    // Jackson 3.x (tools.jackson namespace)
    relocate("tools.jackson", "$RELOCATION_PREFIX.tools.jackson")
    
    // NOTE: com.fasterxml.jackson.annotation is NOT relocated because:
    // 1. Hibernate expects original Jackson paths (com.fasterxml.jackson.core)
    // 2. Jackson 3.x only has annotations at com.fasterxml, core moved to tools.jackson
    // 3. Relocating com.fasterxml would break Hibernate's Jackson integration
    
    // Database
    //relocate("org.h2", "$RELOCATION_PREFIX.org.h2")
    
    // Utilities
    relocate("com.github.benmanes", "$RELOCATION_PREFIX.com.github.benmanes")
    relocate("me.devnatan.inventoryframework", "$RELOCATION_PREFIX.me.devnatan.inventoryframework")
    relocate("com.tcoded", "$RELOCATION_PREFIX.com.tcoded")
    relocate("com.cryptomorin.xseries", "$RELOCATION_PREFIX.com.cryptomorin.xseries")
}
