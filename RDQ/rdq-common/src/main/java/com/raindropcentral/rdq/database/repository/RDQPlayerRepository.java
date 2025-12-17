package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing {@link RDQPlayer} entities in the RaindropQuests system.
 * <p>
 * Extends {@link CachedRepository} to provide caching and asynchronous database operations
 * for player entities, using the player's unique UUID as the cache key.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class RDQPlayerRepository extends CachedRepository<RDQPlayer, Long, UUID> {

    /**
     * Constructs a new {@code RDQPlayerRepository} with the specified executor and entity manager factory.
     *
     * @param executor             the {@link ExecutorService} for asynchronous operations
     * @param entityManagerFactory the {@link EntityManagerFactory} for JPA entity management
     */
    public RDQPlayerRepository(
            @NotNull ExecutorService executor,
            @NotNull EntityManagerFactory entityManagerFactory,
            @NotNull Class<RDQPlayer> entityClass,
            @NotNull Function<RDQPlayer, UUID> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
    }
}
