package com.raindropcentral.rdq.database.repository;

import com.raindropcentral.rdq.database.entity.quest.QuestCategory;
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
 * Repository for managing persistent {@link QuestCategory} entities.
 * <p>
 * This repository provides cached access to quest categories with methods for finding by identifier,
 * enabled status, and display order. Quest categories are used to organize quests into logical groups
 * for navigation and progression tracking.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class QuestCategoryRepository extends CachedRepository<QuestCategory, Long, String> {
	
	private final EntityManagerFactory entityManagerFactory;
	private final ExecutorService executor;
	
	/**
	 * Constructs a new {@code QuestCategoryRepository} for managing {@link QuestCategory} entities.
	 *
	 * @param executor             the {@link ExecutorService} used for executing repository operations asynchronously
	 * @param entityManagerFactory the {@link EntityManagerFactory} used to create and manage entity managers for persistence operations
	 * @param entityClass          the entity class
	 * @param keyExtractor         function to extract the cache key from the entity
	 */
	public QuestCategoryRepository(
		final @NotNull ExecutorService executor,
		final @NotNull EntityManagerFactory entityManagerFactory,
		@NotNull Class<QuestCategory> entityClass,
		@NotNull Function<QuestCategory, String> keyExtractor
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
	 * Finds a quest category by its unique identifier.
	 * <p>
	 * This method is used by the quest system to look up categories by their identifier.
	 * </p>
	 *
	 * @param identifier the category identifier
	 * @return CompletableFuture containing the optional category
	 */
	@NotNull
	public CompletableFuture<Optional<QuestCategory>> findByIdentifier(@NotNull final String identifier) {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				var results = em.createQuery(
					"SELECT c FROM QuestCategory c WHERE c.identifier = :identifier",
					QuestCategory.class
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
	 * Finds all quest categories with the specified enabled status.
	 * <p>
	 * This method is used to filter categories based on their availability to players.
	 * </p>
	 *
	 * @param enabled whether to find enabled or disabled categories
	 * @return CompletableFuture containing the list of categories
	 */
	@NotNull
	public CompletableFuture<List<QuestCategory>> findByEnabled(final boolean enabled) {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT c FROM QuestCategory c WHERE c.enabled = :enabled ORDER BY c.displayOrder",
					QuestCategory.class
				)
				.setParameter("enabled", enabled)
				.getResultList();
			} finally {
				em.close();
			}
		}, executor);
	}
	
	/**
	 * Finds all quest categories ordered by their display order.
	 * <p>
	 * This method is used to retrieve all categories in the correct order for display
	 * in quest navigation menus and category selection views.
	 * </p>
	 *
	 * @return CompletableFuture containing the list of all categories ordered by sort order
	 */
	@NotNull
	public CompletableFuture<List<QuestCategory>> findAllOrdered() {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT c FROM QuestCategory c ORDER BY c.sortOrder",
					QuestCategory.class
				)
				.getResultList();
			} finally {
				em.close();
			}
		}, executor);
	}
	
	/**
	 * Finds all enabled quest categories ordered by their display order.
	 * <p>
	 * This method is the most commonly used query for displaying available categories to players.
	 * </p>
	 *
	 * @return CompletableFuture containing the list of enabled categories ordered by display order
	 */
	@NotNull
	public CompletableFuture<List<QuestCategory>> findAllEnabledOrdered() {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT c FROM QuestCategory c WHERE c.enabled = true ORDER BY c.displayOrder",
					QuestCategory.class
				)
				.getResultList();
			} finally {
				em.close();
			}
		}, executor);
	}
	
	/**
	 * Finds a category with all its quests eagerly loaded.
	 * <p>
	 * This method uses JOIN FETCH to load quests in a single query,
	 * avoiding N+1 query problems when displaying category details.
	 * </p>
	 *
	 * @param identifier the category identifier
	 * @return CompletableFuture containing the optional category with eagerly loaded quests
	 */
	@NotNull
	public CompletableFuture<Optional<QuestCategory>> findByIdentifierWithQuests(@NotNull final String identifier) {
		return CompletableFuture.supplyAsync(() -> {
			EntityManager em = entityManagerFactory.createEntityManager();
			try {
				var results = em.createQuery(
					"SELECT DISTINCT c FROM QuestCategory c " +
					"LEFT JOIN FETCH c.quests " +
					"WHERE c.identifier = :identifier",
					QuestCategory.class
				)
				.setParameter("identifier", identifier)
				.getResultList();
				
				return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
			} finally {
				em.close();
			}
		}, executor);
	}
}
