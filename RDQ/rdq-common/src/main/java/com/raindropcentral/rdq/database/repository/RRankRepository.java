package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.rank.RRank;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class RRankRepository extends GenericCachedRepository<RRank, Long, String> {

    public RRankRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RRank.class, RRank::getIdentifier);
    }

    public @NotNull CompletableFuture<Optional<RRank>> findByIdentifierAsync(final @NotNull String identifier) {
        return findByCacheKeyAsync("identifier", identifier)
                .thenApply(Optional::ofNullable);
    }

    public @NotNull CompletableFuture<Optional<RRank>> findByLuckPermsGroupAsync(final @NotNull String groupName) {
        return findByAttributesAsync(Map.of("assignedLuckPermsGroup", groupName))
                .thenApply(Optional::ofNullable);
    }
}