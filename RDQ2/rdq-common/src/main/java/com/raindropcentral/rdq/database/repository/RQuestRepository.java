package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.quest.RQuest;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository that provides cached access to {@link RQuest} entities and exposes asynchronous lookup helpers.
 *
 * <p>The repository delegates to the {@link GenericCachedRepository} base implementation to resolve quests by
 * their unique identifier while leveraging the shared executor for non-blocking database access.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public final class RQuestRepository extends GenericCachedRepository<RQuest, Long, String> {

    public RQuestRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RQuest.class, RQuest::getIdentifier);
    }

    /**
     * Retrieves a quest by its unique identifier using the cache when possible and falling back to the database if required.
     *
     * @param identifier the quest identifier to locate
     * @return a future completing with an optional quest match
     */
    public @NotNull CompletableFuture<Optional<RQuest>> findByIdentifierAsync(final @NotNull String identifier) {
        return findByCacheKeyAsync("identifier", identifier)
                .thenApply(Optional::ofNullable);
    }
}