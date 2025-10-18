package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.perk.RPerk;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class RPerkRepository extends GenericCachedRepository<RPerk, Long, String> {

    public RPerkRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RPerk.class, RPerk::getIdentifier);
    }

    public @NotNull CompletableFuture<Optional<RPerk>> findByIdentifierAsync(final @NotNull String identifier) {
        return findByCacheKeyAsync("identifier", identifier)
                .thenApply(Optional::ofNullable);
    }
}