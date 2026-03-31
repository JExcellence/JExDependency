package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.entity.quest.QuestCategory;
import com.raindropcentral.rdq.model.quest.QuestDifficulty;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing persistent {@link Quest} entities.
 * <p>
 * This repository provides cached access to quests with methods for finding by identifier,
 * category, difficulty, and enabled status. It follows the CachedRepository pattern for
 * optimal performance with frequently accessed quest definitions.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class QuestRepository extends CachedRepository<Quest, Long, String> {
	
	private final EntityManagerFactory entityManagerFactory;
	private final ExecutorService executor;
	
	/**
	 * Constructs a new {@code QuestRepository} for managing {@link Quest} entities.
	 *
	 * @param executor             the {@link ExecutorService} used for executing repository operations asynchronously
	 * @param entityManagerFactory the {@link EntityManagerFactory} used to create and manage entity managers for persistence operations
	 * @param entityClass          the entity class
	 * @param keyExtractor         function to extract the cache key from the entity
	 */
	public QuestRepository(
		final @NotNull ExecutorService executor,
		final @NotNull EntityManagerFactory entityManagerFactory,
		@NotNull Class<Quest> entityClass,
		@NotNull Function<Quest, String> keyExtractor
	) {
		super(executor, entityManagerFactory, entityClass, keyExtractor);
		this.entityManagerFactory = entityManagerFactory;
		this.executor = executor;
	}
	
	/**
	 * Finds a quest by its unique identifier.
	 * <p>
	 * This method is used by the quest system to look up quests by their identifier.
	 * </p>
	 *
	 * @param identifier the quest identifier
	 * @return CompletableFuture containing the optional quest
	 */
	@NotNull
	public CompletableFuture<Optional<Quest>> findByIdentifier(@NotNull final String identifier) {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				var results = em.createQuery(
					"SELECT q FROM Quest q WHERE q.identifier = :identifier",
					Quest.class
				)
				.setParameter("identifier", identifier)
				.getResultList();
				
				return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
			} finally {
				em.close();
			}
		}, executor);
	}
	
	/**
	 * Finds all quests with the specified enabled status.
	 * <p>
	 * This method is used to filter quests based on their availability to players.
	 * </p>
	 *
	 * @param enabled whether to find enabled or disabled quests
	 * @return CompletableFuture containing the list of quests
	 */
	@NotNull
	public CompletableFuture<List<Quest>> findByEnabled(final boolean enabled) {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT q FROM Quest q WHERE q.enabled = :enabled",
					Quest.class
				)
				.setParameter("enabled", enabled)
				.getResultList();
			} finally {
				em.close();
			}
		}, executor);
	}
	
	/**
	 * Finds all quests belonging to a specific category.
	 * <p>
	 * This method is used to retrieve all quests within a category for display
	 * in category-based quest views.
	 * </p>
	 *
	 * @param category the quest category
	 * @return CompletableFuture containing the list of quests in the category
	 */
	@NotNull
	public CompletableFuture<List<Quest>> findByCategory(@NotNull final QuestCategory category) {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT q FROM Quest q WHERE q.category = :category ORDER BY q.difficulty",
					Quest.class
				)
				.setParameter("category", category)
				.getResultList();
			} finally {
				em.close();
			}
		}, executor);
	}
	
	/**
	 * Finds all quests with a specific difficulty level.
	 * <p>
	 * This method is used to filter quests by difficulty for progression tracking
	 * and quest recommendations.
	 * </p>
	 *
	 * @param difficulty the quest difficulty level
	 * @return CompletableFuture containing the list of quests with the specified difficulty
	 */
	@NotNull
	public CompletableFuture<List<Quest>> findByDifficulty(@NotNull final QuestDifficulty difficulty) {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT q FROM Quest q WHERE q.difficulty = :difficulty AND q.enabled = true",
					Quest.class
				)
				.setParameter("difficulty", difficulty)
				.getResultList();
			} finally {
				em.close();
			}
		}, executor);
	}
	
	/**
	 * Finds all quests in a category with tasks eagerly loaded.
	 * <p>
	 * This method uses JOIN FETCH to load tasks in a single query,
	 * avoiding N+1 query problems when displaying quest details.
	 * </p>
	 *
	 * @param category the quest category
	 * @return CompletableFuture containing the list of quests with eagerly loaded details
	 */
	@NotNull
	public CompletableFuture<List<Quest>> findByCategoryWithDetails(@NotNull final QuestCategory category) {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT DISTINCT q FROM Quest q " +
					"LEFT JOIN FETCH q.tasks " +
															"WHERE q.category = :category " +
					"ORDER BY q.difficulty",
					Quest.class
				)
				.setParameter("category", category)
				.getResultList();
			} finally {
				em.close();
			}
		}, executor);
	}
	
	/**
	 * Finds all enabled quests with their categories and tasks eagerly loaded.
	 * <p>
	 * This method is used for populating the quest cache on startup. Uses LEFT JOIN FETCH
	 * to eagerly load category and task associations, avoiding LazyInitializationException
	 * when these are accessed outside the Hibernate session.
	 * </p>
	 *
	 * @return CompletableFuture containing the list of all enabled quests
	 */
	@NotNull
	public CompletableFuture<List<Quest>> findAllEnabled() {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				// Load quests with category and tasks eagerly (avoids LazyInitializationException)
			final List<Quest> quests = em.createQuery(
				"SELECT DISTINCT q FROM Quest q " +
				"LEFT JOIN FETCH q.category " +
				"LEFT JOIN FETCH q.tasks " +
				"WHERE q.enabled = true " +
				"ORDER BY q.category.displayOrder, q.difficulty",
				Quest.class
			).getResultList();

			return quests;
			} finally {
				em.close();
			}
		}, executor);
	}
}
