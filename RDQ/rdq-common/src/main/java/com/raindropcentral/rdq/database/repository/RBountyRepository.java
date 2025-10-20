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

/**
 * Repository responsible for caching and retrieving {@link RBounty} entities.
 *
 * <p>
 * The repository delegates to the {@link GenericCachedRepository} base class for query execution while exposing domain
 * specific lookups that operate asynchronously on the supplied {@link ExecutorService}.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public final class RBountyRepository extends GenericCachedRepository<RBounty, Long, Long> {

    /**
     * Creates a new repository with the provided executor and entity manager factory.
     *
     * @param executor the executor used for asynchronous query execution
     * @param entityManagerFactory the entity manager factory supplying persistence contexts
     */
    public RBountyRepository(
            final @NotNull ExecutorService executor,
            final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RBounty.class, AbstractEntity::getId);
    }

    /**
     * Retrieves the bounty associated with the supplied player.
     *
     * @param player the player whose bounty should be located
     * @return a future containing the player's bounty when present
     */
    public @NotNull CompletableFuture<Optional<RBounty>> findByPlayerAsync(final @NotNull RDQPlayer player) {
        return findByAttributesAsync(Map.of("player", player))
                .thenApply(Optional::ofNullable);
    }

    /**
     * Retrieves the bounty associated with the specified commissioner identifier.
     *
     * @param commissioner the commissioner identifier used to locate the bounty
     * @return a future containing the commissioner's bounty when present
     */
    public @NotNull CompletableFuture<Optional<RBounty>> findByCommissionerAsync(final @NotNull UUID commissioner) {
        return findByAttributesAsync(Map.of("commissioner", commissioner))
                .thenApply(Optional::ofNullable);
    }
}