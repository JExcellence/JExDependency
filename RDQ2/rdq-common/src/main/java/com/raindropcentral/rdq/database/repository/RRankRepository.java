package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.rank.RRank;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository responsible for loading and caching {@link RRank} entities from the RDQ persistence layer.
 * It leverages {@link GenericCachedRepository} to provide asynchronous lookup capabilities backed by a cache key.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public final class RRankRepository extends GenericCachedRepository<RRank, Long, String> {

    /**
     * Creates a new repository that resolves rank entities using the supplied executor and entity manager factory.
     *
     * @param executor              the asynchronous executor that performs database operations
     * @param entityManagerFactory  the factory supplying entity managers for persistence access
     */
    public RRankRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RRank.class, RRank::getIdentifier);
    }

    /**
     * Retrieves the rank that matches the provided identifier, resolving from cache or persistence as needed.
     *
     * @param identifier the unique identifier associated with the desired rank
     * @return a future containing the optional rank when the lookup completes
     */
    public @NotNull CompletableFuture<Optional<RRank>> findByIdentifierAsync(final @NotNull String identifier) {
        return findByCacheKeyAsync("identifier", identifier)
                .thenApply(Optional::ofNullable);
    }

    /**
     * Resolves the rank linked to the supplied LuckPerms group name.
     *
     * @param groupName the LuckPerms group to match against stored ranks
     * @return a future that yields the optional rank corresponding to the supplied group name
     */
    public @NotNull CompletableFuture<Optional<RRank>> findByLuckPermsGroupAsync(final @NotNull String groupName) {
        return findByAttributesAsync(Map.of("assignedLuckPermsGroup", groupName))
                .thenApply(Optional::ofNullable);
    }
}