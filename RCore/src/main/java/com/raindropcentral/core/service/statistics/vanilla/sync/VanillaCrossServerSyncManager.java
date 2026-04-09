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

package com.raindropcentral.core.service.statistics.vanilla.sync;

import com.raindropcentral.core.service.central.RCentralApiClient;
import com.raindropcentral.core.service.statistics.delivery.StatisticEntry;
import com.raindropcentral.core.service.statistics.vanilla.cache.StatisticCacheManager;
import com.raindropcentral.core.service.statistics.vanilla.config.VanillaStatisticConfig;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages cross-server synchronization for vanilla Minecraft statistics.
 * Handles fetching statistics from the backend when players join and merging
 * them with local values using the HIGHEST_WINS strategy.
 *
 * <p>Features:
 * <ul>
 *   <li>Automatic sync on player join</li>
 *   <li>HIGHEST_WINS merge strategy (vanilla stats are cumulative)</li>
 *   <li>Server-specific and global tracking</li>
 *   <li>Conflict logging for audit purposes</li>
 *   <li>5-minute sync caching to avoid redundant requests</li>
 * </ul>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class VanillaCrossServerSyncManager {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RCore");
    private static final long CACHE_VALIDITY_MS = 5 * 60 * 1000; // 5 minutes

    private final RCentralApiClient apiClient;
    private final String apiKey;
    private final String serverIdentifier;
    private final VanillaStatisticConfig config;
    private final StatisticCacheManager cacheManager;

    /**
     * Cache of synchronized statistics with timestamps.
     * Key: Player UUID, Value: Cached sync data
     */
    private final Map<UUID, CachedSyncData> syncCache;

    /**
     * Creates a new vanilla cross-server sync manager.
     *
     * @param apiClient        the API client for backend communication
     * @param apiKey           the API key for authentication
     * @param serverIdentifier the unique server identifier
     * @param config           the vanilla statistic configuration
     * @param cacheManager     the cache manager for local statistics
     */
    public VanillaCrossServerSyncManager(
        final @NotNull RCentralApiClient apiClient,
        final @NotNull String apiKey,
        final @NotNull String serverIdentifier,
        final @NotNull VanillaStatisticConfig config,
        final @NotNull StatisticCacheManager cacheManager
    ) {
        this.apiClient = apiClient;
        this.apiKey = apiKey;
        this.serverIdentifier = serverIdentifier;
        this.config = config;
        this.cacheManager = cacheManager;
        this.syncCache = new ConcurrentHashMap<>();
    }

    /**
     * Requests the latest vanilla statistics for a player from the backend.
     * Called when a player joins the server to sync cross-server data.
     *
     * <p>This method checks the sync cache first to avoid redundant backend requests.
     * If cached data is valid (less than 5 minutes old), no request is made.
     *
     * @param playerUuid the player UUID
     * @return a future that completes when the request is done
     */
    public CompletableFuture<Void> requestLatestStatistics(final @NotNull UUID playerUuid) {
        // Check if sync is enabled
        if (!config.isEnableCrossServerSync()) {
            LOGGER.fine("Cross-server sync disabled, skipping request for " + playerUuid);
            return CompletableFuture.completedFuture(null);
        }

        // Check cache validity
        CachedSyncData cached = syncCache.get(playerUuid);
        if (cached != null && !cached.isExpired()) {
            LOGGER.fine("Using cached sync data for " + playerUuid + 
                " (age: " + cached.getAgeSeconds() + "s)");
            return CompletableFuture.completedFuture(null);
        }

        LOGGER.fine("Requesting vanilla statistics for " + playerUuid + " from backend");

        return apiClient.requestPlayerStatistics(apiKey, playerUuid)
            .thenAccept(remoteStats -> {
                if (!remoteStats.isEmpty()) {
                    // Filter for vanilla statistics only
                    List<StatisticEntry> vanillaStats = remoteStats.stream()
                        .filter(stat -> stat.statisticKey().startsWith("minecraft."))
                        .toList();

                    if (!vanillaStats.isEmpty()) {
                        syncCache.put(playerUuid, new CachedSyncData(vanillaStats));
                        LOGGER.fine("Cached " + vanillaStats.size() + 
                            " vanilla statistics for " + playerUuid);
                    }
                } else {
                    LOGGER.fine("No remote statistics found for " + playerUuid);
                }
            })
            .exceptionally(error -> {
                LOGGER.warning("Failed to request statistics for " + playerUuid + 
                    ": " + error.getMessage());
                return null;
            });
    }

    /**
     * Gets cached synchronized statistics for a player.
     *
     * @param playerUuid the player UUID
     * @return the cached statistics or null if not cached or expired
     */
    public @Nullable List<StatisticEntry> getCachedStatistics(final @NotNull UUID playerUuid) {
        CachedSyncData cached = syncCache.get(playerUuid);
        if (cached != null && !cached.isExpired()) {
            return cached.statistics;
        }
        return null;
    }

    /**
     * Checks if cached sync data is valid for a player.
     *
     * @param playerUuid the player UUID
     * @return true if cache is valid and not expired
     */
    public boolean isCacheValid(final @NotNull UUID playerUuid) {
        CachedSyncData cached = syncCache.get(playerUuid);
        return cached != null && !cached.isExpired();
    }

    /**
     * Invalidates the sync cache for a player.
     * Called when a player disconnects or when fresh data is needed.
     *
     * @param playerUuid the player UUID
     */
    public void invalidateCache(final @NotNull UUID playerUuid) {
        syncCache.remove(playerUuid);
        LOGGER.fine("Invalidated sync cache for " + playerUuid);
    }

    /**
     * Merges remote statistics with local values using the HIGHEST_WINS strategy.
     * Since vanilla statistics are cumulative, the highest value is always correct.
     *
     * <p>This method:
     * <ul>
     *   <li>Compares remote and local values for each statistic</li>
     *   <li>Selects the highest value (HIGHEST_WINS strategy)</li>
     *   <li>Updates the local cache with merged values</li>
     *   <li>Logs conflicts for audit purposes</li>
     *   <li>Returns the merged statistics</li>
     * </ul>
     *
     * @param playerUuid     the player UUID
     * @param remoteStats    the statistics from the backend
     * @param localStats     the local statistics
     * @return the merged statistics with highest values
     */
    public @NotNull Map<String, Integer> mergeStatistics(
        final @NotNull UUID playerUuid,
        final @NotNull List<StatisticEntry> remoteStats,
        final @NotNull Map<String, Integer> localStats
    ) {
        Map<String, Integer> merged = new HashMap<>(localStats);
        int conflictCount = 0;
        int remoteWins = 0;
        int localWins = 0;

        for (StatisticEntry remoteStat : remoteStats) {
            String key = remoteStat.statisticKey();
            
            // Only process vanilla statistics
            if (!key.startsWith("minecraft.")) {
                continue;
            }

            // Get remote value
            Integer remoteValue = extractIntegerValue(remoteStat.value());
            if (remoteValue == null) {
                continue;
            }

            // Get local value
            Integer localValue = localStats.get(key);
            if (localValue == null) {
                // No local value, use remote
                merged.put(key, remoteValue);
                LOGGER.fine("New statistic from remote for " + playerUuid + "/" + key + 
                    ": " + remoteValue);
                continue;
            }

            // Apply HIGHEST_WINS strategy
            if (!remoteValue.equals(localValue)) {
                conflictCount++;
                int highestValue = Math.max(remoteValue, localValue);
                merged.put(key, highestValue);

                // Log conflict with resolution strategy
                String winner = remoteValue > localValue ? "REMOTE" : "LOCAL";
                if (remoteValue > localValue) {
                    remoteWins++;
                } else {
                    localWins++;
                }

                LOGGER.info("Conflict resolved for " + playerUuid + "/" + key + 
                    ": local=" + localValue + ", remote=" + remoteValue + 
                    ", winner=" + winner + " (HIGHEST_WINS), merged=" + highestValue);
            }
        }

        if (conflictCount > 0) {
            LOGGER.info("Merged " + conflictCount + " conflicting statistics for " + 
                playerUuid + " using HIGHEST_WINS strategy " +
                "(remote wins: " + remoteWins + ", local wins: " + localWins + ")");
        } else {
            LOGGER.fine("No conflicts found for " + playerUuid + 
                " (" + remoteStats.size() + " remote stats checked)");
        }

        // Update cache with merged values
        cacheManager.updateCache(playerUuid, merged);

        return merged;
    }

    /**
     * Creates a server-specific statistic key by appending the server identifier.
     * Used for tracking statistics per-server in addition to global tracking.
     *
     * <p>Format: {@code <base_key>@<server_id>}
     * <p>Example: {@code minecraft.blocks.mined.stone@server-123}
     *
     * @param baseKey the base statistic key
     * @return the server-specific key
     */
    public @NotNull String createServerSpecificKey(final @NotNull String baseKey) {
        return baseKey + "@" + serverIdentifier;
    }

    /**
     * Checks if a statistic key is server-specific.
     *
     * @param key the statistic key
     * @return true if the key contains a server identifier
     */
    public boolean isServerSpecificKey(final @NotNull String key) {
        return key.contains("@");
    }

    /**
     * Extracts the base key from a server-specific key.
     *
     * @param serverSpecificKey the server-specific key
     * @return the base key without server identifier
     */
    public @NotNull String extractBaseKey(final @NotNull String serverSpecificKey) {
        int atIndex = serverSpecificKey.indexOf('@');
        if (atIndex > 0) {
            return serverSpecificKey.substring(0, atIndex);
        }
        return serverSpecificKey;
    }

    /**
     * Gets the server identifier.
     *
     * @return the server identifier
     */
    public @NotNull String getServerIdentifier() {
        return serverIdentifier;
    }

    /**
     * Extracts an integer value from a statistic entry value.
     * Handles various numeric types.
     *
     * @param value the value object
     * @return the integer value, or null if not a number
     */
    private @Nullable Integer extractIntegerValue(final @Nullable Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Integer intValue) {
            return intValue;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        if (value instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Clears all cached sync data.
     * Useful for testing or when resetting the sync state.
     */
    public void clearCache() {
        int size = syncCache.size();
        syncCache.clear();
        LOGGER.info("Cleared sync cache (" + size + " entries)");
    }

    /**
     * Gets the number of cached sync entries.
     *
     * @return the cache size
     */
    public int getCacheSize() {
        return syncCache.size();
    }

    /**
     * Cached sync data with timestamp for expiration checking.
     */
    private static class CachedSyncData {
        final List<StatisticEntry> statistics;
        final long timestamp;

        CachedSyncData(final List<StatisticEntry> statistics) {
            this.statistics = statistics;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_VALIDITY_MS;
        }

        long getAgeSeconds() {
            return (System.currentTimeMillis() - timestamp) / 1000;
        }
    }
}
