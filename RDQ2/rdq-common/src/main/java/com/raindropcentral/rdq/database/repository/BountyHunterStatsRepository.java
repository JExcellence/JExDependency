package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.bounty.BountyHunterStats;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.entity.AbstractEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Repository for managing bounty hunter statistics persistence.
 * <p>
 * This repository handles CRUD operations for hunter statistics and provides
 * specialized queries for leaderboard rankings and player-specific stats lookup.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 2.0.0
 */
public final class BountyHunterStatsRepository extends GenericCachedRepository<BountyHunterStats, Long, Long> {

    public BountyHunterStatsRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, BountyHunterStats.class, AbstractEntity::getId);
    }

    /**
     * Finds hunter statistics for a specific player by their RDQPlayer entity.
     *
     * @param player the player whose stats should be retrieved
     * @return a future completing with the stats, or empty if none exist
     */
    public @NotNull CompletableFuture<Optional<BountyHunterStats>> findByPlayerAsync(final @NotNull RDQPlayer player) {
        return findByAttributesAsync(Map.of("player", player))
                .thenApply(Optional::ofNullable);
    }

    /**
     * Finds hunter statistics for a specific player by their UUID.
     *
     * @param uniqueId the UUID of the player
     * @return a future completing with the stats, or empty if none exist
     */
    public @NotNull CompletableFuture<Optional<BountyHunterStats>> findByPlayerUuidAsync(final @NotNull UUID uniqueId) {
        return findByAttributesAsync(Map.of("player.uniqueId", uniqueId))
                .thenApply(Optional::ofNullable);
    }

    /**
     * Retrieves the top bounty hunters ordered by specified criteria.
     *
     * @param limit   the maximum number of entries to return
     * @param orderBy the field to order by ("bountiesClaimed" or "totalRewardValue")
     * @return a future completing with the ordered list of hunter statistics
     */
    public @NotNull CompletableFuture<List<BountyHunterStats>> findTopHuntersAsync(
            final int limit,
            final @NotNull String orderBy
    ) {
        return CompletableFuture.supplyAsync(() -> {
            List<BountyHunterStats> list = findListByAttributes(Map.of());
            Comparator<BountyHunterStats> comparator = switch (orderBy) {
                case "total_reward_value", "totalRewardValue" -> 
                    Comparator.comparing(BountyHunterStats::getTotalRewardValue).reversed();
                case "bounties_claimed", "bountiesClaimed" -> 
                    Comparator.comparing(BountyHunterStats::getBountiesClaimed).reversed();
                case "highest_bounty_value", "highestBountyValue" -> 
                    Comparator.comparing(BountyHunterStats::getHighestBountyValue).reversed();
                default -> 
                    Comparator.comparing(BountyHunterStats::getBountiesClaimed).reversed();
            };
            return list.stream()
                    .sorted(comparator)
                    .limit(limit)
                    .collect(Collectors.toList());
        });
    }

    /**
     * Retrieves the top bounty hunters by bounties claimed.
     *
     * @param limit the maximum number of entries to return
     * @return a future completing with the ordered list of hunter statistics
     */
    public @NotNull CompletableFuture<List<BountyHunterStats>> findTopByBountiesClaimedAsync(final int limit) {
        return findTopHuntersAsync(limit, "bountiesClaimed");
    }

    /**
     * Retrieves the top bounty hunters by total reward value.
     *
     * @param limit the maximum number of entries to return
     * @return a future completing with the ordered list of hunter statistics
     */
    public @NotNull CompletableFuture<List<BountyHunterStats>> findTopByRewardValueAsync(final int limit) {
        return findTopHuntersAsync(limit, "totalRewardValue");
    }

    /**
     * Retrieves paginated bounty hunter statistics ordered by bounties claimed.
     *
     * @param page     the page number (0-indexed)
     * @param pageSize the number of entries per page
     * @return a future completing with the paginated list of hunter statistics
     */
    public @NotNull CompletableFuture<List<BountyHunterStats>> findPaginatedAsync(
            final int page,
            final int pageSize
    ) {
        return CompletableFuture.supplyAsync(() -> {
            List<BountyHunterStats> list = findListByAttributes(Map.of());
            return list.stream()
                    .sorted(Comparator.comparing(BountyHunterStats::getBountiesClaimed).reversed())
                    .skip((long) page * pageSize)
                    .limit(pageSize)
                    .collect(Collectors.toList());
        });
    }

    /**
     * Gets the rank position of a specific player based on bounties claimed.
     *
     * @param playerUuid the UUID of the player to rank
     * @return a future completing with the rank (1-indexed), or 0 if not ranked
     */
    public @NotNull CompletableFuture<Integer> getPlayerRankAsync(final @NotNull UUID playerUuid) {
        return findByPlayerUuidAsync(playerUuid)
                .thenCompose(statsOpt -> {
                    if (statsOpt.isEmpty()) {
                        return CompletableFuture.completedFuture(0);
                    }
                    
                    BountyHunterStats playerStats = statsOpt.get();
                    int bountiesClaimed = playerStats.getBountiesClaimed();
                    
                    return CompletableFuture.supplyAsync(() -> {
                        List<BountyHunterStats> allStats = findListByAttributes(Map.of());
                        long betterCount = allStats.stream()
                                .filter(stats -> stats.getBountiesClaimed() > bountiesClaimed)
                                .count();
                        return (int) betterCount + 1;
                    });
                });
    }

    /**
     * Counts the number of players with more bounties claimed than the specified player.
     *
     * @param playerUuid the UUID of the player to compare against
     * @return a future completing with the count of players above
     */
    public @NotNull CompletableFuture<Integer> countPlayersAboveAsync(final @NotNull UUID playerUuid) {
        return findByPlayerUuidAsync(playerUuid)
                .thenCompose(statsOpt -> {
                    if (statsOpt.isEmpty()) {
                        return CompletableFuture.completedFuture(0);
                    }
                    
                    BountyHunterStats playerStats = statsOpt.get();
                    int bountiesClaimed = playerStats.getBountiesClaimed();
                    
                    return CompletableFuture.supplyAsync(() -> {
                        List<BountyHunterStats> allStats = findListByAttributes(Map.of());
                        return (int) allStats.stream()
                                .filter(stats -> stats.getBountiesClaimed() > bountiesClaimed)
                                .count();
                    });
                });
    }

    /**
     * Gets the total count of all hunter statistics records.
     *
     * @return a future completing with the total count
     */
    public @NotNull CompletableFuture<Integer> countAllAsync() {
        return CompletableFuture.supplyAsync(() -> 
            findListByAttributes(Map.of()).size()
        );
    }
}
