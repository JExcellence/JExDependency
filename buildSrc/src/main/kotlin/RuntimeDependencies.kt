/**
 * Centralized runtime dependency definitions for JExDependency system.
 * 
 * These dependencies are downloaded at runtime by the plugin and injected into the classpath.
 * This single source of truth is used to generate dependencies.yml files during build.
 */
object RuntimeDependencies {
    
    // ===========================================
    // Version Constants
    // ===========================================
    object Versions {
        // Database
        const val HIBERNATE = "7.1.4.Final"
        const val HIBERNATE_COMMONS = "7.0.1.Final"
        const val H2 = "2.4.240"
        const val MYSQL = "9.2.0"
        
        // Jackson 2.x (compatible with server's bundled Jackson)
        const val JACKSON = "2.18.2"
        const val JACKSON_DATATYPE = "2.18.2"
        
        // Jakarta
        const val JAKARTA_PERSISTENCE = "3.2.0"
        const val JAKARTA_TRANSACTION = "2.0.1"
        const val JAKARTA_XML_BIND = "4.0.0"
        const val JAKARTA_ACTIVATION = "2.1.0"
        const val JAKARTA_INJECT = "2.0.1"
        
        // JAXB
        const val JAXB = "4.0.5"
        const val ISTACK = "4.1.1"
        const val ANGUS = "2.0.0"
        
        // Utilities
        const val JAVASSIST = "3.29.2-GA"
        const val JAVASSIST_REFLECTIONS = "3.30.2-GA"  // Newer version for Reflections
        const val BYTE_BUDDY = "1.17.6"
        const val JBOSS_LOGGING = "3.6.1.Final"
        const val CLASSMATE = "1.7.0"
        const val JANDEX = "3.3.1"
        const val ANTLR = "4.13.2"
        const val HIBERNATE_MODELS = "1.0.1"
        const val SNAKEYAML = "2.4"
        const val CAFFEINE = "3.2.2"
        const val REFLECTIONS = "0.10.2"
        const val DOM4J = "2.1.4"
        const val UUID_GENERATOR = "5.1.0"
        const val JETBRAINS_ANNOTATIONS = "23.0.0"
        
        // Logging
        const val SLF4J = "2.0.17"
        
        // Adventure
        const val ADVENTURE = "4.17.0"
        const val ADVENTURE_PLATFORM = "4.3.4"
        const val EXAMINATION = "1.3.0"
        const val OPTION = "1.1.0"
        
        // Minecraft Libraries
        const val XSERIES = "13.3.3"
        const val FOLIALIB = "0.5.1"
        const val INVENTORY_FRAMEWORK = "3.7.1"
        
        // Internal
        const val JEHIBERNATE = "3.0.3"
    }
    
    // ===========================================
    // Dependency Groups (with section names for YAML output)
    // ===========================================
    
    /** Represents a group of dependencies with a section name */
    data class DependencyGroup(val name: String, val dependencies: List<String>)
    
    /** Core Hibernate ORM dependencies (Hibernate 7.x — commons-annotations was discontinued) */
    val hibernate = DependencyGroup("Hibernate ORM", listOf(
        "org.hibernate.orm:hibernate-core:${Versions.HIBERNATE}",
        "org.hibernate.models:hibernate-models:${Versions.HIBERNATE_MODELS}",
        "org.javassist:javassist:${Versions.JAVASSIST}",
        "net.bytebuddy:byte-buddy:${Versions.BYTE_BUDDY}",
        "org.jboss.logging:jboss-logging:${Versions.JBOSS_LOGGING}",
        "com.fasterxml:classmate:${Versions.CLASSMATE}",
        "io.smallrye:jandex:${Versions.JANDEX}",
        "org.antlr:antlr4-runtime:${Versions.ANTLR}"
    ))
    
    /** Database drivers */
    val databases = DependencyGroup("Database Drivers", listOf(
        "com.h2database:h2:${Versions.H2}",
        "com.mysql:mysql-connector-j:${Versions.MYSQL}"
    ))
    
