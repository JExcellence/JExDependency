package com.raindropcentral.rdq.rank.repository;

import com.raindropcentral.rdq.database.entity.rank.PlayerRankPath;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Repository for managing player rank path persistence.
 * <p>
 * This repository provides async database operations for player rank paths
 * including queries by player and tree.
 * </p>
 *
 * @author JExcellence
 * @version 6.0.0
 * @since 6.0.0
 */
public final class PlayerRankPathRepository extends GenericCachedRepository<PlayerRankPath, Long, Long> {

    public PlayerRankPathRepository(
            final @NotNull ExecutorService executor,
            final @Nullable EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, PlayerRankPath.class, PlayerRankPath::getId);
    }

    /**
     * Finds all rank paths for a specific player.
     *
     * @param playerId the UUID of the player
     * @return a future completing with the list of player rank paths
     */
    public @NotNull CompletableFuture<List<PlayerRankPath>> findByPlayerIdAsync(final @NotNull UUID playerId) {
        return findListByAttributesAsync(Map.of("playerId", playerId));
    }

    /**
     * Finds all active rank paths for a specific player.
     *
     * @param playerId the UUID of the player
     * @return a future completing with the list of active player rank paths
     */
    public @NotNull CompletableFuture<List<PlayerRankPath>> findActiveByPlayerIdAsync(final @NotNull UUID playerId) {
        return findListByAttributesAsync(Map.of("playerId", playerId, "active", true));
    }

    /**
     * Finds a rank path for a player in a specific tree.
     *
     * @param playerId the UUID of the player
     * @param treeId   the ID of the rank tree
     * @return a future completing with the player rank path, or empty if not found
     */
    public @NotNull CompletableFuture<Optional<PlayerRankPath>> findByPlayerIdAndTreeIdAsync(
            final @NotNull UUID playerId,
            final @NotNull String treeId
    ) {
        return findByAttributesAsync(Map.of("playerId", playerId, "treeId", treeId))
                .thenApply(Optional::ofNullable);
    }

    /**
     * Counts the number of active rank paths for a player.
     *
     * @param playerId the UUID of the player
     * @return a future completing with the count
     */
    public @NotNull CompletableFuture<Long> countActiveByPlayerIdAsync(final @NotNull UUID playerId) {
        return findActiveByPlayerIdAsync(playerId)
                .thenApply(list -> (long) list.size());
    }

    /**
     * Deactivates all rank paths for a player.
     *
     * @param playerId the UUID of the player
     * @return a future completing when the operation is done
     */
    public @NotNull CompletableFuture<Void> deactivateAllForPlayerAsync(final @NotNull UUID playerId) {
        return findByPlayerIdAsync(playerId)
                .thenCompose(paths -> {
                    List<CompletableFuture<PlayerRankPath>> updates = paths.stream()
                            .filter(PlayerRankPath::isActive)
                            .map(path -> {
                                path.setActive(false);
                                return updateAsync(path);
                            })
                            .collect(Collectors.toList());
                    return CompletableFuture.allOf(updates.toArray(new CompletableFuture[0]));
                });
    }
}
