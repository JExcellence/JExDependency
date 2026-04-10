package com.raindropcentral.rdq.database.repository.quest;

import com.raindropcentral.rdq.database.entity.quest.QuestTask;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
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

	/**
	 * Finds a specific task within a quest with rewards and requirements eagerly loaded.
	 * <p>
	 * Uses separate fetch steps to avoid MultipleBagFetchException while still
	 * returning a detached entity whose child collections are initialized.
	 *
	 * @param questId        the quest's database ID
	 * @param taskIdentifier the task's unique identifier within the quest
	 * @return a future containing the optional task with collections loaded
	 */
	public CompletableFuture<Optional<QuestTask>> findByQuestAndIdentifierWithCollections(
		@NotNull final Long questId,
		@NotNull final String taskIdentifier
	) {
		return CompletableFuture.supplyAsync(() -> {
			final EntityManager em = this.entityManagerFactory.createEntityManager();
			try {
				final EntityGraph<QuestTask> rewardGraph = em.createEntityGraph(QuestTask.class);
				rewardGraph.addAttributeNodes("rewards");

				final List<QuestTask> tasksWithRewards = em.createQuery(
					"SELECT qt FROM QuestTask qt " +
					"WHERE qt.quest.id = :questId " +
					"AND qt.taskIdentifier = :taskIdentifier",
					QuestTask.class
				)
				.setParameter("questId", questId)
				.setParameter("taskIdentifier", taskIdentifier)
				.setHint("jakarta.persistence.fetchgraph", rewardGraph)
				.getResultList();

				if (tasksWithRewards.isEmpty()) {
					return Optional.empty();
				}

				final QuestTask task = tasksWithRewards.get(0);

				em.createQuery(
					"SELECT DISTINCT qt FROM QuestTask qt " +
					"LEFT JOIN FETCH qt.requirements req " +
					"LEFT JOIN FETCH req.requirement " +
					"WHERE qt.id = :id",
					QuestTask.class
				)
				.setParameter("id", task.getId())
				.getSingleResult();

				return Optional.of(task);
			} finally {
				if (em.isOpen()) {
					em.close();
				}
			}
		}, this.executor);
	}
}
