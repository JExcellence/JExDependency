package com.raindropcentral.rdq.perk.repository;

import com.raindropcentral.rdq.database.entity.perk.PerkProgressEntity;
import de.jexcellence.hibernate.entity.AbstractEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for managing perk progress persistence.
 * <p>
 * This repository provides async database operations for perk requirement progress
 * including queries by player, perk, and completion status.
 * </p>
 *
 * @author JExcellence
 * @version 6.0.0
 * @since 6.0.0
 */
public final class PerkProgressRepository extends GenericCachedRepository<PerkProgressEntity, Long, Long> {

    public PerkProgressRepository(
            final @NotNull ExecutorService executor,
            final @Nullable EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, PerkProgressEntity.class, AbstractEntity::getId);
    }

    /**
     * Finds all progress entries for a specific player.
     *
     * @param playerId the UUID of the player
     * @return a future completing with the list of progress entries
     */
    public @NotNull CompletableFuture<List<PerkProgressEntity>> findByPlayerIdAsync(final @NotNull UUID playerId) {
        return findListByAttributesAsync(Map.of("playerId", playerId));
    }

    /**
     * Finds all progress entries for a player and perk.
     *
     * @param playerId the UUID of the player
     * @param perkId   the ID of the perk
     * @return a future completing with the list of progress entries
     */
    public @NotNull CompletableFuture<List<PerkProgressEntity>> findByPlayerIdAndPerkIdAsync(
            final @NotNull UUID playerId,
            final @NotNull String perkId
    ) {
        return findListByAttributesAsync(Map.of("playerId", playerId, "perkId", perkId));
    }

    /**
     * Finds a specific progress entry.
     *
     * @param playerId        the UUID of the player
     * @param perkId          the ID of the perk
     * @param requirementType the type of requirement
     * @param requirementKey  the key of the requirement
     * @return a future completing with the progress entry, or empty if not found
     */
    public @NotNull CompletableFuture<Optional<PerkProgressEntity>> findByPlayerPerkAndRequirementAsync(
            final @NotNull UUID playerId,
            final @NotNull String perkId,
            final @NotNull String requirementType,
            final @NotNull String requirementKey
    ) {
        return findByAttributesAsync(Map.of(
                "playerId", playerId,
                "perkId", perkId,
                "requirementType", requirementType,
                "requirementKey", requirementKey
        )).thenApply(Optional::ofNullable);
    }

    /**
     * Finds all completed progress entries for a player.
     *
     * @param playerId the UUID of the player
     * @return a future completing with the list of completed progress entries
     */
    public @NotNull CompletableFuture<List<PerkProgressEntity>> findCompletedByPlayerIdAsync(final @NotNull UUID playerId) {
        return findListByAttributesAsync(Map.of("playerId", playerId, "completed", true));
    }

    /**
     * Finds all incomplete progress entries for a player.
     *
     * @param playerId the UUID of the player
     * @return a future completing with the list of incomplete progress entries
     */
    public @NotNull CompletableFuture<List<PerkProgressEntity>> findIncompleteByPlayerIdAsync(final @NotNull UUID playerId) {
        return findListByAttributesAsync(Map.of("playerId", playerId, "completed", false));
    }

    /**
     * Checks if all requirements for a perk are completed.
     *
     * @param playerId the UUID of the player
     * @param perkId   the ID of the perk
     * @return a future completing with true if all requirements are completed
     */
    public @NotNull CompletableFuture<Boolean> areAllRequirementsCompletedAsync(
            final @NotNull UUID playerId,
            final @NotNull String perkId
    ) {
        return findByPlayerIdAndPerkIdAsync(playerId, perkId)
                .thenApply(list -> !list.isEmpty() && list.stream().allMatch(PerkProgressEntity::completed));
    }

    /**
     * Counts completed requirements for a player and perk.
     *
     * @param playerId the UUID of the player
     * @param perkId   the ID of the perk
     * @return a future completing with the count
     */
    public @NotNull CompletableFuture<Long> countCompletedByPlayerIdAndPerkIdAsync(
            final @NotNull UUID playerId,
            final @NotNull String perkId
    ) {
        return findByPlayerIdAndPerkIdAsync(playerId, perkId)
                .thenApply(list -> list.stream().filter(PerkProgressEntity::completed).count());
    }

    /**
     * Counts total requirements for a player and perk.
     *
     * @param playerId the UUID of the player
     * @param perkId   the ID of the perk
     * @return a future completing with the count
     */
    public @NotNull CompletableFuture<Long> countTotalByPlayerIdAndPerkIdAsync(
            final @NotNull UUID playerId,
            final @NotNull String perkId
    ) {
        return findByPlayerIdAndPerkIdAsync(playerId, perkId)
                .thenApply(list -> (long) list.size());
    }
}
