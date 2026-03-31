package com.raindropcentral.rdq.database.repository.quest;

import com.raindropcentral.rdq.database.entity.quest.QuestTaskReward;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing {@link QuestTaskReward} entities.
 * <p>
 * Provides CRUD operations for task-level rewards.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestTaskRewardRepository extends CachedRepository<QuestTaskReward, Long, Long> {

	/**
	 * Constructs a new {@code QuestTaskRewardRepository}.
	 *
	 * @param executor             the executor service for async operations
	 * @param entityManagerFactory the entity manager factory
	 * @param entityClass          the entity class type
	 * @param keyExtractor         function to extract the cache key from the entity
	 */
	public QuestTaskRewardRepository(
			@NotNull final ExecutorService executor,
			@NotNull final EntityManagerFactory entityManagerFactory,
			@NotNull final Class<QuestTaskReward> entityClass,
			@NotNull final Function<QuestTaskReward, Long> keyExtractor
	) {
		super(executor, entityManagerFactory, entityClass, keyExtractor);
	}
}
