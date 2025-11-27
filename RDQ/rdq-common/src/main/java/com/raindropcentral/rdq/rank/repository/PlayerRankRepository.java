package com.raindropcentral.rdq.rank.repository;

import com.raindropcentral.rdq.database.entity.rank.PlayerRank;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Repository for managing player rank persistence.
 * <p>
 * This repository provides async database operations for player ranks including
 * queries by player, tree, and rank ID.
 * </p>
 *
 * @author JExcellence
 * @version 6.0.0
 * @since 6.0.0
 */
public final class PlayerRankRepository extends GenericCachedRepository<PlayerRank, Long, Long> {

    public PlayerRankRepository(
            final @NotNull ExecutorService executor,
            final @Nullable EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, PlayerRank.class, PlayerRank::getId);
    }

    /**
     * Finds all ranks for a specific player.
     *
     * @param playerId the UUID of the player
     * @return a future completing with the list of player ranks
     */
    public @NotNull CompletableFuture<List<PlayerRank>> findByPlayerIdAsync(final @NotNull UUID playerId) {
        return findListByAttributesAsync(Map.of("playerId", playerId));
    }

    /**
     * Finds all ranks for a player in a specific tree.
     *
     * @param playerId the UUID of the player
     * @param treeId   the ID of the rank tree
     * @return a future completing with the list of player ranks
     */
    public @NotNull CompletableFuture<List<PlayerRank>> findByPlayerIdAndTreeIdAsync(
            final @NotNull UUID playerId,
            final @NotNull String treeId
    ) {
        return findListByAttributesAsync(Map.of("playerId", playerId, "treeId", treeId))
                .thenApply(list -> list.stream()
                        .sorted(Comparator.comparing(PlayerRank::unlockedAt))
                        .collect(Collectors.toList()));
    }

    /**
     * Finds a specific rank for a player.
     *
     * @param playerId the UUID of the player
     * @param rankId   the ID of the rank
     * @return a future completing with the player rank, or empty if not found
     */
    public @NotNull CompletableFuture<Optional<PlayerRank>> findByPlayerIdAndRankIdAsync(
            final @NotNull UUID playerId,
            final @NotNull String rankId
    ) {
        return findByAttributesAsync(Map.of("playerId", playerId, "rankId", rankId))
                .thenApply(Optional::ofNullable);
    }

    /**
     * Checks if a player has unlocked a specific rank.
     *
     * @param playerId the UUID of the player
     * @param rankId   the ID of the rank
     * @return a future completing with true if the rank is unlocked
     */
    public @NotNull CompletableFuture<Boolean> hasUnlockedRankAsync(
            final @NotNull UUID playerId,
            final @NotNull String rankId
    ) {
        return findByPlayerIdAndRankIdAsync(playerId, rankId)
                .thenApply(Optional::isPresent);
    }

    /**
     * Counts the number of ranks a player has unlocked.
     *
     * @param playerId the UUID of the player
     * @return a future completing with the count
     */
    public @NotNull CompletableFuture<Long> countByPlayerIdAsync(final @NotNull UUID playerId) {
        return findByPlayerIdAsync(playerId)
                .thenApply(list -> (long) list.size());
    }
}
