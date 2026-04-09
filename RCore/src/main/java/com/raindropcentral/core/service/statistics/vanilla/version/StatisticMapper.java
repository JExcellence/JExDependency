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

package com.raindropcentral.core.service.statistics.vanilla.version;

import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Maps version-specific statistic names to consistent identifiers for backend storage.
 *
 * <p>This class handles renamed statistics across Minecraft versions and provides
 * consistent identifier mapping for all statistics. The mapping format follows the
 * pattern: {@code minecraft.<category>.<statistic>[.<subtype>]}
 *
 * <h2>Identifier Format Examples</h2>
 * <ul>
 *   <li>{@code minecraft.blocks.mined.stone} - MINE_BLOCK for STONE</li>
 *   <li>{@code minecraft.mobs.killed.zombie} - KILL_ENTITY for ZOMBIE</li>
 *   <li>{@code minecraft.travel.walk} - WALK_ONE_CM</li>
 *   <li>{@code minecraft.general.deaths} - DEATHS</li>
 *   <li>{@code minecraft.items.crafted.diamond_sword} - CRAFT_ITEM for DIAMOND_SWORD</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * StatisticMapper mapper = new StatisticMapper(versionDetector);
 * 
 * // Simple statistic
 * String id = mapper.mapStatistic(Statistic.DEATHS);
 * // Returns: "minecraft.general.deaths"
 * 
 * // Material-based statistic
 * String id = mapper.mapStatistic(Statistic.MINE_BLOCK, Material.STONE, null);
 * // Returns: "minecraft.blocks.mined.stone"
 * 
 * // Entity-based statistic
 * String id = mapper.mapStatistic(Statistic.KILL_ENTITY, null, EntityType.ZOMBIE);
 * // Returns: "minecraft.mobs.killed.zombie"
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class StatisticMapper {

    private static final Logger LOGGER = Logger.getLogger(StatisticMapper.class.getName());
    
    private final MinecraftVersionDetector versionDetector;
    
    /**
     * Map of renamed statistics across versions.
     * Key: old statistic name, Value: new statistic name
     */
    private final Map<String, String> renamedStatistics;
    
    /**
     * Constructs a new statistic mapper.
     *
     * @param versionDetector the version detector for version-specific mappings
     */
    public StatisticMapper(final @NotNull MinecraftVersionDetector versionDetector) {
        this.versionDetector = versionDetector;
        this.renamedStatistics = new HashMap<>();
        
        initializeRenamedStatistics();
    }
    
    /**
     * Maps a simple statistic to a consistent identifier.
     *
     * @param statistic the statistic to map
     * @return the consistent identifier (e.g., "minecraft.general.deaths")
     */
    public @NotNull String mapStatistic(final @NotNull Statistic statistic) {
        return mapStatistic(statistic, null, null);
    }
    
    /**
     * Maps a statistic with optional material or entity type to a consistent identifier.
     *
     * @param statistic the statistic to map
     * @param material the material, or null if not applicable
     * @param entityType the entity type, or null if not applicable
     * @return the consistent identifier
     */
    public @NotNull String mapStatistic(
        final @NotNull Statistic statistic,
        final @Nullable Material material,
        final @Nullable EntityType entityType
    ) {
        final String statisticName = getConsistentStatisticName(statistic);
        final String category = determineCategory(statistic);
        final String action = determineAction(statistic);
        
        final StringBuilder identifier = new StringBuilder("minecraft.")
            .append(category)
            .append(".")
            .append(action);
        
        // Add subtype if present
        if (material != null) {
            identifier.append(".").append(material.name().toLowerCase());
        } else if (entityType != null) {
            identifier.append(".").append(entityType.name().toLowerCase());
        }
        
        return identifier.toString();
    }
    
    /**
     * Gets the consistent statistic name, handling renamed statistics.
     *
     * @param statistic the statistic
     * @return the consistent statistic name
     */
    private @NotNull String getConsistentStatisticName(final @NotNull Statistic statistic) {
        final String name = statistic.name();
        return renamedStatistics.getOrDefault(name, name);
    }
    
    /**
     * Determines the category for a statistic.
     *
     * @param statistic the statistic
     * @return the category (blocks, items, mobs, travel, general, interactions)
     */
    private @NotNull String determineCategory(final @NotNull Statistic statistic) {
        return switch (statistic.getType()) {
            case BLOCK -> "blocks";
            case ITEM -> "items";
            case ENTITY -> "mobs";
            case UNTYPED -> {
                // Determine category based on statistic name
                final String name = statistic.name();
                if (name.contains("_CM") || name.contains("ONE_CM")) {
                    yield "travel";
                } else if (name.startsWith("INTERACT_WITH_")) {
                    yield "interactions";
                } else {
                    yield "general";
                }
            }
        };
    }
    
    /**
     * Determines the action for a statistic.
     *
     * @param statistic the statistic
     * @return the action (mined, placed, crafted, killed, etc.)
     */
    private @NotNull String determineAction(final @NotNull Statistic statistic) {
        final String name = statistic.name();
        
        // Handle specific statistic mappings
        return switch (name) {
            case "MINE_BLOCK" -> "mined";
            case "USE_ITEM" -> "used";
            case "BREAK_ITEM" -> "broken";
            case "CRAFT_ITEM" -> "crafted";
            case "PICKUP" -> "picked_up";
            case "DROP" -> "dropped";
            case "KILL_ENTITY" -> "killed";
            case "ENTITY_KILLED_BY" -> "killed_by";
            case "WALK_ONE_CM" -> "walk";
            case "SPRINT_ONE_CM" -> "sprint";
            case "CROUCH_ONE_CM" -> "crouch";
            case "SWIM_ONE_CM" -> "swim";
            case "FLY_ONE_CM" -> "fly";
            case "CLIMB_ONE_CM" -> "climb";
            case "FALL_ONE_CM" -> "fall";
            case "MINECART_ONE_CM" -> "minecart";
            case "BOAT_ONE_CM" -> "boat";
            case "PIG_ONE_CM" -> "pig";
            case "HORSE_ONE_CM" -> "horse";
            case "AVIATE_ONE_CM" -> "aviate";
            case "WALK_ON_WATER_ONE_CM" -> "walk_on_water";
            case "WALK_UNDER_WATER_ONE_CM" -> "walk_under_water";
            case "STRIDER_ONE_CM" -> "strider";
            default -> {
                // Handle INTERACT_WITH_ statistics
                if (name.startsWith("INTERACT_WITH_")) {
                    yield name.substring("INTERACT_WITH_".length()).toLowerCase();
                }
                // Default: use lowercase name
                yield name.toLowerCase();
            }
        };
    }
    
    /**
     * Initializes the map of renamed statistics across versions.
     *
     * <p>This method populates the {@link #renamedStatistics} map with known
     * statistic renames that occurred between Minecraft versions.
     */
    private void initializeRenamedStatistics() {
        // Example: PLAY_ONE_TICK was renamed to PLAY_ONE_MINUTE in 1.13+
        // Add more mappings as needed for version compatibility
        
        // Note: Most statistics have remained consistent since 1.13
        // This map is primarily for future-proofing
        
        LOGGER.fine("Initialized statistic rename mappings for version " + 
                   versionDetector.getMajorVersion() + "." + 
                   versionDetector.getMinorVersion());
    }
}
