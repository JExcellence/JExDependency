package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.bounty.Bounty;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing {@link Bounty} entities in the RaindropQuests system.
 *
 * <p>Extends {@link CachedRepository} to provide caching and asynchronous database operations
 * for bounty entities, using the commissioner's UUID as the cache key.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class BountyRepository extends CachedRepository<Bounty, Long, Long> {

    /**
     * Constructs a new {@code BountyRepository} with the specified executor and entity manager factory.
     *
     * @param executor             the {@link ExecutorService} for asynchronous operations
     * @param entityManagerFactory the {@link EntityManagerFactory} for JPA entity management
     */
    public BountyRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<Bounty> entityClass,
            @NotNull Function<Bounty, Long> keyExtractor
    ) {
        
        super(
            executor,
            entityManagerFactory,
                entityClass, keyExtractor
        );
    }
}
