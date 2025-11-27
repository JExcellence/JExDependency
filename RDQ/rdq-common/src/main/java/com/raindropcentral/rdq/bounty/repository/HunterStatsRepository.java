package com.raindropcentral.rdq.bounty.repository;

import com.raindropcentral.rdq.database.entity.bounty.HunterStatsEntity;
import de.jexcellence.hibernate.entity.AbstractEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
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
 * @version 6.0.0
 * @since 6.0.0
 */
public final class HunterStatsRepository extends GenericCachedRepository<HunterStatsEntity, Long, UUID> {

    public HunterStatsRepository(
            final @NotNull ExecutorService executor,
            final @Nullable EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, HunterStatsEntity.class, HunterStatsEntity::playerId);
    }

    /**
     * Finds hunter statistics for a specific player by their UUID.
     *
     * @param playerId the UUID of the player
     * @return a future completing with the stats, or empty if none exist
     */
    public @NotNull CompletableFuture<Optional<HunterStatsEntity>> findByPlayerIdAsync(final @NotNull UUID playerId) {
        return findByAttributesAsync(Map.of("playerId", playerId))
                .thenApply(Optional::ofNullable);
    }

    /**
     * Retrieves the top bounty hunters ordered by specified criteria.
     *
     * @param limit   the maximum number of entries to return
     * @param orderBy the field to order by ("bountiesClaimed" or "totalEarned")
     * @return a future completing with the ordered list of hunter statistics
     */
    public @NotNull CompletableFuture<List<HunterStatsEntity>> findTopHuntersAsync(
            final int limit,
            final @NotNull String orderBy
    ) {
        return CompletableFuture.supplyAsync(() -> {
            List<HunterStatsEntity> list = findListByAttributes(Map.of());
            Comparator<HunterStatsEntity> comparator = switch (orderBy) {
                case "totalEarned", "total_earned" ->
                        Comparator.comparing(HunterStatsEntity::totalEarned).reversed();
                case "highestBounty", "highest_bounty" ->
                        Comparator.comparing(HunterStatsEntity::highestBounty).reversed();
                default ->
                        Comparator.comparing(HunterStatsEntity::bountiesClaimed).reversed();
            };
            return list.stream()
                    .filter(s -> s.bountiesClaimed() > 0)
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
    public @NotNull CompletableFuture<List<HunterStatsEntity>> findTopByBountiesClaimedAsync(final int limit) {
        return findTopHuntersAsync(limit, "bountiesClaimed");
    }

    /**
     * Retrieves the top bounty hunters by total earned.
     *
     * @param limit the maximum number of entries to return
     * @return a future completing with the ordered list of hunter statistics
     */
    public @NotNull CompletableFuture<List<HunterStatsEntity>> findTopByTotalEarnedAsync(final int limit) {
        return findTopHuntersAsync(limit, "totalEarned");
    }

    /**
     * Gets the rank position of a specific player based on bounties claimed.
     *
     * @param playerId the UUID of the player to rank
     * @return a future completing with the rank (1-indexed), or 0 if not ranked
     */
    public @NotNull CompletableFuture<Integer> getPlayerRankAsync(final @NotNull UUID playerId) {
        return findByPlayerIdAsync(playerId)
                .thenCompose(statsOpt -> {
                    if (statsOpt.isEmpty()) {
                        return CompletableFuture.completedFuture(0);
                    }

                    HunterStatsEntity playerStats = statsOpt.get();
                    int bountiesClaimed = playerStats.bountiesClaimed();

                    return CompletableFuture.supplyAsync(() -> {
                        List<HunterStatsEntity> allStats = findListByAttributes(Map.of());
                        long betterCount = allStats.stream()
                                .filter(stats -> stats.bountiesClaimed() > bountiesClaimed)
                                .count();
                        return (int) betterCount + 1;
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
