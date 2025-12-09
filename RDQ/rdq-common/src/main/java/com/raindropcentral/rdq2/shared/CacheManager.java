/*
package com.raindropcentral.rdq2.shared;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.raindropcentral.rdq2.bounty.dto.Bounty;
import com.raindropcentral.rdq2.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq2.database.entity.rank.RRank;
import com.raindropcentral.rdq2.database.entity.rank.RRankTree;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;

*/
/**
 * Centralized cache manager using Caffeine for high-performance caching.
 *
 * <p>Provides typed caches for common entities (players, ranks, bounties) and
 * a generic cache for arbitrary data. All caches are configured with appropriate
 * TTL and size limits.
 *
 * <p>Cache configuration:
 * <ul>
 *   <li>Players: 1000 max, 30 min access expiry</li>
 *   <li>Ranks: 500 max, 1 hour write expiry</li>
 *   <li>Rank Trees: 50 max, 1 hour write expiry</li>
 *   <li>Bounties: 500 max, 5 min write expiry</li>
 *   <li>Generic: 1000 max, 15 min write expiry</li>
 * </ul>
 *
 * @see com.github.benmanes.caffeine.cache.Cache
 *//*

public final class CacheManager {

    private static final Logger LOGGER = Logger.getLogger(CacheManager.class.getName());

    private final Cache<UUID, RDQPlayer> playerCache;
    private final Cache<String, RRank> rankCache;
    private final Cache<String, RRankTree> rankTreeCache;
    private final Cache<UUID, List<Bounty>> playerBountiesCache;
    private final Cache<String, Object> genericCache;

    public CacheManager() {
        this.playerCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(Duration.ofMinutes(30))
            .recordStats()
            .build();

        this.rankCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofHours(1))
            .recordStats()
            .build();

        this.rankTreeCache = Caffeine.newBuilder()
            .maximumSize(50)
            .expireAfterWrite(Duration.ofHours(1))
            .recordStats()
            .build();

        this.playerBountiesCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(Duration.ofMinutes(5))
            .recordStats()
            .build();

        this.genericCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(15))
            .recordStats()
            .build();

        LOGGER.info("CacheManager initialized with Caffeine caches");
    }

    @Nullable
    public RDQPlayer getPlayer(@NotNull UUID playerId) {
        return playerCache.getIfPresent(playerId);
    }

    @NotNull
    public RDQPlayer getPlayer(@NotNull UUID playerId, @NotNull Function<UUID, RDQPlayer> loader) {
        return playerCache.get(playerId, loader);
    }

    public void putPlayer(@NotNull RDQPlayer player) {
        playerCache.put(player.getUniqueId(), player);
    }

    public void invalidatePlayer(@NotNull UUID playerId) {
        playerCache.invalidate(playerId);
        playerBountiesCache.invalidate(playerId);
    }

    @Nullable
    public RRank getRank(@NotNull String rankId) {
        return rankCache.getIfPresent(rankId);
    }

    @NotNull
    public RRank getRank(@NotNull String rankId, @NotNull Function<String, RRank> loader) {
        return rankCache.get(rankId, loader);
    }

    public void putRank(@NotNull RRank rank) {
        rankCache.put(rank.getIdentifier(), rank);
    }

    public void invalidateRank(@NotNull String rankId) {
        rankCache.invalidate(rankId);
    }

    @Nullable
    public RRankTree getRankTree(@NotNull String treeId) {
        return rankTreeCache.getIfPresent(treeId);
    }

    @NotNull
    public RRankTree getRankTree(@NotNull String treeId, @NotNull Function<String, RRankTree> loader) {
        return rankTreeCache.get(treeId, loader);
    }

    public void putRankTree(@NotNull RRankTree tree) {
        rankTreeCache.put(tree.getIdentifier(), tree);
    }

    public void invalidateRankTree(@NotNull String treeId) {
        rankTreeCache.invalidate(treeId);
    }

    @Nullable
    public List<Bounty> getPlayerBounties(@NotNull UUID playerId) {
        return playerBountiesCache.getIfPresent(playerId);
    }

    @NotNull
    public List<Bounty> getPlayerBounties(@NotNull UUID playerId, @NotNull Function<UUID, List<Bounty>> loader) {
        return playerBountiesCache.get(playerId, loader);
    }

    public void putPlayerBounties(@NotNull UUID playerId, @NotNull List<Bounty> bounties) {
        playerBountiesCache.put(playerId, bounties);
    }

    public void invalidatePlayerBounties(@NotNull UUID playerId) {
        playerBountiesCache.invalidate(playerId);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T get(@NotNull String key) {
        return (T) genericCache.getIfPresent(key);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public <T> T get(@NotNull String key, @NotNull Function<String, T> loader) {
        return (T) genericCache.get(key, k -> loader.apply(k));
    }

    public void put(@NotNull String key, @NotNull Object value) {
        genericCache.put(key, value);
    }

    public void invalidate(@NotNull String key) {
        genericCache.invalidate(key);
    }

    public void invalidateAll() {
        playerCache.invalidateAll();
        rankCache.invalidateAll();
        rankTreeCache.invalidateAll();
        playerBountiesCache.invalidateAll();
        genericCache.invalidateAll();
        LOGGER.info("All caches invalidated");
    }

    public void cleanupPlayer(@NotNull UUID playerId) {
        playerCache.invalidate(playerId);
        playerBountiesCache.invalidate(playerId);
    }

    public CacheStats getStats() {
        return new CacheStats(
            playerCache.stats(),
            rankCache.stats(),
            rankTreeCache.stats(),
            playerBountiesCache.stats(),
            genericCache.stats()
        );
    }

    public record CacheStats(
        com.github.benmanes.caffeine.cache.stats.CacheStats players,
        com.github.benmanes.caffeine.cache.stats.CacheStats ranks,
        com.github.benmanes.caffeine.cache.stats.CacheStats rankTrees,
        com.github.benmanes.caffeine.cache.stats.CacheStats bounties,
        com.github.benmanes.caffeine.cache.stats.CacheStats generic
    ) {
        public double overallHitRate() {
            var totalHits = players.hitCount() + ranks.hitCount() + rankTrees.hitCount() 
                + bounties.hitCount() + generic.hitCount();
            var totalRequests = players.requestCount() + ranks.requestCount() + rankTrees.requestCount() 
                + bounties.requestCount() + generic.requestCount();
            return totalRequests == 0 ? 0.0 : (double) totalHits / totalRequests;
        }
    }
}
*/
