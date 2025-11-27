package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.entity.AbstractEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
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
 * @version 2.0.0
 * @since 2.0.0
 */
public final class RBountyRepository extends GenericCachedRepository<RBounty, Long, Long> {

    public RBountyRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RBounty.class, AbstractEntity::getId);
    }

    /**
     * Finds an active bounty by target player.
     *
     * @param player the target player
     * @return a future completing with the bounty, or empty if none exists
     */
    public @NotNull CompletableFuture<Optional<RBounty>> findActiveByTargetAsync(final @NotNull RDQPlayer player) {
        return findActiveByTargetAsync(player.getUniqueId());
    }

    /**
     * Finds an active bounty by target UUID.
     *
     * @param targetUuid the UUID of the target player
     * @return a future completing with the bounty, or empty if none exists
     */
    public @NotNull CompletableFuture<Optional<RBounty>> findActiveByTargetAsync(final @NotNull UUID targetUuid) {
        return findByAttributesAsync(Map.of("targetUniqueId", targetUuid, "active", true))
                .thenApply(Optional::ofNullable);
    }

    /**
     * Finds a bounty by target player (active or inactive).
     *
     * @param player the target player
     * @return a future completing with the bounty, or empty if none exists
     */
    public @NotNull CompletableFuture<Optional<RBounty>> findByPlayerAsync(final @NotNull RDQPlayer player) {
        return findByPlayerAsync(player.getUniqueId());
    }

    /**
     * Finds a bounty by target UUID (active or inactive).
     *
     * @param uniqueId the UUID of the target player
     * @return a future completing with the bounty, or empty if none exists
     */
    public @NotNull CompletableFuture<Optional<RBounty>> findByPlayerAsync(final @NotNull UUID uniqueId) {
        return findByAttributesAsync(Map.of("targetUniqueId", uniqueId))
                .thenApply(Optional::ofNullable);
    }

    /**
     * Finds all bounties created by a specific commissioner.
     *
     * @param commissioner the UUID of the commissioner
     * @return a future completing with the list of bounties
     */
    public @NotNull CompletableFuture<List<RBounty>> findByCommissionerAsync(final @NotNull UUID commissioner) {
        return findListByAttributesAsync(Map.of("commissionerUniqueId", commissioner));
    }

    /**
     * Finds all active bounties with pagination support.
     *
     * @param page     the page number (0-indexed)
     * @param pageSize the number of bounties per page
     * @return a future completing with the paginated list of active bounties
     */
    public @NotNull CompletableFuture<List<RBounty>> findAllActiveAsync(final int page, final int pageSize) {
        return CompletableFuture.supplyAsync(() -> {
            List<RBounty> allActive = findListByAttributes(Map.of("active", true));
            return allActive.stream()
                    .sorted(Comparator.comparing(RBounty::getCreatedAt).reversed())
                    .skip((long) page * pageSize)
                    .limit(pageSize)
                    .collect(Collectors.toList());
        });
    }

    /**
     * Finds all active bounties without pagination.
     *
     * @return a future completing with all active bounties
     */
    public @NotNull CompletableFuture<List<RBounty>> findAllActiveAsync() {
        return findListByAttributesAsync(Map.of("active", true))
                .thenApply(list -> list.stream()
                        .sorted(Comparator.comparing(RBounty::getCreatedAt).reversed())
                        .collect(Collectors.toList()));
    }

    /**
     * Finds all expired bounties that are still marked as active.
     *
     * @return a future completing with the list of expired bounties
     */
    public @NotNull CompletableFuture<List<RBounty>> findExpiredAsync() {
        return CompletableFuture.supplyAsync(() -> {
            LocalDateTime now = LocalDateTime.now();
            return findListByAttributes(Map.of("active", true)).stream()
                    .filter(bounty -> bounty.getExpiresAt()
                            .map(expiresAt -> now.isAfter(expiresAt))
                            .orElse(false))
                    .collect(Collectors.toList());
        });
    }

    /**
     * Counts the total number of active bounties.
     *
     * @return a future completing with the count of active bounties
     */
    public @NotNull CompletableFuture<Integer> countActiveAsync() {
        return CompletableFuture.supplyAsync(() -> 
            findListByAttributes(Map.of("active", true)).size()
        );
    }

    /**
     * Legacy method - use findByCommissionerAsync instead.
     *
     * @deprecated use {@link #findByCommissionerAsync(UUID)}
     */
    @Deprecated
    public @NotNull CompletableFuture<List<RBounty>> findAllByCommissionerAsync(final @NotNull UUID commissioner) {
        return findByCommissionerAsync(commissioner);
    }
}