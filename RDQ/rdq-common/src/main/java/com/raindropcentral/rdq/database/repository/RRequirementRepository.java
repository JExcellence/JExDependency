package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.requirement.BaseRequirement;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 *
 * <p>
 * Extends {@link de.jexcellence.hibernate.repository.CachedRepository} to provide caching and asynchronous database operations
 * for player entities, using the player's unique UUID as the cache key.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class RRequirementRepository extends CachedRepository<BaseRequirement, Long, Long> {

    /**
     * Constructs a new {@code RDQPlayerRepository} with the specified executor and entity manager factory.
     *
     * @param executor             the {@link java.util.concurrent.ExecutorService} for asynchronous operations
     * @param entityManagerFactory the {@link jakarta.persistence.EntityManagerFactory} for JPA entity management
     */
    public RRequirementRepository(
            final ExecutorService executor,
            final EntityManagerFactory entityManagerFactory,
            @NotNull Class<BaseRequirement> entityClass,
            @NotNull Function<BaseRequirement, Long> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
    }
}
