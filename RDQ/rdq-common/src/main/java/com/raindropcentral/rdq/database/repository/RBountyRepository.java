package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.entity.AbstractEntity;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class RBountyRepository extends GenericCachedRepository<RBounty, Long, Long> {

    public RBountyRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RBounty.class, AbstractEntity::getId);
    }

    public @NotNull CompletableFuture<Optional<RBounty>> findByPlayerAsync(final @NotNull RDQPlayer player) {
        return findByAttributesAsync(Map.of("player", player))
                .thenApply(Optional::ofNullable);
    }

    public @NotNull CompletableFuture<Optional<RBounty>> findByPlayerAsync(final @NotNull UUID uniqueId) {
        return findByAttributesAsync(Map.of("player.uniqueId", uniqueId))
                .thenApply(Optional::ofNullable);
    }


    public @NotNull CompletableFuture<Optional<RBounty>> findByCommissionerAsync(final @NotNull UUID commissioner) {
        return findByAttributesAsync(Map.of("commissioner", commissioner))
                .thenApply(Optional::ofNullable);
    }
}