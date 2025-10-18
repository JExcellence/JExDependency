package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class RRankTreeRepository extends GenericCachedRepository<RRankTree, Long, String> {

    public RRankTreeRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RRankTree.class, RRankTree::getIdentifier);
    }

    public @NotNull CompletableFuture<Optional<RRankTree>> findByIdentifierAsync(final @NotNull String identifier) {
        return findByCacheKeyAsync("identifier", identifier).thenApply(Optional::ofNullable);
    }
}