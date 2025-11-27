package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.RBounty;
import com.raindropcentral.rdq.database.entity.RDQPlayer;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;

import java.util.concurrent.ExecutorService;

/**
 * Repository for managing {@link RBounty} entities in the RaindropQuests system.
 * <p>
 * Extends {@link GenericCachedRepository} to provide caching and asynchronous database operations
 * for bounty entities, using the commissioner's UUID as the cache key.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class RBountyRepository extends GenericCachedRepository<RBounty, Long, RDQPlayer> {

    /**
     * Constructs a new {@code RBountyRepository} with the specified executor and entity manager factory.
     *
     * @param executor             the {@link ExecutorService} for asynchronous operations
     * @param entityManagerFactory the {@link EntityManagerFactory} for JPA entity management
     */
    public RBountyRepository(
            final ExecutorService executor,
            final EntityManagerFactory entityManagerFactory
    ) {
        
        super(
            executor,
            entityManagerFactory,
            RBounty.class,
            RBounty::getPlayer
        );
    }
}
