package com.raindropcentral.rdq.perk.repository;

import com.raindropcentral.rdq.database.entity.perk.PlayerPerkEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Repository for managing player perk persistence.
 * <p>
 * This repository provides async database operations for player perks including
 * queries by player, perk ID, and unlock/active status.
 * </p>
 *
 * @author JExcellence
 * @version 6.0.0
 * @since 6.0.0
 */
public final class PlayerPerkRepository extends GenericCachedRepository<PlayerPerkEntity, Long, Long> {

    public PlayerPerkRepository(
            final @NotNull ExecutorService executor,
            final @Nullable EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, PlayerPerkEntity.class, PlayerPerkEntity::getId);
    }

    /**
     * Finds all perks for a specific player.
     *
     * @param playerId the UUID of the player
     * @return a future completing with the list of player perks
     */
    public @NotNull CompletableFuture<List<PlayerPerkEntity>> findByPlayerIdAsync(final @NotNull UUID playerId) {
        return findListByAttributesAsync(Map.of("playerId", playerId));
    }

    /**
     * Finds a specific perk for a player.
     *
     * @param playerId the UUID of the player
     * @param perkId   the ID of the perk
     * @return a future completing with the player perk, or empty if not found
     */
    public @NotNull CompletableFuture<Optional<PlayerPerkEntity>> findByPlayerIdAndPerkIdAsync(
            final @NotNull UUID playerId,
            final @NotNull String perkId
    ) {
        return findByAttributesAsync(Map.of("playerId", playerId, "perkId", perkId))
                .thenApply(Optional::ofNullable);
    }

    /**
     * Finds all unlocked perks for a player.
     *
     * @param playerId the UUID of the player
     * @return a future completing with the list of unlocked player perks
     */
    public @NotNull CompletableFuture<List<PlayerPerkEntity>> findUnlockedByPlayerIdAsync(final @NotNull UUID playerId) {
        return findListByAttributesAsync(Map.of("playerId", playerId, "unlocked", true));
    }

    /**
     * Finds all active perks for a player.
     *
     * @param playerId the UUID of the player
     * @return a future completing with the list of active player perks
     */
    public @NotNull CompletableFuture<List<PlayerPerkEntity>> findActiveByPlayerIdAsync(final @NotNull UUID playerId) {
        return findListByAttributesAsync(Map.of("playerId", playerId, "active", true));
    }

    /**
     * Checks if a player has unlocked a specific perk.
     *
     * @param playerId the UUID of the player
     * @param perkId   the ID of the perk
     * @return a future completing with true if the perk is unlocked
     */
    public @NotNull CompletableFuture<Boolean> hasUnlockedPerkAsync(
            final @NotNull UUID playerId,
            final @NotNull String perkId
    ) {
        return findByPlayerIdAndPerkIdAsync(playerId, perkId)
                .thenApply(opt -> opt.map(PlayerPerkEntity::unlocked).orElse(false));
    }

    /**
     * Counts the number of unlocked perks for a player.
     *
     * @param playerId the UUID of the player
     * @return a future completing with the count
     */
    public @NotNull CompletableFuture<Long> countUnlockedByPlayerIdAsync(final @NotNull UUID playerId) {
        return findUnlockedByPlayerIdAsync(playerId)
                .thenApply(list -> (long) list.size());
    }

    /**
     * Counts the number of active perks for a player.
     *
     * @param playerId the UUID of the player
     * @return a future completing with the count
     */
    public @NotNull CompletableFuture<Long> countActiveByPlayerIdAsync(final @NotNull UUID playerId) {
        return findActiveByPlayerIdAsync(playerId)
                .thenApply(list -> (long) list.size());
    }

    /**
     * Deactivates all perks for a player.
     *
     * @param playerId the UUID of the player
     * @return a future completing with the number of deactivated perks
     */
    public @NotNull CompletableFuture<Integer> deactivateAllByPlayerIdAsync(final @NotNull UUID playerId) {
        return findActiveByPlayerIdAsync(playerId)
                .thenCompose(perks -> {
                    List<CompletableFuture<PlayerPerkEntity>> updates = perks.stream()
                            .map(perk -> {
                                perk.deactivate();
                                return updateAsync(perk);
                            })
                            .collect(Collectors.toList());
                    return CompletableFuture.allOf(updates.toArray(new CompletableFuture[0]))
                            .thenApply(v -> updates.size());
                });
    }
}
