package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.quest.RQuest;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public final class RQuestRepository extends GenericCachedRepository<RQuest, Long, String> {

    public RQuestRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RQuest.class, RQuest::getIdentifier);
    }

    public @NotNull CompletableFuture<Optional<RQuest>> findByIdentifierAsync(final @NotNull String identifier) {
        return findByCacheKeyAsync("identifier", identifier)
                .thenApply(Optional::ofNullable);
    }
}