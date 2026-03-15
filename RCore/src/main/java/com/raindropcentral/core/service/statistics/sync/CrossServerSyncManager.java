package com.raindropcentral.core.service.statistics.sync;

import com.raindropcentral.core.service.central.RCentralApiClient;
import com.raindropcentral.core.service.statistics.config.StatisticsDeliveryConfig;
import com.raindropcentral.core.service.statistics.delivery.StatisticEntry;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages cross-server statistic synchronization.
 * Handles fetching and merging statistics from the RaindropCentral backend
 * when players join the server.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class CrossServerSyncManager {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RCore");

    private final RCentralApiClient apiClient;
    private final String apiKey;
    private final ConflictResolver conflictResolver;
    private final StatisticsDeliveryConfig config;

    // Cache of synced statistics with timestamps
    private final Map<UUID, CachedPlayerStatistics> syncCache;

    // Callback for applying synced statistics
    private SyncCallback syncCallback;

    /**
     * Executes CrossServerSyncManager.
     */
    public CrossServerSyncManager(
        final @NotNull RCentralApiClient apiClient,
        final @NotNull String apiKey,
        final @NotNull ConflictResolver conflictResolver,
        final @NotNull StatisticsDeliveryConfig config
    ) {
        this.apiClient = apiClient;
        this.apiKey = apiKey;
        this.conflictResolver = conflictResolver;
        this.config = config;
        this.syncCache = new ConcurrentHashMap<>();
    }

    /**
     * Sets the callback for applying synced statistics.
     *
     * @param callback the sync callback
     */
    public void setSyncCallback(final @NotNull SyncCallback callback) {
        this.syncCallback = callback;
    }

    /**
     * Requests the latest statistics for a player from the backend.
     * Called when a player joins the server to sync cross-server data.
     *
     * @param playerUuid the player UUID
     * @return a future that completes when sync is done
     */
    public CompletableFuture<Void> requestLatestStatistics(final @NotNull UUID playerUuid) {
        if (!config.isEnableCrossServerSync()) {
            return CompletableFuture.completedFuture(null);
        }

        // Check cache validity
        CachedPlayerStatistics cached = syncCache.get(playerUuid);
        if (cached != null && !cached.isExpired(config.getCacheValidityMs())) {
            LOGGER.fine("Using cached statistics for " + playerUuid);
            return CompletableFuture.completedFuture(null);
        }

        LOGGER.fine("Requesting statistics for " + playerUuid + " from backend");

        return apiClient.requestPlayerStatistics(apiKey, playerUuid)
            .thenAccept(remoteStats -> {
                if (!remoteStats.isEmpty()) {
                    syncCache.put(playerUuid, new CachedPlayerStatistics(remoteStats));
                    LOGGER.fine("Cached " + remoteStats.size() + " statistics for " + playerUuid);
                }
            })
            .exceptionally(e -> {
                LOGGER.warning("Failed to request statistics for " + playerUuid + ": " + e.getMessage());
                return null;
            });
    }


    /**
     * Synchronizes player statistics with the backend.
     * Fetches remote statistics and merges them with local values.
     *
     * @param playerUuid the player UUID
     * @return a future containing the sync result
     */
    public CompletableFuture<SyncResult> syncPlayerStatistics(final @NotNull UUID playerUuid) {
        if (!config.isEnableCrossServerSync()) {
            return CompletableFuture.completedFuture(new SyncResult(true, 0, 0));
        }

        return apiClient.requestPlayerStatistics(apiKey, playerUuid)
            .thenApply(remoteStats -> {
                if (remoteStats.isEmpty()) {
                    return new SyncResult(true, 0, 0);
                }

                int synced = 0;
                int conflicts = 0;

                for (StatisticEntry remoteStat : remoteStats) {
                    Object localValue = getLocalValue(playerUuid, remoteStat.statisticKey());
                    Object remoteValue = remoteStat.value();

                    // Resolve conflict
                    ConflictResolver.ResolutionResult resolution = conflictResolver.resolveWithMetadata(
                        remoteStat.statisticKey(),
                        localValue,
                        remoteValue,
                        config.getDefaultConflictStrategy()
                    );

                    if (resolution.hadConflict()) {
                        conflicts++;
                        LOGGER.fine("Conflict resolved for " + playerUuid + "/" + remoteStat.statisticKey() +
                            ": " + localValue + " vs " + remoteValue + " -> " + resolution.resolvedValue());
                    }

                    // Apply resolved value
                    if (syncCallback != null) {
                        syncCallback.applyStatistic(playerUuid, remoteStat.statisticKey(), resolution.resolvedValue());
                    }
                    synced++;
                }

                // Update cache
                syncCache.put(playerUuid, new CachedPlayerStatistics(remoteStats));

                LOGGER.info("Synced " + synced + " statistics for " + playerUuid +
                    " (" + conflicts + " conflicts resolved)");

                return new SyncResult(true, synced, conflicts);
            })
            .exceptionally(e -> {
                LOGGER.warning("Failed to sync statistics for " + playerUuid + ": " + e.getMessage());
                return new SyncResult(false, 0, 0);
            });
    }

    /**
     * Gets cached remote statistics for a player.
     *
     * @param playerUuid the player UUID
     * @return the cached statistics or null
     */
    public @Nullable List<StatisticEntry> getCachedStatistics(final @NotNull UUID playerUuid) {
        CachedPlayerStatistics cached = syncCache.get(playerUuid);
        return cached != null ? cached.statistics : null;
    }

    /**
     * Checks if cached statistics are valid for a player.
     *
     * @param playerUuid the player UUID
     * @return true if cache is valid
     */
    public boolean isCacheValid(final @NotNull UUID playerUuid) {
        CachedPlayerStatistics cached = syncCache.get(playerUuid);
        return cached != null && !cached.isExpired(config.getCacheValidityMs());
    }

    /**
     * Invalidates the cache for a player.
     *
     * @param playerUuid the player UUID
     */
    public void invalidateCache(final @NotNull UUID playerUuid) {
        syncCache.remove(playerUuid);
    }

    /**
     * Clears all cached statistics.
     */
    public void clearCache() {
        syncCache.clear();
    }

    /**
     * Gets a local statistic value (to be implemented by callback).
     */
    private Object getLocalValue(final UUID playerUuid, final String statisticKey) {
        if (syncCallback != null) {
            return syncCallback.getLocalStatistic(playerUuid, statisticKey);
        }
        return null;
    }

    // ==================== Inner Classes ====================

    /**
     * Cached player statistics with timestamp.
     */
    private static class CachedPlayerStatistics {
        final List<StatisticEntry> statistics;
        final long timestamp;

        CachedPlayerStatistics(List<StatisticEntry> statistics) {
            this.statistics = statistics;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired(long validityMs) {
            return System.currentTimeMillis() - timestamp > validityMs;
        }
    }

    /**
     * Result of a synchronization operation.
     */
    public record SyncResult(
        boolean success,
        int statisticsSynced,
        int conflictsResolved
    ) {}

    /**
     * Statistic scope for cross-server sync.
     */
    public enum StatisticScope {
        /** Statistic is global across all servers. */
        GLOBAL,
        /** Statistic is specific to a server. */
        SERVER_SPECIFIC,
        /** Statistic is specific to a world. */
        WORLD_SPECIFIC
    }

    /**
     * Callback interface for sync operations.
     */
    public interface SyncCallback {
        /**
         * Gets a local statistic value.
         */
        Object getLocalStatistic(UUID playerUuid, String statisticKey);

        /**
         * Applies a synced statistic value.
         */
        void applyStatistic(UUID playerUuid, String statisticKey, Object value);
    }
}
