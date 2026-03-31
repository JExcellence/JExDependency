package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.reward.BaseReward;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing {@link BaseReward} entities.
 * <p>
 * Extends {@link CachedRepository} to provide caching and asynchronous database operations
 * for reward entities.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class RRewardRepository extends CachedRepository<BaseReward, Long, Long> {

    /**
     * Constructs a new {@code RRewardRepository} with the specified executor and entity manager factory.
     *
     * @param executor             the {@link ExecutorService} for asynchronous operations
     * @param entityManagerFactory the {@link EntityManagerFactory} for JPA entity management
     * @param entityClass          the entity class
     * @param keyExtractor         function to extract the cache key from the entity
     */
    public RRewardRepository(
            final ExecutorService executor,
            final EntityManagerFactory entityManagerFactory,
            @NotNull Class<BaseReward> entityClass,
            @NotNull Function<BaseReward, Long> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
    }
}