    /** Jakarta APIs */
    val jakarta = DependencyGroup("Jakarta APIs", listOf(
        "jakarta.persistence:jakarta.persistence-api:${Versions.JAKARTA_PERSISTENCE}",
        "jakarta.transaction:jakarta.transaction-api:${Versions.JAKARTA_TRANSACTION}",
        "jakarta.xml.bind:jakarta.xml.bind-api:${Versions.JAKARTA_XML_BIND}",
        "jakarta.activation:jakarta.activation-api:${Versions.JAKARTA_ACTIVATION}",
        "jakarta.inject:jakarta.inject-api:${Versions.JAKARTA_INJECT}"
    ))
    
    /** JAXB Runtime */
    val jaxb = DependencyGroup("JAXB Runtime", listOf(
        "org.glassfish.jaxb:jaxb-runtime:${Versions.JAXB}",
        "org.glassfish.jaxb:jaxb-core:${Versions.JAXB}",
        "org.glassfish.jaxb:txw2:${Versions.JAXB}",
        "com.sun.istack:istack-commons-runtime:${Versions.ISTACK}",
        "org.eclipse.angus:angus-activation:${Versions.ANGUS}"
    ))
    
    /** Jackson JSON serialization (2.x - compatible with server) */
    val jackson = DependencyGroup("Jackson JSON (2.x)", listOf(
        "com.fasterxml.jackson.core:jackson-core:${Versions.JACKSON}",
        "com.fasterxml.jackson.core:jackson-databind:${Versions.JACKSON}",
        "com.fasterxml.jackson.core:jackson-annotations:${Versions.JACKSON}",
        "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${Versions.JACKSON_DATATYPE}",
        "com.fasterxml.uuid:java-uuid-generator:${Versions.UUID_GENERATOR}"
    ))
    
    /** Jackson without JSR310 datatype (for plugins that don't need it) */
    val jacksonCore = DependencyGroup("Jackson JSON (2.x)", listOf(
        "com.fasterxml.jackson.core:jackson-core:${Versions.JACKSON}",
        "com.fasterxml.jackson.core:jackson-databind:${Versions.JACKSON}",
        "com.fasterxml.jackson.core:jackson-annotations:${Versions.JACKSON}",
        "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${Versions.JACKSON_DATATYPE}",
        "com.fasterxml.uuid:java-uuid-generator:${Versions.UUID_GENERATOR}"
    ))
    
    /** Logging */
    val logging = DependencyGroup("Logging", listOf(
        "org.slf4j:slf4j-api:${Versions.SLF4J}",
        "org.slf4j:slf4j-jdk14:${Versions.SLF4J}"
    ))
    
    /** Common utilities */
    val utilities = DependencyGroup("Utilities", listOf(
        "com.github.ben-manes.caffeine:caffeine:${Versions.CAFFEINE}",
        "org.jetbrains:annotations:${Versions.JETBRAINS_ANNOTATIONS}"
    ))
    
    /** Adventure API */
    val adventure = DependencyGroup("Adventure API", listOf(
        "net.kyori:adventure-api:${Versions.ADVENTURE}",
        "net.kyori:adventure-key:${Versions.ADVENTURE}",
        "net.kyori:adventure-nbt:${Versions.ADVENTURE}",
        "net.kyori:adventure-text-minimessage:${Versions.ADVENTURE}",
        "net.kyori:adventure-text-serializer-json:${Versions.ADVENTURE}",
        "net.kyori:adventure-text-serializer-legacy:${Versions.ADVENTURE}",
        "net.kyori:adventure-text-serializer-plain:${Versions.ADVENTURE}",
        "net.kyori:adventure-text-serializer-gson:${Versions.ADVENTURE}",
        "net.kyori:adventure-text-serializer-gson-legacy-impl:${Versions.ADVENTURE}",
        "net.kyori:adventure-text-serializer-json-legacy-impl:${Versions.ADVENTURE}",
        "net.kyori:adventure-platform-bukkit:${Versions.ADVENTURE_PLATFORM}",
        "net.kyori:adventure-platform-api:${Versions.ADVENTURE_PLATFORM}",
        "net.kyori:adventure-platform-facet:${Versions.ADVENTURE_PLATFORM}",
        "net.kyori:examination-api:${Versions.EXAMINATION}",
        "net.kyori:examination-string:${Versions.EXAMINATION}",
        "net.kyori:option:${Versions.OPTION}"
    ))
    
