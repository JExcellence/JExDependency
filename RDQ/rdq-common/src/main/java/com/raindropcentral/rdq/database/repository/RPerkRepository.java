package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.perk.RPerk;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository responsible for retrieving and caching {@link RPerk} entities by their identifiers.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public final class RPerkRepository extends GenericCachedRepository<RPerk, Long, String> {

    /**
     * Creates a new repository that loads perks using the provided asynchronous executor and entity manager factory.
     *
     * @param executor the executor service used to perform database operations off the main thread
     * @param entityManagerFactory the factory used to create entity managers for database interaction
     */
    public RPerkRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RPerk.class, RPerk::getIdentifier);
    }

    /**
     * Asynchronously locates a perk by its identifier using the cached lookup key.
     *
     * @param identifier the unique identifier of the perk being requested
     * @return a future containing the optional perk matching the supplied identifier
     */
    public @NotNull CompletableFuture<Optional<RPerk>> findByIdentifierAsync(final @NotNull String identifier) {
        return findByCacheKeyAsync("identifier", identifier)
                .thenApply(Optional::ofNullable);
    }
}