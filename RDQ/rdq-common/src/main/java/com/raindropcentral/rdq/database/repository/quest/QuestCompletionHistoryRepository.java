package com.raindropcentral.rdq.database.repository.quest;

import com.raindropcentral.rdq.database.entity.quest.QuestCompletionHistory;
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
 * Repository for managing persistent {@link QuestCompletionHistory} entities.
 * <p>
 * Provides cached access to quest completion history with methods for finding
 * completions by player, quest, and checking cooldown status.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestCompletionHistoryRepository extends CachedRepository<QuestCompletionHistory, Long, String> {
	
	private final EntityManagerFactory entityManagerFactory;
	private final ExecutorService executor;
	
	/**
	 * Constructs a new {@code QuestCompletionHistoryRepository}.
	 *
	 * @param executor             the executor service for async operations
	 * @param entityManagerFactory the entity manager factory
	 * @param entityClass          the entity class type
	 * @param keyExtractor         function to extract the cache key from the entity (playerId:questIdentifier)
	 */
	public QuestCompletionHistoryRepository(
		@NotNull final ExecutorService executor,
		@NotNull final EntityManagerFactory entityManagerFactory,
		@NotNull final Class<QuestCompletionHistory> entityClass,
		@NotNull final Function<QuestCompletionHistory, String> keyExtractor
	) {
		super(executor, entityManagerFactory, entityClass, keyExtractor);
		this.entityManagerFactory = entityManagerFactory;
		this.executor = executor;
	}
	
	/**
	 * Finds all completion history entries for a specific player.
	 *
	 * @param playerId the player's UUID
	 * @return a future containing the list of completion history entries
	 */
	public CompletableFuture<List<QuestCompletionHistory>> findByPlayer(@NotNull final UUID playerId) {
		return CompletableFuture.supplyAsync(() -> {
			var em = this.entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT qch FROM QuestCompletionHistory qch " +
					"WHERE qch.playerId = :playerId " +
					"ORDER BY qch.completedAt DESC",
					QuestCompletionHistory.class
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
	 * Finds all completion history entries for a specific quest.
	 *
	 * @param questIdentifier the quest identifier
	 * @return a future containing the list of completion history entries
	 */
	public CompletableFuture<List<QuestCompletionHistory>> findByQuest(@NotNull final String questIdentifier) {
		return CompletableFuture.supplyAsync(() -> {
			var em = this.entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT qch FROM QuestCompletionHistory qch " +
					"WHERE qch.questIdentifier = :questIdentifier " +
					"ORDER BY qch.completedAt DESC",
					QuestCompletionHistory.class
				)
				.setParameter("questIdentifier", questIdentifier)
				.getResultList();
			} finally {
				if (em.isOpen()) {
					em.close();
				}
			}
		}, this.executor);
	}
	
	/**
	 * Finds completion history for a specific player and quest.
	 *
	 * @param playerId        the player's UUID
	 * @param questIdentifier the quest identifier
	 * @return a future containing the optional completion history entry
	 */
	public CompletableFuture<Optional<QuestCompletionHistory>> findByPlayerAndQuest(
		@NotNull final UUID playerId,
		@NotNull final String questIdentifier
	) {
		return CompletableFuture.supplyAsync(() -> {
			var em = this.entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT qch FROM QuestCompletionHistory qch " +
					"WHERE qch.playerId = :playerId " +
					"AND qch.questIdentifier = :questIdentifier",
					QuestCompletionHistory.class
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
	 * Counts the number of times a player has completed a specific quest.
	 *
	 * @param playerId        the player's UUID
	 * @param questIdentifier the quest identifier
	 * @return a future containing the completion count
	 */
	public CompletableFuture<Integer> countCompletions(
		@NotNull final UUID playerId,
		@NotNull final String questIdentifier
	) {
		return CompletableFuture.supplyAsync(() -> {
			var em = this.entityManagerFactory.createEntityManager();
			try {
				Long count = em.createQuery(
					"SELECT COUNT(qch) FROM QuestCompletionHistory qch " +
					"WHERE qch.playerId = :playerId " +
					"AND qch.questIdentifier = :questIdentifier",
					Long.class
				)
				.setParameter("playerId", playerId)
				.setParameter("questIdentifier", questIdentifier)
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
