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
 * Enumeration defining the types of upgrades that can be applied to machines.
 *
 * <p>Upgrades enhance machine performance in specific ways. Each upgrade type
 * has multiple levels, with each level providing incremental improvements.
 * Upgrades are permanent once applied and persist across server restarts.
 *
 * <p>Available upgrade types:
 * <ul>
 *     <li>{@link #SPEED} - Reduces crafting cycle time</li>
 *     <li>{@link #EFFICIENCY} - Reduces fuel consumption</li>
 *     <li>{@link #BONUS_OUTPUT} - Chance for extra output items</li>
 *     <li>{@link #FUEL_REDUCTION} - Decreases fuel cost per operation</li>
 * </ul>
 *
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
public enum EUpgradeType {
    
    /**
     * Speed upgrade reduces the time between crafting cycles.
     *
     * <p>Effect: Each level reduces crafting cooldown by a configured percentage.
     * <ul>
     *     <li>Default: 10% reduction per level</li>
     *     <li>Max Level: 5 (50% faster at max)</li>
     *     <li>Example: 100 tick cooldown → 50 ticks at level 5</li>
     * </ul>
     *
     * <p>Typical requirements:
     * <ul>
     *     <li>Level 1: 32 Redstone + 5000 coins</li>
     *     <li>Level 2: 64 Redstone + 10000 coins</li>
     *     <li>Higher levels: Increasing costs</li>
     * </ul>
     */
    SPEED(
        "speed",
        "Speed",
        Material.REDSTONE,
        "§c",
        5,
        0.10
    ),
    
    /**
     * Efficiency upgrade provides a chance to avoid fuel consumption.
     *
     * <p>Effect: Each level adds a percentage chance to not consume fuel per operation.
     * <ul>
     *     <li>Default: 15% chance per level</li>
     *     <li>Max Level: 5 (75% chance at max)</li>
     *     <li>Example: At level 3, 45% chance to craft without fuel cost</li>
     * </ul>
     *
     * <p>Typical requirements:
     * <ul>
     *     <li>Level 1: 8 Diamonds + 5000 coins</li>
     *     <li>Level 2: 16 Diamonds + 10000 coins</li>
     *     <li>Higher levels: Increasing costs</li>
     * </ul>
     */
    EFFICIENCY(
        "efficiency",
        "Efficiency",
        Material.DIAMOND,
        "§b",
        5,
        0.15
    ),
    
    /**
     * Bonus Output upgrade provides a chance to produce extra items.
     *
     * <p>Effect: Each level adds a percentage chance to double the output.
     * <ul>
     *     <li>Default: 10% chance per level</li>
     *     <li>Max Level: 3 (30% chance at max)</li>
     *     <li>Example: At level 2, 20% chance to get 2x output</li>
     * </ul>
     *
     * <p>Typical requirements:
     * <ul>
     *     <li>Level 1: 16 Emeralds + 10000 coins</li>
     *     <li>Level 2: 32 Emeralds + 20000 coins</li>
     *     <li>Level 3: 64 Emeralds + 40000 coins</li>
     * </ul>
     */
    BONUS_OUTPUT(
        "bonus_output",
        "Bonus Output",
        Material.EMERALD,
        "§a",
        3,
        0.10
    ),
    
    /**
     * Fuel Reduction upgrade decreases the fuel cost per operation.
     *
     * <p>Effect: Each level reduces fuel consumption by a configured percentage.
     * <ul>
     *     <li>Default: 10% reduction per level</li>
     *     <li>Max Level: 5 (50% less fuel at max)</li>
     *     <li>Example: 10 fuel per craft → 5 fuel at level 5</li>
     * </ul>
     *
     * <p>Note: This stacks multiplicatively with Efficiency upgrade.
     * At max Fuel Reduction (50% cost) and max Efficiency (75% chance to skip),
     * effective fuel usage is dramatically reduced.
     *
     * <p>Typical requirements:
     * <ul>
     *     <li>Level 1: 16 Gold Ingots + 5000 coins</li>
     *     <li>Level 2: 32 Gold Ingots + 10000 coins</li>
     *     <li>Higher levels: Increasing costs</li>
     * </ul>
     */
    FUEL_REDUCTION(
        "fuel_reduction",
        "Fuel Reduction",
        Material.GOLD_INGOT,
        "§6",
        5,
        0.10
    );
    
    private final String identifier;
    private final String displayName;
    private final Material iconMaterial;
    private final String colorCode;
    private final int maxLevel;
    private final double effectPerLevel;
    
    /**
     * Constructs an upgrade type with specified properties.
     *
     * @param identifier      unique string identifier for the upgrade type
     * @param displayName     human-readable display name
     * @param iconMaterial    material to use as icon indicator
     * @param colorCode       color code for text formatting
     * @param maxLevel        maximum level this upgrade can reach
     * @param effectPerLevel  effect multiplier per level (as decimal, e.g., 0.10 = 10%)
     */
    EUpgradeType(
        final @NotNull String identifier,
        final @NotNull String displayName,
        final @NotNull Material iconMaterial,
        final @NotNull String colorCode,
        final int maxLevel,
        final double effectPerLevel
    ) {
        this.identifier = identifier;
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
        this.colorCode = colorCode;
        this.maxLevel = maxLevel;
        this.effectPerLevel = effectPerLevel;
    }
    
    /**
     * Gets the unique identifier for this upgrade type.
     *
     * @return the upgrade type identifier
     */
    public @NotNull String getIdentifier() {
        return this.identifier;
    }
    
    /**
     * Gets the display name for this upgrade type.
     *
     * @return the human-readable upgrade type name
     */
    public @NotNull String getDisplayName() {
        return this.displayName;
    }
    
    /**
     * Gets the material to use as an icon for this upgrade type.
     *
     * @return the icon material
     */
    public @NotNull Material getIconMaterial() {
        return this.iconMaterial;
    }
    
    /**
     * Gets the color code for this upgrade type.
     *
     * @return the color code string (e.g., "§c")
     */
    public @NotNull String getColorCode() {
        return this.colorCode;
    }
    
    /**
     * Gets the maximum level this upgrade can reach.
     *
     * @return the maximum upgrade level
     */
    public int getMaxLevel() {
        return this.maxLevel;
    }
    
    /**
     * Gets the effect multiplier per level.
     *
     * @return the effect per level as a decimal (e.g., 0.10 for 10%)
     */
    public double getEffectPerLevel() {
        return this.effectPerLevel;
    }
    
    /**
     * Calculates the total effect for a given level.
     *
     * @param level the upgrade level
     * @return the total effect multiplier
     */
    public double calculateEffect(final int level) {
        return this.effectPerLevel * level;
    }
    
    /**
     * Checks if a level is valid for this upgrade type.
     *
     * @param level the level to check
     * @return true if the level is between 1 and maxLevel, false otherwise
     */
    public boolean isValidLevel(final int level) {
        return level >= 1 && level <= this.maxLevel;
    }
    
    /**
     * Gets the localization key for this upgrade type's display name.
     *
     * @return the i18n key for the upgrade type name
     */
    public @NotNull String getDisplayNameKey() {
        return "machine.upgrade." + this.identifier + ".name";
    }
    
    /**
     * Gets the localization key for this upgrade type's description.
     *
     * @return the i18n key for the upgrade type description
     */
    public @NotNull String getDescriptionKey() {
        return "machine.upgrade." + this.identifier + ".description";
    }
    
    /**
     * Gets a formatted display name with color code.
     *
     * @return the colored display name
     */
    public @NotNull String getColoredDisplayName() {
        return this.colorCode + this.displayName;
    }
    
    /**
     * Finds an upgrade type by its identifier.
     *
     * @param identifier the identifier to search for
     * @return the matching upgrade type, or null if not found
     */
    public static EUpgradeType fromIdentifier(final @NotNull String identifier) {
        for (final EUpgradeType type : values()) {
            if (type.getIdentifier().equalsIgnoreCase(identifier)) {
                return type;
            }
        }
        return null;
    }
}
