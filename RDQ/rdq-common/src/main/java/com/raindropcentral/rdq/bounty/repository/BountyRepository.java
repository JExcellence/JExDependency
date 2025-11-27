package com.raindropcentral.rdq.bounty.repository;

import com.raindropcentral.rdq.bounty.Bounty;
import com.raindropcentral.rdq.database.entity.bounty.BountyEntity;
import com.raindropcentral.rdq.database.entity.bounty.BountyStatus;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Repository for managing bounty persistence and queries.
 * <p>
 * This repository provides async database operations for bounties including
 * queries by target, commissioner, active status, and expiration.
 * </p>
 *
 * @author JExcellence
 * @version 6.0.0
 * @since 6.0.0
 */
public final class BountyRepository extends GenericCachedRepository<BountyEntity, Long, Long> {

    public BountyRepository(
            final @NotNull ExecutorService executor,
            final @Nullable EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, BountyEntity.class, BountyEntity::getId);
    }

    /**
     * Finds an active bounty by target UUID.
     *
     * @param targetId the UUID of the target player
     * @return a future completing with the bounty, or empty if none exists
     */
    public @NotNull CompletableFuture<Optional<Bounty>> findActiveByTargetAsync(final @NotNull UUID targetId) {
        return findListByAttributesAsync(Map.of("targetId", targetId, "status", BountyStatus.ACTIVE))
                .thenApply(list -> list.stream()
                        .filter(b -> b.expiresAt() == null || b.expiresAt().isAfter(Instant.now()))
                        .findFirst()
                        .map(BountyEntity::toRecord));
    }

    /**
     * Finds all active bounties by target UUID.
     *
     * @param targetId the UUID of the target player
     * @return a future completing with the list of active bounties
     */
    public @NotNull CompletableFuture<List<Bounty>> findAllActiveByTargetAsync(final @NotNull UUID targetId) {
        return findListByAttributesAsync(Map.of("targetId", targetId, "status", BountyStatus.ACTIVE))
                .thenApply(list -> list.stream()
                        .filter(b -> b.expiresAt() == null || b.expiresAt().isAfter(Instant.now()))
                        .map(BountyEntity::toRecord)
                        .collect(Collectors.toList()));
    }

    /**
     * Finds all bounties created by a specific commissioner.
     *
     * @param placerId the UUID of the commissioner
     * @return a future completing with the list of bounties
     */
    public @NotNull CompletableFuture<List<Bounty>> findByPlacerIdAsync(final @NotNull UUID placerId) {
        return findListByAttributesAsync(Map.of("placerId", placerId))
                .thenApply(list -> list.stream()
                        .sorted(Comparator.comparing(BountyEntity::createdAt).reversed())
                        .map(BountyEntity::toRecord)
                        .collect(Collectors.toList()));
    }

    /**
     * Finds all bounties targeting a specific player.
     *
     * @param targetId the UUID of the target player
     * @return a future completing with the list of bounties
     */
    public @NotNull CompletableFuture<List<Bounty>> findByTargetIdAsync(final @NotNull UUID targetId) {
        return findListByAttributesAsync(Map.of("targetId", targetId))
                .thenApply(list -> list.stream()
                        .sorted(Comparator.comparing(BountyEntity::createdAt).reversed())
                        .map(BountyEntity::toRecord)
                        .collect(Collectors.toList()));
    }

    /**
     * Finds all active bounties with pagination support.
     *
     * @param page     the page number (0-indexed)
     * @param pageSize the number of bounties per page
     * @return a future completing with the paginated list of active bounties
     */
    public @NotNull CompletableFuture<List<Bounty>> findAllActiveAsync(final int page, final int pageSize) {
        return findListByAttributesAsync(Map.of("status", BountyStatus.ACTIVE))
                .thenApply(list -> list.stream()
                        .filter(b -> b.expiresAt() == null || b.expiresAt().isAfter(Instant.now()))
                        .sorted(Comparator.comparing(BountyEntity::amount).reversed())
                        .skip((long) page * pageSize)
                        .limit(pageSize)
                        .map(BountyEntity::toRecord)
                        .collect(Collectors.toList()));
    }

    /**
     * Finds all active bounties without pagination.
     *
     * @return a future completing with all active bounties
     */
    public @NotNull CompletableFuture<List<Bounty>> findAllActiveAsync() {
        return findListByAttributesAsync(Map.of("status", BountyStatus.ACTIVE))
                .thenApply(list -> list.stream()
                        .filter(b -> b.expiresAt() == null || b.expiresAt().isAfter(Instant.now()))
                        .sorted(Comparator.comparing(BountyEntity::amount).reversed())
                        .map(BountyEntity::toRecord)
                        .collect(Collectors.toList()));
    }

    /**
     * Finds all expired bounties that are still marked as active.
     *
     * @return a future completing with the list of expired bounties
     */
    public @NotNull CompletableFuture<List<Bounty>> findExpiredAsync() {
        return findListByAttributesAsync(Map.of("status", BountyStatus.ACTIVE))
                .thenApply(list -> {
                    Instant now = Instant.now();
                    return list.stream()
                            .filter(b -> b.expiresAt() != null && now.isAfter(b.expiresAt()))
                            .map(BountyEntity::toRecord)
                            .collect(Collectors.toList());
                });
    }

    /**
     * Counts the total number of active bounties.
     *
     * @return a future completing with the count of active bounties
     */
    public @NotNull CompletableFuture<Integer> countActiveAsync() {
        return findListByAttributesAsync(Map.of("status", BountyStatus.ACTIVE))
                .thenApply(list -> (int) list.stream()
                        .filter(b -> b.expiresAt() == null || b.expiresAt().isAfter(Instant.now()))
                        .count());
    }

    /**
     * Counts active bounties for a specific target.
     *
     * @param targetId the UUID of the target player
     * @return a future completing with the count
     */
    public @NotNull CompletableFuture<Long> countActiveByTargetAsync(final @NotNull UUID targetId) {
        return findListByAttributesAsync(Map.of("targetId", targetId, "status", BountyStatus.ACTIVE))
                .thenApply(list -> list.stream()
                        .filter(b -> b.expiresAt() == null || b.expiresAt().isAfter(Instant.now()))
                        .count());
    }

    /**
     * Creates a new bounty entity.
     *
     * @param entity the bounty entity to create
     * @return a future completing with the created bounty record
     */
    public @NotNull CompletableFuture<Bounty> createBountyAsync(final @NotNull BountyEntity entity) {
        return createAsync(entity).thenApply(BountyEntity::toRecord);
    }

    /**
     * Updates an existing bounty entity.
     *
     * @param entity the bounty entity to update
     * @return a future completing with the updated bounty record
     */
    public @NotNull CompletableFuture<Bounty> updateBountyAsync(final @NotNull BountyEntity entity) {
        return updateAsync(entity).thenApply(BountyEntity::toRecord);
    }

    /**
     * Finds a bounty entity by ID.
     *
     * @param id the bounty ID
     * @return a future completing with the bounty entity, or empty if not found
     */
    public @NotNull CompletableFuture<Optional<BountyEntity>> findEntityByIdAsync(final @NotNull Long id) {
        return findByCacheKeyAsync("id", id).thenApply(Optional::ofNullable);
    }
}
