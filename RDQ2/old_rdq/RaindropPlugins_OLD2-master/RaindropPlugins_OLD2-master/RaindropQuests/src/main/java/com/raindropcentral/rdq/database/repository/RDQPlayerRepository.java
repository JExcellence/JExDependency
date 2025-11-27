package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.RDQPlayer;
import de.jexcellence.hibernate.repository.GenericCachedRepository;
import jakarta.persistence.EntityManagerFactory;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

/**
 * Repository for managing {@link RDQPlayer} entities in the RaindropQuests system.
 * <p>
 * Extends {@link GenericCachedRepository} to provide caching and asynchronous database operations
 * for player entities, using the player's unique UUID as the cache key.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class RDQPlayerRepository extends GenericCachedRepository<RDQPlayer, Long, UUID> {

    /**
     * Constructs a new {@code RDQPlayerRepository} with the specified executor and entity manager factory.
     *
     * @param executor             the {@link ExecutorService} for asynchronous operations
     * @param entityManagerFactory the {@link EntityManagerFactory} for JPA entity management
     */
    public RDQPlayerRepository(
            final ExecutorService executor,
            final EntityManagerFactory entityManagerFactory
    ) {
        super(executor, entityManagerFactory, RDQPlayer.class, RDQPlayer::getUniqueId);
    }
}
