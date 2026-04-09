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

package com.raindropcentral.core.service.statistics.vanilla.monitoring;

import org.bukkit.Statistic;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Provides performance logging for vanilla statistic collection operations.
 * <p>
 * This class logs warnings when collection operations exceed performance thresholds
 * and logs errors when statistic access fails. It helps identify performance issues
 * and track collection failures.
 */
public class PerformanceLogger {
    
    private static final long SLOW_COLLECTION_THRESHOLD_MS = 100;
    
    private final Logger logger;
    
    /**
     * Creates a new performance logger.
     *
     * @param logger the logger to use for output
     */
    public PerformanceLogger(Logger logger) {
        this.logger = logger;
    }
    
    /**
     * Logs collection performance if it exceeds the threshold.
     * <p>
     * Logs a warning if the collection duration exceeds 100ms.
     *
     * @param playerId the UUID of the player whose statistics were collected
     * @param durationMs the collection duration in milliseconds
     * @param statisticsCollected the number of statistics collected
     */
    public void logCollectionPerformance(UUID playerId, long durationMs, int statisticsCollected) {
        if (durationMs > SLOW_COLLECTION_THRESHOLD_MS) {
            logger.warning(String.format(
                "Slow vanilla statistic collection for player %s: %dms (%d statistics)",
                playerId,
                durationMs,
                statisticsCollected
            ));
        }
    }
    
    /**
     * Logs a statistic access failure.
     * <p>
     * This is called when accessing a player's statistic throws an exception,
     * typically because the statistic doesn't exist in the current Minecraft version.
     *
     * @param playerId the UUID of the player
     * @param statistic the statistic that failed to access
     * @param error the exception that was thrown
     */
    public void logStatisticAccessFailure(UUID playerId, Statistic statistic, Exception error) {
        logger.severe(String.format(
            "Failed to access statistic %s for player %s: %s",
            statistic.name(),
            playerId,
            error.getMessage()
        ));
    }
    
    /**
     * Logs a statistic access failure with material context.
     *
     * @param playerId the UUID of the player
     * @param statistic the statistic that failed to access
     * @param material the material associated with the statistic
     * @param error the exception that was thrown
     */
    public void logStatisticAccessFailure(UUID playerId, Statistic statistic, String material, Exception error) {
        logger.severe(String.format(
            "Failed to access statistic %s[%s] for player %s: %s",
            statistic.name(),
            material,
            playerId,
            error.getMessage()
        ));
    }
    
    /**
     * Logs a statistic access failure with entity type context.
     *
     * @param playerId the UUID of the player
     * @param statistic the statistic that failed to access
     * @param entityType the entity type associated with the statistic
     * @param error the exception that was thrown
     */
    public void logStatisticAccessFailureForEntity(UUID playerId, Statistic statistic, String entityType, Exception error) {
        logger.severe(String.format(
            "Failed to access statistic %s[%s] for player %s: %s",
            statistic.name(),
            entityType,
            playerId,
            error.getMessage()
        ));
    }
    
    /**
     * Logs a batch collection performance summary.
     *
     * @param playerCount the number of players in the batch
     * @param durationMs the total batch collection duration in milliseconds
     * @param totalStatistics the total number of statistics collected
     */
    public void logBatchCollectionPerformance(int playerCount, long durationMs, int totalStatistics) {
        if (durationMs > SLOW_COLLECTION_THRESHOLD_MS * playerCount) {
            logger.warning(String.format(
                "Slow batch collection: %d players in %dms (%d statistics, avg %.2fms per player)",
                playerCount,
                durationMs,
                totalStatistics,
                (double) durationMs / playerCount
            ));
        }
    }
    
    /**
     * Logs a cache persistence failure.
     *
     * @param error the exception that occurred during persistence
     */
    public void logCachePersistenceFailure(Exception error) {
        logger.severe(String.format(
            "Failed to persist vanilla statistics cache: %s",
            error.getMessage()
        ));
    }
    
    /**
     * Logs a cache load failure.
     *
     * @param error the exception that occurred during loading
     */
    public void logCacheLoadFailure(Exception error) {
        logger.severe(String.format(
            "Failed to load vanilla statistics cache: %s",
            error.getMessage()
        ));
    }
    
    /**
     * Logs successful cache persistence.
     *
     * @param playerCount the number of players whose data was persisted
     * @param durationMs the persistence duration in milliseconds
     */
    public void logCachePersistenceSuccess(int playerCount, long durationMs) {
        logger.fine(String.format(
            "Persisted vanilla statistics cache: %d players in %dms",
            playerCount,
            durationMs
        ));
    }
}
