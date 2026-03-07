package com.raindropcentral.rdq.database.repository.quest;

import com.raindropcentral.rdq.database.entity.quest.QuestCategory;
import de.jexcellence.hibernate.repository.CachedRepository;
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
 * Provides cached access to quest categories with methods for finding
 * enabled categories and ordering by display order.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestCategoryRepository extends CachedRepository<QuestCategory, Long, String> {
	
	private final EntityManagerFactory entityManagerFactory;
	private final ExecutorService executor;
	
	/**
	 * Constructs a new {@code QuestCategoryRepository}.
	 *
	 * @param executor             the executor service for async operations
	 * @param entityManagerFactory the entity manager factory
	 * @param entityClass          the entity class type
	 * @param keyExtractor         function to extract the cache key from the entity
	 */
	public QuestCategoryRepository(
		@NotNull final ExecutorService executor,
		@NotNull final EntityManagerFactory entityManagerFactory,
		@NotNull final Class<QuestCategory> entityClass,
		@NotNull final Function<QuestCategory, String> keyExtractor
	) {
		super(executor, entityManagerFactory, entityClass, keyExtractor);
		this.entityManagerFactory = entityManagerFactory;
		this.executor = executor;
	}
	
	/**
	 * Finds all enabled quest categories ordered by display order.
	 *
	 * @return a future containing the list of enabled categories
	 */
	public CompletableFuture<List<QuestCategory>> findAllEnabled() {
		return CompletableFuture.supplyAsync(() -> {
			var em = this.entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT qc FROM QuestCategory qc " +
					"WHERE qc.enabled = true " +
					"ORDER BY qc.displayOrder ASC",
					QuestCategory.class
				).getResultList();
			} finally {
				if (em.isOpen()) {
					em.close();
				}
			}
		}, this.executor);
	}
	
	/**
	 * Finds a quest category by its unique identifier.
	 *
	 * @param identifier the category identifier
	 * @return a future containing the optional category
	 */
	public CompletableFuture<Optional<QuestCategory>> findByIdentifier(@NotNull final String identifier) {
		return CompletableFuture.supplyAsync(() -> {
			var em = this.entityManagerFactory.createEntityManager();
			try {
				var result = em.createQuery(
					"SELECT qc FROM QuestCategory qc WHERE qc.identifier = :identifier",
					QuestCategory.class
				)
				.setParameter("identifier", identifier)
				.getResultStream()
				.findFirst();
				
				return result;
			} finally {
				if (em.isOpen()) {
					em.close();
				}
			}
		}, this.executor);
	}
	
	/**
	 * Finds all quest categories ordered by display order.
	 *
	 * @return a future containing the list of all categories
	 */
	public CompletableFuture<List<QuestCategory>> findAllOrderedByDisplayOrder() {
		return CompletableFuture.supplyAsync(() -> {
			var em = this.entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT qc FROM QuestCategory qc ORDER BY qc.displayOrder ASC",
					QuestCategory.class
				).getResultList();
			} finally {
				if (em.isOpen()) {
					em.close();
				}
			}
		}, this.executor);
	}
}
