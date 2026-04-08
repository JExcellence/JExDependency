package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.entity.quest.QuestCompletionHistory;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManager;
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
 * This repository provides access to quest completion history records, which are used for
 * tracking repeatability, cooldowns, statistics, and leaderboards. Unlike player progress
 * entities, completion history is not cached as it's accessed less frequently.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class QuestCompletionHistoryRepository extends CachedRepository<QuestCompletionHistory, Long, Long> {
	
	private final EntityManagerFactory entityManagerFactory;
	private final ExecutorService executor;
	
	/**
	 * Constructs a new {@code QuestCompletionHistoryRepository} for managing {@link QuestCompletionHistory} entities.
	 *
	 * @param executor             the {@link ExecutorService} used for executing repository operations asynchronously
	 * @param entityManagerFactory the {@link EntityManagerFactory} used to create and manage entity managers for persistence operations
	 * @param entityClass          the entity class
	 * @param keyExtractor         function to extract the cache key from the entity
	 */
	public QuestCompletionHistoryRepository(
		final @NotNull ExecutorService executor,
		final @NotNull EntityManagerFactory entityManagerFactory,
		@NotNull Class<QuestCompletionHistory> entityClass,
		@NotNull Function<QuestCompletionHistory, Long> keyExtractor
	) {
		super(
			executor,
			entityManagerFactory,
			entityClass,
			keyExtractor
		);
		this.entityManagerFactory = entityManagerFactory;
		this.executor = executor;
	}
	
	/**
	 * Finds all completion history records for a specific player.
	 * <p>
	 * This method is used to display a player's quest completion history and statistics.
	 * Results are ordered by completion time (most recent first).
	 *
	 * @param playerId the player's UUID
	 * @return CompletableFuture containing the list of completion records
	 */
	@NotNull
	public CompletableFuture<List<QuestCompletionHistory>> findByPlayer(@NotNull final UUID playerId) {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT h FROM QuestCompletionHistory h WHERE h.player.uniqueId = :playerId ORDER BY h.completedAt DESC",
					QuestCompletionHistory.class
				)
				.setParameter("playerId", playerId)
				.getResultList();
			} finally {
				em.close();
			}
		}, executor);
	}
	
	/**
	 * Finds the most recent completion record for a specific player and quest.
	 * <p>
	 * This method is used for cooldown calculations and repeatability checks.
	 *
	 * @param playerId the player's UUID
	 * @param questId  the quest ID
	 * @return CompletableFuture containing the optional most recent completion record
	 */
	@NotNull
	public CompletableFuture<Optional<QuestCompletionHistory>> findLatestByPlayerAndQuest(
		@NotNull final UUID playerId,
		@NotNull final Long questId
	) {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				var results = em.createQuery(
					"SELECT h FROM QuestCompletionHistory h " +
					"WHERE h.player.uniqueId = :playerId AND h.quest.id = :questId " +
					"ORDER BY h.completedAt DESC",
					QuestCompletionHistory.class
				)
				.setParameter("playerId", playerId)
				.setParameter("questId", questId)
				.setMaxResults(1)
				.getResultList();
				
				return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
			} finally {
				em.close();
			}
		}, executor);
	}
	
	/**
	 * Counts how many times a player has completed a specific quest.
	 * <p>
	 * This method is used for repeatability checks and max completion enforcement.
	 *
	 * @param playerId the player's UUID
	 * @param questId  the quest ID
	 * @return CompletableFuture containing the completion count
	 */
	@NotNull
	public CompletableFuture<Long> countByPlayerAndQuest(
		@NotNull final UUID playerId,
		@NotNull final Long questId
	) {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT COUNT(h) FROM QuestCompletionHistory h " +
					"WHERE h.player.uniqueId = :playerId AND h.quest.id = :questId",
					Long.class
				)
				.setParameter("playerId", playerId)
				.setParameter("questId", questId)
				.getSingleResult();
			} finally {
				em.close();
			}
		}, executor);
	}
	
	/**
	 * Finds all completion records for a specific quest.
	 * <p>
	 * This method is used for quest statistics and leaderboards.
	 * Results are ordered by completion time (most recent first).
	 *
	 * @param quest the quest
	 * @return CompletableFuture containing the list of completion records
	 */
	@NotNull
	public CompletableFuture<List<QuestCompletionHistory>> findByQuest(@NotNull final Quest quest) {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT h FROM QuestCompletionHistory h WHERE h.quest = :quest ORDER BY h.completedAt DESC",
					QuestCompletionHistory.class
				)
				.setParameter("quest", quest)
				.getResultList();
			} finally {
				em.close();
			}
		}, executor);
	}
	
	/**
	 * Finds the fastest completions for a specific quest.
	 * <p>
	 * This method is used for leaderboards showing the fastest quest completions.
	 *
	 * @param quest the quest
	 * @param limit maximum number of results to return
	 * @return CompletableFuture containing the list of fastest completion records
	 */
	@NotNull
	public CompletableFuture<List<QuestCompletionHistory>> findFastestByQuest(
		@NotNull final Quest quest,
		final int limit
	) {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT h FROM QuestCompletionHistory h " +
					"WHERE h.quest = :quest " +
					"ORDER BY h.timeTakenSeconds ASC",
					QuestCompletionHistory.class
				)
				.setParameter("quest", quest)
				.setMaxResults(limit)
				.getResultList();
			} finally {
				em.close();
			}
		}, executor);
	}
	
	/**
	 * Finds all completion records for a player and quest.
	 * <p>
	 * This method returns the complete history of a player's completions for a specific quest,
	 * ordered by completion time (most recent first).
	 *
	 * @param playerId the player's UUID
	 * @param questId  the quest ID
	 * @return CompletableFuture containing the list of completion records
	 */
	@NotNull
	public CompletableFuture<List<QuestCompletionHistory>> findAllByPlayerAndQuest(
		@NotNull final UUID playerId,
		@NotNull final Long questId
	) {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT h FROM QuestCompletionHistory h " +
					"WHERE h.player.uniqueId = :playerId AND h.quest.id = :questId " +
					"ORDER BY h.completedAt DESC",
					QuestCompletionHistory.class
				)
				.setParameter("playerId", playerId)
				.setParameter("questId", questId)
				.getResultList();
			} finally {
				em.close();
			}
		}, executor);
	}
}
