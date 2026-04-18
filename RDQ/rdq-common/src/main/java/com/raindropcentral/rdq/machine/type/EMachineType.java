/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdq.machine.type;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

/**
 * Enumeration defining the different types of machines available in the system.
 *
 * <p>Each machine type represents a distinct automated system with unique
 * functionality, structure requirements, and capabilities. Machine types are
 * permission-gated and can be unlocked through the perk system.
 *
 * <p>Currently supported machine types:
 * <ul>
 *     <li>{@link #FABRICATOR} - Automated crafting machine with 3x3 grid support</li>
 * </ul>
 *
 * <p>Future machine types may include:
 * <ul>
 *     <li>SMELTER - Automated smelting and ore processing</li>
 *     <li>CRUSHER - Resource crushing and grinding</li>
 *     <li>ASSEMBLER - Advanced crafting with larger grids</li>
 *     <li>GENERATOR - Power generation for machine networks</li>
 * </ul>
 *
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
public enum EMachineType {
    
    /**
     * Automated crafting machine supporting 3x3 crafting recipes.
     *
     * <p>The Fabricator automates standard Minecraft crafting by:
     * <ul>
     *     <li>Accepting recipe configuration through GUI</li>
     *     <li>Consuming fuel per crafting operation</li>
     *     <li>Drawing materials from virtual or physical storage</li>
     *     <li>Outputting crafted items to storage</li>
     *     <li>Supporting upgrades for speed, efficiency, and output</li>
     * </ul>
     *
     * <p>Multi-block structure:
     * <ul>
     *     <li>Core: DROPPER block</li>
     *     <li>Required: 3 HOPPER blocks (below, east, west)</li>
     *     <li>Required: 1 CHEST block (south for output)</li>
     * </ul>
     */
    FABRICATOR(
        "fabricator",
        "Fabricator",
        Material.DROPPER,
        "rdq.machine.fabricator",
        "machine.fabricator"
    );
    
    private final String identifier;
    private final String displayName;
    private final Material coreMaterial;
    private final String permission;
    private final String configKey;
    
    /**
     * Constructs a machine type with specified properties.
     *
     * @param identifier    unique string identifier for the machine type
     * @param displayName   human-readable display name
     * @param coreMaterial  material used as the core block in multi-block structure
     * @param permission    permission node required to create this machine type
     * @param configKey     configuration file key for this machine type
     */
    EMachineType(
        final @NotNull String identifier,
        final @NotNull String displayName,
        final @NotNull Material coreMaterial,
        final @NotNull String permission,
        final @NotNull String configKey
    ) {
        this.identifier = identifier;
        this.displayName = displayName;
        this.coreMaterial = coreMaterial;
        this.permission = permission;
        this.configKey = configKey;
    }
    
    /**
     * Gets the unique identifier for this machine type.
     *
     * @return the machine type identifier
     */
    public @NotNull String getIdentifier() {
        return this.identifier;
    }
    
    /**
     * Gets the display name for this machine type.
     *
     * @return the human-readable machine type name
     */
    public @NotNull String getDisplayName() {
        return this.displayName;
    }
    
    /**
     * Gets the core material used in this machine's multi-block structure.
     *
     * @return the core block material
     */
    public @NotNull Material getCoreMaterial() {
        return this.coreMaterial;
    }
    
    /**
     * Gets the permission node required to create this machine type.
     *
     * @return the permission string
     */
    public @NotNull String getPermission() {
        return this.permission;
    }
    
    /**
     * Gets the configuration key for this machine type.
     *
     * @return the config key used in YAML files
     */
    public @NotNull String getConfigKey() {
        return this.configKey;
    }
    
    /**
     * Gets the localization key for this machine type's display name.
     *
     * @return the i18n key for the machine type name
     */
    public @NotNull String getDisplayNameKey() {
        return "machine.type." + this.identifier + ".name";
    }
    
    /**
     * Gets the localization key for this machine type's description.
     *
     * @return the i18n key for the machine type description
     */
    public @NotNull String getDescriptionKey() {
        return "machine.type." + this.identifier + ".description";
    }
    
    /**
     * Finds a machine type by its identifier.
     *
     * @param identifier the identifier to search for
     * @return the matching machine type, or null if not found
     */
    public static EMachineType fromIdentifier(final @NotNull String identifier) {
        for (final EMachineType type : values()) {
            if (type.getIdentifier().equalsIgnoreCase(identifier)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * Finds a machine type by its core material.
     *
     * @param material the core material to search for
     * @return the matching machine type, or null if not found
     */
    public static EMachineType fromCoreMaterial(final @NotNull Material material) {
        for (final EMachineType type : values()) {
            if (type.getCoreMaterial() == material) {
                return type;
            }
        }
        return null;
    }
}
