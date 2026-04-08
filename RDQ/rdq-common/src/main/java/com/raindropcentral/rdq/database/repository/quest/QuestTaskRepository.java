package com.raindropcentral.rdq.database.repository.quest;

import com.raindropcentral.rdq.database.entity.quest.QuestTask;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing persistent {@link QuestTask} entities.
 * <p>
 * Provides cached access to quest tasks with methods for finding by quest
 * and by quest + task identifier combination.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestTaskRepository extends CachedRepository<QuestTask, Long, Long> {

	private final EntityManagerFactory entityManagerFactory;
	private final ExecutorService executor;

	/**
	 * Constructs a new {@code QuestTaskRepository}.
	 *
	 * @param executor             the executor service for async operations
	 * @param entityManagerFactory the entity manager factory
	 * @param entityClass          the entity class type
	 * @param keyExtractor         function to extract the cache key from the entity
	 */
	public QuestTaskRepository(
		@NotNull final ExecutorService executor,
		@NotNull final EntityManagerFactory entityManagerFactory,
		@NotNull final Class<QuestTask> entityClass,
		@NotNull final Function<QuestTask, Long> keyExtractor
	) {
		super(executor, entityManagerFactory, entityClass, keyExtractor);
		this.entityManagerFactory = entityManagerFactory;
		this.executor = executor;
	}

	/**
	 * Finds all tasks for a given quest, ordered by their display index.
	 *
	 * @param questId the quest's database ID
	 * @return a future containing the ordered list of tasks
	 */
	public CompletableFuture<List<QuestTask>> findByQuest(@NotNull final Long questId) {
		return CompletableFuture.supplyAsync(() -> {
			var em = this.entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT qt FROM QuestTask qt " +
					"WHERE qt.quest.id = :questId " +
					"ORDER BY qt.orderIndex ASC",
					QuestTask.class
				)
				.setParameter("questId", questId)
				.getResultList();
			} finally {
				if (em.isOpen()) {
					em.close();
				}
			}
		}, this.executor);
	}

	/**
	 * Finds a specific task within a quest by its task identifier.
	 *
	 * @param questId        the quest's database ID
	 * @param taskIdentifier the task's unique identifier within the quest
	 * @return a future containing the optional task
	 */
	public CompletableFuture<Optional<QuestTask>> findByQuestAndIdentifier(
		@NotNull final Long questId,
		@NotNull final String taskIdentifier
	) {
		return CompletableFuture.supplyAsync(() -> {
			var em = this.entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT qt FROM QuestTask qt " +
					"WHERE qt.quest.id = :questId " +
					"AND qt.taskIdentifier = :taskIdentifier",
					QuestTask.class
				)
				.setParameter("questId", questId)
				.setParameter("taskIdentifier", taskIdentifier)
				.getResultStream()
				.findFirst();
			} finally {
				if (em.isOpen()) {
					em.close();
				}
			}
		}, this.executor);
	}
}