    /** Minecraft plugin utilities */
    val minecraftUtils = DependencyGroup("Minecraft Utilities", listOf(
        "com.github.cryptomorin:XSeries:${Versions.XSERIES}",
        "com.tcoded:FoliaLib:${Versions.FOLIALIB}"
    ))
    
    /** Inventory Framework (Paper) */
    val inventoryFramework = DependencyGroup("Inventory Framework", listOf(
        "me.devnatan:inventory-framework-api:${Versions.INVENTORY_FRAMEWORK}",
        "me.devnatan:inventory-framework-core:${Versions.INVENTORY_FRAMEWORK}",
        "me.devnatan:inventory-framework-platform:${Versions.INVENTORY_FRAMEWORK}",
        "me.devnatan:inventory-framework-platform-bukkit:${Versions.INVENTORY_FRAMEWORK}",
        "me.devnatan:inventory-framework-platform-paper:${Versions.INVENTORY_FRAMEWORK}",
        "me.devnatan:inventory-framework-anvil-input:${Versions.INVENTORY_FRAMEWORK}"
    ))
    
    /** Inventory Framework for Spigot (no paper-specific) */
    val inventoryFrameworkSpigot = DependencyGroup("Inventory Framework", listOf(
        "me.devnatan:inventory-framework-api:${Versions.INVENTORY_FRAMEWORK}",
        "me.devnatan:inventory-framework-core:${Versions.INVENTORY_FRAMEWORK}",
        "me.devnatan:inventory-framework-platform:${Versions.INVENTORY_FRAMEWORK}",
        "me.devnatan:inventory-framework-platform-bukkit:${Versions.INVENTORY_FRAMEWORK}",
        "me.devnatan:inventory-framework-anvil-input:${Versions.INVENTORY_FRAMEWORK}"
    ))
    
    /** Internal JExcellence libraries */
    val jehibernate = DependencyGroup("JExcellence Internal", listOf(
        "de.jexcellence.hibernate:JEHibernate:${Versions.JEHIBERNATE}"
    ))
    
    /** Extra utilities for some plugins */
    val extras = DependencyGroup("Extra Utilities", listOf(
        "org.yaml:snakeyaml:${Versions.SNAKEYAML}",
        // reflections is required by the bundled JEHibernate thin JAR for entity class scanning.
        // It is injected by JEDependency into the dependency classloader whose context is
        // propagated to async threads in JExEconomy.onEnable() before JEHibernate initialises.
        "org.reflections:reflections:${Versions.REFLECTIONS}",
        "org.javassist:javassist:${Versions.JAVASSIST_REFLECTIONS}",
        "org.dom4j:dom4j:${Versions.DOM4J}"
    ))
    
    // ===========================================
    // Pre-composed Dependency Sets
    // ===========================================
    
    /** Full dependency set for RCore (Paper) */
    val rcoreFull: List<DependencyGroup>
        get() = listOf(hibernate, databases, jakarta, jaxb, jackson, logging, 
                       utilities, adventure, minecraftUtils, inventoryFramework, jehibernate)
    
    /** Full dependency set for RDQ/JExEconomy (Paper)
     *  Note: JEHibernate itself is bundled in the plugin JAR (implementation dep) — not downloaded here.
     *  reflections is included in extras so the bundled JEHibernate thin JAR can scan entity classes. */
    val pluginFullPaper: List<DependencyGroup>
        get() = listOf(hibernate, databases, jakarta, jaxb, jacksonCore, logging,
                       utilities, adventure, minecraftUtils, inventoryFramework, extras)

    /** Full dependency set for RDQ/JExEconomy/RDT (Spigot)
     *  Note: JEHibernate itself is bundled in the plugin JAR (implementation dep) — not downloaded here. */
    val pluginFullSpigot: List<DependencyGroup>
        get() = listOf(hibernate, databases, jakarta, jaxb, jacksonCore, logging,
                       utilities, adventure, minecraftUtils, inventoryFrameworkSpigot, extras)
    
    /** JExDependency's own dependencies */
    val jexdependencyFull: List<DependencyGroup>
        get() = listOf(hibernate, databases, jakarta, jaxb, jackson, logging, 
                       utilities, adventure, minecraftUtils, inventoryFramework, jehibernate, extras)
}
