package de.jexcellence.jexplatform.progression;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TTL-based caching layer over {@link ProgressionValidator}.
 *
 * <p>Caches {@link ProgressionState} objects per player per node, reducing
 * database queries for frequently accessed data. Default TTL is 5 minutes.
 *
 * @param <T> the type of progression node
 * @author JExcellence
 * @since 1.0.0
 */
public class CachedProgressionValidator<T extends ProgressionNode<T>>
        extends ProgressionValidator<T> {

    private record CacheEntry<T extends ProgressionNode<T>>(
            ProgressionState<T> state,
            Instant expiresAt
    ) {
        /** Returns {@code true} if this entry has expired. */
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    /**
     * Cache performance metrics.
     *
     * @param hits      number of cache hits
     * @param misses    number of cache misses
     * @param hitRate   hit rate percentage (0–100)
     * @param cacheSize current number of cached entries
     * @param evictions total number of evictions
     */
    public record CacheMetrics(
            long hits, long misses, double hitRate, int cacheSize, long evictions
    ) {
        /**
         * Returns the total number of cache requests.
         *
         * @return hits + misses
         */
        public long totalRequests() {
            return hits + misses;
        }
    }

    private final Map<UUID, Map<String, CacheEntry<T>>> stateCache =
            new ConcurrentHashMap<>();
    private final Duration cacheTtl;
    private long cacheHits;
    private long cacheMisses;
    private long evictions;

    /**
     * Creates a cached validator with the default 5-minute TTL.
     *
     * @param completionTracker the completion tracker
     * @param nodes             all progression nodes
     */
    public CachedProgressionValidator(
            @NotNull CompletionTracker<T> completionTracker,
            @NotNull Collection<T> nodes) {
        this(completionTracker, nodes, Duration.ofMinutes(5));
    }

    /**
     * Creates a cached validator with a custom TTL.
     *
     * @param completionTracker the completion tracker
     * @param nodes             all progression nodes
     * @param cacheTtl          time-to-live for cache entries
     */
    public CachedProgressionValidator(
            @NotNull CompletionTracker<T> completionTracker,
            @NotNull Collection<T> nodes,
            @NotNull Duration cacheTtl) {
        super(completionTracker, nodes);
        this.cacheTtl = cacheTtl;
    }

    @Override
    @NotNull
    public CompletableFuture<ProgressionState<T>> getProgressionState(
            @NotNull UUID playerId,
            @NotNull String nodeIdentifier) {
        var playerCache = stateCache.get(playerId);
        if (playerCache != null) {
            var entry = playerCache.get(nodeIdentifier);
            if (entry != null && !entry.isExpired()) {
                cacheHits++;
                return CompletableFuture.completedFuture(entry.state());
            } else if (entry != null) {
                playerCache.remove(nodeIdentifier);
                evictions++;
            }
        }

        cacheMisses++;
        return super.getProgressionState(playerId, nodeIdentifier)
                .thenApply(state -> {
                    stateCache.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
                            .put(nodeIdentifier,
                                    new CacheEntry<>(state, Instant.now().plus(cacheTtl)));
                    return state;
                });
    }

    @Override
    public void invalidatePlayerCache(@NotNull UUID playerId) {
        var removed = stateCache.remove(playerId);
        if (removed != null) {
            evictions += removed.size();
        }
        super.invalidatePlayerCache(playerId);
    }

    /**
     * Returns cache performance metrics.
     *
     * @return the current cache metrics
     */
    @NotNull
    public CacheMetrics getCacheMetrics() {
        var totalRequests = cacheHits + cacheMisses;
        var hitRate = totalRequests > 0 ? (cacheHits * 100.0 / totalRequests) : 0.0;
        var cacheSize = stateCache.values().stream().mapToInt(Map::size).sum();
        return new CacheMetrics(cacheHits, cacheMisses, hitRate, cacheSize, evictions);
    }

    /**
     * Clears all cached data.
     */
    public void clearAllCaches() {
        var size = stateCache.values().stream().mapToInt(Map::size).sum();
        stateCache.clear();
        evictions += size;
    }

    /**
     * Removes expired entries from the cache.
     *
     * @return the number of entries removed
     */
    public int cleanupExpiredEntries() {
        var removed = 0;
        for (var playerCache : stateCache.values()) {
            var sizeBefore = playerCache.size();
            playerCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
            removed += (sizeBefore - playerCache.size());
        }
        evictions += removed;
        return removed;
    }
}
