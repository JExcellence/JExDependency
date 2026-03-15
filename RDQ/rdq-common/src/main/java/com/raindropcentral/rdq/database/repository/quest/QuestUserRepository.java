package com.raindropcentral.rdq.database.repository.quest;

import com.raindropcentral.rdq.database.entity.quest.QuestUser;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing persistent {@link QuestUser} entities.
 *
 * <p>Provides cached access to player quest progress with methods for finding
 * active quests, completed quests, and quest progress by player and quest.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestUserRepository extends CachedRepository<QuestUser, Long, Long> {
	
	private final EntityManagerFactory entityManagerFactory;
	private final ExecutorService executor;
	
	/**
	 * Constructs a new {@code QuestUserRepository}.
	 *
	 * @param executor             the executor service for async operations
	 * @param entityManagerFactory the entity manager factory
	 * @param entityClass          the entity class type
	 * @param keyExtractor         function to extract the cache key from the entity
	 */
	public QuestUserRepository(
		@NotNull final ExecutorService executor,
		@NotNull final EntityManagerFactory entityManagerFactory,
		@NotNull final Class<QuestUser> entityClass,
		@NotNull final Function<QuestUser, Long> keyExtractor
	) {
		super(executor, entityManagerFactory, entityClass, keyExtractor);
		this.entityManagerFactory = entityManagerFactory;
		this.executor = executor;
	}
	
	/**
	 * Finds all active quests for a player.
	 * Uses JOIN FETCH to eagerly load quest and task progress data.
	 *
	 * @param playerId the player's UUID
	 * @return a future containing the list of active quest progress
	 */
	public CompletableFuture<List<QuestUser>> findActiveByPlayer(@NotNull final UUID playerId) {
		return CompletableFuture.supplyAsync(() -> {
			var em = this.entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT qu FROM QuestUser qu " +
					"JOIN FETCH qu.quest " +
					"LEFT JOIN FETCH qu.taskProgress " +
					"WHERE qu.playerId = :playerId " +
					"AND qu.completed = false " +
					"ORDER BY qu.startedAt DESC",
					QuestUser.class
				)
				.setParameter("playerId", playerId)
				.getResultList();
			} finally {
				if (em.isOpen()) {
					em.close();
				}
			}
		}, this.executor);
	}
	
	/**
	 * Finds active quest progress for a specific player and quest.
	 * Uses JOIN FETCH to eagerly load quest and task progress data.
	 *
	 * @param playerId       the player's UUID
	 * @param questIdentifier the quest identifier
	 * @return a future containing the optional quest progress
	 */
	public CompletableFuture<Optional<QuestUser>> findActiveByPlayerAndQuest(
		@NotNull final UUID playerId,
		@NotNull final String questIdentifier
	) {
		return CompletableFuture.supplyAsync(() -> {
			var em = this.entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT qu FROM QuestUser qu " +
					"JOIN FETCH qu.quest q " +
					"LEFT JOIN FETCH qu.taskProgress " +
					"WHERE qu.playerId = :playerId " +
					"AND q.identifier = :questIdentifier " +
					"AND qu.completed = false",
					QuestUser.class
				)
				.setParameter("playerId", playerId)
				.setParameter("questIdentifier", questIdentifier)
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
	 * Finds all completed quests for a player.
	 *
	 * @param playerId the player's UUID
	 * @return a future containing the list of completed quest progress
	 */
	public CompletableFuture<List<QuestUser>> findCompletedByPlayer(@NotNull final UUID playerId) {
		return CompletableFuture.supplyAsync(() -> {
			var em = this.entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT qu FROM QuestUser qu " +
					"JOIN FETCH qu.quest " +
					"WHERE qu.playerId = :playerId " +
					"AND qu.completed = true " +
					"ORDER BY qu.completedAt DESC",
					QuestUser.class
				)
				.setParameter("playerId", playerId)
				.getResultList();
			} finally {
				if (em.isOpen()) {
					em.close();
				}
			}
		}, this.executor);
	}
	
	/**
	 * Counts the number of active quests for a player.
	 *
	 * @param playerId the player's UUID
	 * @return a future containing the count of active quests
	 */
	public CompletableFuture<Integer> countActiveByPlayer(@NotNull final UUID playerId) {
		return CompletableFuture.supplyAsync(() -> {
			var em = this.entityManagerFactory.createEntityManager();
			try {
				Long count = em.createQuery(
					"SELECT COUNT(qu) FROM QuestUser qu " +
					"WHERE qu.playerId = :playerId " +
					"AND qu.completed = false",
					Long.class
				)
				.setParameter("playerId", playerId)
				.getSingleResult();
				
				return count.intValue();
			} finally {
				if (em.isOpen()) {
					em.close();
				}
			}
		}, this.executor);
	}
}
