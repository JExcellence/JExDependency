package com.raindropcentral.rdq.database.repository.quest;

import com.raindropcentral.rdq.database.entity.quest.QuestReward;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing {@link QuestReward} entities.
 * <p>
 * Provides CRUD operations for quest-level rewards.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestRewardRepository extends CachedRepository<QuestReward, Long, Long> {

    /**
     * Constructs a new {@code QuestRewardRepository}.
     *
     * @param executor             the executor service for async operations
     * @param entityManagerFactory the entity manager factory
     * @param entityClass          the entity class type
     * @param keyExtractor         function to extract the cache key from the entity
     */
    public QuestRewardRepository(
            @NotNull final ExecutorService executor,
            @NotNull final EntityManagerFactory entityManagerFactory,
            @NotNull final Class<QuestReward> entityClass,
            @NotNull final Function<QuestReward, Long> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
    }
}
