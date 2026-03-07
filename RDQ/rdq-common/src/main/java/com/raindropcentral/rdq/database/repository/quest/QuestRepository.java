package com.raindropcentral.rdq.database.repository.quest;

import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.quest.model.QuestDifficulty;
import de.jexcellence.hibernate.repository.CachedRepository;
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
 * Provides cached access to quests with methods for finding by category,
 * difficulty, and enabled status.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestRepository extends CachedRepository<Quest, Long, String> {
	
	private final EntityManagerFactory entityManagerFactory;
	private final ExecutorService executor;
	
	/**
	 * Constructs a new {@code QuestRepository}.
	 *
	 * @param executor             the executor service for async operations
	 * @param entityManagerFactory the entity manager factory
	 * @param entityClass          the entity class type
	 * @param keyExtractor         function to extract the cache key from the entity
	 */
	public QuestRepository(
		@NotNull final ExecutorService executor,
		@NotNull final EntityManagerFactory entityManagerFactory,
		@NotNull final Class<Quest> entityClass,
		@NotNull final Function<Quest, String> keyExtractor
	) {
		super(executor, entityManagerFactory, entityClass, keyExtractor);
		this.entityManagerFactory = entityManagerFactory;
		this.executor = executor;
	}
	
	/**
	 * Finds all quests in a specific category.
	 *
	 * @param categoryId the category ID
	 * @return a future containing the list of quests
	 */
	public CompletableFuture<List<Quest>> findByCategory(@NotNull final Long categoryId) {
		return CompletableFuture.supplyAsync(() -> {
			var em = this.entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT q FROM Quest q " +
					"WHERE q.category.id = :categoryId " +
					"AND q.enabled = true " +
					"ORDER BY q.difficulty ASC",
					Quest.class
				)
				.setParameter("categoryId", categoryId)
				.getResultList();
			} finally {
				if (em.isOpen()) {
					em.close();
				}
			}
		}, this.executor);
	}
	
	/**
	 * Finds a quest by its unique identifier.
	 *
	 * @param identifier the quest identifier
	 * @return a future containing the optional quest
	 */
	public CompletableFuture<Optional<Quest>> findByIdentifier(@NotNull final String identifier) {
		return CompletableFuture.supplyAsync(() -> {
			var em = this.entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT q FROM Quest q WHERE q.identifier = :identifier",
					Quest.class
				)
				.setParameter("identifier", identifier)
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
	 * Finds all enabled quests.
	 *
	 * @return a future containing the list of enabled quests
	 */
	public CompletableFuture<List<Quest>> findAllEnabled() {
		return CompletableFuture.supplyAsync(() -> {
			var em = this.entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT q FROM Quest q WHERE q.enabled = true",
					Quest.class
				).getResultList();
			} finally {
				if (em.isOpen()) {
					em.close();
				}
			}
		}, this.executor);
	}
	
	/**
	 * Finds all quests with a specific difficulty level.
	 *
	 * @param difficulty the quest difficulty
	 * @return a future containing the list of quests
	 */
	public CompletableFuture<List<Quest>> findByDifficulty(@NotNull final QuestDifficulty difficulty) {
		return CompletableFuture.supplyAsync(() -> {
			var em = this.entityManagerFactory.createEntityManager();
			try {
				return em.createQuery(
					"SELECT q FROM Quest q " +
					"WHERE q.difficulty = :difficulty " +
					"AND q.enabled = true",
					Quest.class
				)
				.setParameter("difficulty", difficulty)
				.getResultList();
			} finally {
				if (em.isOpen()) {
					em.close();
				}
			}
		}, this.executor);
	}
}
