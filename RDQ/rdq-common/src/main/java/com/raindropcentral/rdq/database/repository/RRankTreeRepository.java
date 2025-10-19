package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository providing cached access to {@link RRankTree} entities.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public final class RRankTreeRepository extends GenericCachedRepository<RRankTree, Long, String> {

    /**
     * Creates a repository backed by the provided executor and entity manager factory.
     *
     * @param executor the executor used for asynchronous repository operations
     * @param entityManagerFactory the factory supplying entity managers for persistence interactions
     */
    public RRankTreeRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RRankTree.class, RRankTree::getIdentifier);
    }

    /**
     * Asynchronously retrieves a rank tree by its unique identifier.
     *
     * @param identifier the unique identifier associated with the desired rank tree
     * @return a future containing the optional rank tree matching the identifier
     */
    public @NotNull CompletableFuture<Optional<RRankTree>> findByIdentifierAsync(final @NotNull String identifier) {
        return findByCacheKeyAsync("identifier", identifier).thenApply(Optional::ofNullable);
    }
}
