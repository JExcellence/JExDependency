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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Checks availability of specific statistics in the current Minecraft version.
 *
 * <p>This class handles version-specific statistic availability by attempting to access
 * statistics and gracefully handling {@link IllegalArgumentException} when they don't exist.
 * Results are cached to avoid repeated checks.
 *
 * <p>The checker logs warnings for unavailable statistics to help administrators understand
 * which statistics cannot be collected in their server version.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * StatisticAvailabilityChecker checker = new StatisticAvailabilityChecker(versionDetector);
 * 
 * if (checker.isStatisticAvailable(Statistic.MINE_BLOCK)) {
 *     // Collect MINE_BLOCK statistics
 * }
 * 
 * if (checker.isStatisticAvailable(Statistic.MINE_BLOCK, Material.STONE)) {
 *     // Collect MINE_BLOCK for STONE
 * }
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class StatisticAvailabilityChecker {

    private static final Logger LOGGER = Logger.getLogger(StatisticAvailabilityChecker.class.getName());
    
    private final MinecraftVersionDetector versionDetector;
    
    /**
     * Cache of statistic availability checks.
     * Key format: "STATISTIC_NAME" or "STATISTIC_NAME:MATERIAL" or "STATISTIC_NAME:ENTITY_TYPE"
     */
    private final ConcurrentHashMap<String, Boolean> availabilityCache;
    
    /**
     * Set of statistics that have been logged as unavailable to prevent duplicate warnings.
     */
    private final Set<String> loggedUnavailable;
    
    /**
     * Constructs a new availability checker.
     *
     * @param versionDetector the version detector to use for version-specific checks
     */
    public StatisticAvailabilityChecker(final @NotNull MinecraftVersionDetector versionDetector) {
        this.versionDetector = versionDetector;
        this.availabilityCache = new ConcurrentHashMap<>();
        this.loggedUnavailable = ConcurrentHashMap.newKeySet();
    }
    
    /**
     * Checks if a statistic is available in the current version.
     *
     * <p>This method checks if the statistic enum value exists and can be accessed
     * without throwing an exception.
     *
     * @param statistic the statistic to check
     * @return {@code true} if the statistic is available, {@code false} otherwise
     */
    public boolean isStatisticAvailable(final @NotNull Statistic statistic) {
        final String cacheKey = statistic.name();
        
        return availabilityCache.computeIfAbsent(cacheKey, key -> {
            try {
                // Attempt to access the statistic
                Statistic.valueOf(statistic.name());
                return true;
            } catch (final IllegalArgumentException e) {
                logUnavailable(statistic.name(), null, null);
                return false;
            }
        });
    }
    
    /**
     * Checks if a material-based statistic is available in the current version.
     *
     * <p>This method checks if the statistic can be used with the specified material
     * without throwing an exception.
     *
     * @param statistic the statistic to check
     * @param material the material to check with the statistic
     * @return {@code true} if the statistic is available for the material, {@code false} otherwise
     */
    public boolean isStatisticAvailable(
        final @NotNull Statistic statistic,
        final @NotNull Material material
    ) {
        final String cacheKey = statistic.name() + ":" + material.name();
        
        return availabilityCache.computeIfAbsent(cacheKey, key -> {
            try {
                // Check if statistic exists
                if (!isStatisticAvailable(statistic)) {
                    return false;
                }
                
                // Check if statistic type matches
                if (statistic.getType() != Statistic.Type.BLOCK && 
                    statistic.getType() != Statistic.Type.ITEM) {
                    logUnavailable(statistic.name(), material.name(), null);
                    return false;
                }
                
                // Material-based statistics are generally available if the statistic exists
                return true;
            } catch (final IllegalArgumentException e) {
                logUnavailable(statistic.name(), material.name(), null);
                return false;
            }
        });
    }
    
    /**
     * Checks if an entity-based statistic is available in the current version.
     *
     * <p>This method checks if the statistic can be used with the specified entity type
     * without throwing an exception.
     *
     * @param statistic the statistic to check
     * @param entityType the entity type to check with the statistic
     * @return {@code true} if the statistic is available for the entity type, {@code false} otherwise
     */
    public boolean isStatisticAvailable(
        final @NotNull Statistic statistic,
        final @NotNull EntityType entityType
    ) {
        final String cacheKey = statistic.name() + ":" + entityType.name();
        
        return availabilityCache.computeIfAbsent(cacheKey, key -> {
            try {
                // Check if statistic exists
                if (!isStatisticAvailable(statistic)) {
                    return false;
                }
                
                // Check if statistic type matches
                if (statistic.getType() != Statistic.Type.ENTITY) {
                    logUnavailable(statistic.name(), null, entityType.name());
                    return false;
                }
                
                // Entity-based statistics are generally available if the statistic exists
                return true;
            } catch (final IllegalArgumentException e) {
                logUnavailable(statistic.name(), null, entityType.name());
                return false;
            }
        });
    }
    
    /**
     * Clears the availability cache.
     *
     * <p>This method should be called if the server version changes or if statistics
     * need to be re-checked.
     */
    public void clearCache() {
        availabilityCache.clear();
        loggedUnavailable.clear();
        LOGGER.info("Cleared statistic availability cache");
    }
    
    /**
     * Gets the number of cached availability checks.
     *
     * @return the cache size
     */
    public int getCacheSize() {
        return availabilityCache.size();
    }
    
    /**
     * Logs a warning for an unavailable statistic.
     *
     * <p>Only logs once per statistic to avoid spam.
     *
     * @param statisticName the statistic name
     * @param materialName the material name, or null
     * @param entityTypeName the entity type name, or null
     */
    private void logUnavailable(
        final @NotNull String statisticName,
        final String materialName,
        final String entityTypeName
    ) {
        final String logKey = statisticName + 
            (materialName != null ? ":" + materialName : "") +
            (entityTypeName != null ? ":" + entityTypeName : "");
        
        if (loggedUnavailable.add(logKey)) {
            final StringBuilder message = new StringBuilder("Statistic not available: ")
                .append(statisticName);
            
            if (materialName != null) {
                message.append(" for material ").append(materialName);
            }
            if (entityTypeName != null) {
                message.append(" for entity ").append(entityTypeName);
            }
            
            message.append(" (Minecraft ")
                .append(versionDetector.getMajorVersion())
                .append(".")
                .append(versionDetector.getMinorVersion())
                .append(")");
            
            LOGGER.warning(message.toString());
        }
    }
}
