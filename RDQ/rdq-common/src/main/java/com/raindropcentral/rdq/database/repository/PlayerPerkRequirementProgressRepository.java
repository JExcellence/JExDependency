package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.perk.RPerkRequirement;
import com.raindropcentral.rdq.database.entity.perk.RPlayerPerkRequirementProgress;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.entity.AbstractEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for managing RPlayerPerkRequirementProgress entities.
 * Provides cached data access and async helpers for common lookups.
 */
public class PlayerPerkRequirementProgressRepository extends GenericCachedRepository<RPlayerPerkRequirementProgress, Long, Long> {

    public PlayerPerkRequirementProgressRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RPlayerPerkRequirementProgress.class, AbstractEntity::getId);
    }

    /**
     * Finds a progress record for a given player and requirement asynchronously.
     *
     * @param player the RDQ player
     * @param requirement the perk requirement
     * @return future resolving to an optional progress entity
     */
    public @NotNull CompletableFuture<Optional<RPlayerPerkRequirementProgress>> findByPlayerAndRequirementAsync(
            final @NotNull RDQPlayer player,
            final @NotNull RPerkRequirement requirement
    ) {
        return findByAttributesAsync(Map.of("player", player, "requirement", requirement))
                .thenApply(Optional::ofNullable);
    }
}
