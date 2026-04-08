package com.raindropcentral.rdq.database.repository.quest;

import com.raindropcentral.rdq.database.entity.quest.Quest;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing persistent {@link Quest} entities.
 * <p>
 * Uses {@link CachedRepository} built-in methods and entity graphs to avoid MultipleBagFetchException.
 * Entity graphs allow eager loading of specific collections without causing Hibernate's
 * "cannot simultaneously fetch multiple bags" error.
 * The cache key is the quest {@code identifier} string.
 *
 * @author RaindropCentral
 * @version 3.0.0
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
     * Finds all enabled quests in a specific category.
     * <p>
     * Uses a custom JPQL query with JOIN FETCH to eagerly load tasks collection.
     *
     * @param categoryId the category database ID
     * @return a future containing the list of quests
     */
    @NotNull
    public CompletableFuture<List<Quest>> findByCategory(@NotNull final Long categoryId) {
        return CompletableFuture.supplyAsync(() -> {
            final EntityManager em = entityManagerFactory.createEntityManager();
            try {
                // Use JOIN FETCH to eagerly load the tasks collection
                final List<Quest> quests = em.createQuery(
                    "SELECT DISTINCT q FROM Quest q LEFT JOIN FETCH q.tasks WHERE q.category.id = :categoryId AND q.enabled = true",
                    Quest.class
                )
                .setParameter("categoryId", categoryId)
                .getResultList();
                
                return quests;
            } finally {
                if (em.isOpen()) {
                    em.close();
                }
            }
        }, executor);
    }

    /**
     * Finds all enabled quests in a specific category with rewards eagerly loaded.
     * <p>
     * Uses entity graphs to load both tasks and rewards collections without causing
     * MultipleBagFetchException. This is useful for GUI displays that need to show
     * reward information.
     *
     * @param categoryId the category database ID
     * @return a future containing the list of quests with rewards loaded
     */
    @NotNull
    public CompletableFuture<List<Quest>> findByCategoryWithRewards(@NotNull final Long categoryId) {
        return CompletableFuture.supplyAsync(() -> {
            final EntityManager em = entityManagerFactory.createEntityManager();
            try {
                // First: Load quests with tasks
                final List<Quest> quests = em.createQuery(
                    "SELECT DISTINCT q FROM Quest q LEFT JOIN FETCH q.tasks WHERE q.category.id = :categoryId AND q.enabled = true",
                    Quest.class
                )
                .setParameter("categoryId", categoryId)
                .getResultList();
                
                // Second: Load rewards with BaseReward for each quest
                if (!quests.isEmpty()) {
                    for (final Quest quest : quests) {
                        em.createQuery(
                            "SELECT DISTINCT q FROM Quest q " +
                            "LEFT JOIN FETCH q.rewards r " +
                            "LEFT JOIN FETCH r.reward " +
                            "WHERE q.id = :id",
                            Quest.class
                        )
                        .setParameter("id", quest.getId())
                        .getSingleResult();
                    }
                }
                
                return quests;
            } finally {
                if (em.isOpen()) {
                    em.close();
                }
            }
        }, executor);
    }

    /**
     * Finds a quest by its unique identifier.
     * <p>
     * Checks the Caffeine key cache first (O(1)), then falls back to
     * {@code findByAttribute("identifier", identifier)} via the inherited
     * {@link #findByKeyAsync} method.
     *
     * @param identifier the quest identifier
     * @return a future containing the optional quest
     */
    @NotNull
    public CompletableFuture<Optional<Quest>> findByIdentifier(@NotNull final String identifier) {
        return findByKeyAsync("identifier", identifier);
    }

    /**
     * Finds all enabled quests asynchronously.
     *
     * @return a future containing the list of enabled quests
     */
    @NotNull
    public CompletableFuture<List<Quest>> findAllEnabled() {
        return findAllByAttributesAsync(Map.of("enabled", true));
    }

    /**
     * Finds a quest by identifier with tasks and rewards eagerly loaded.
     * <p>
     * Uses entity graphs to load collections without causing MultipleBagFetchException.
     * This loads the quest with its tasks and quest-level rewards in separate queries.
     *
     * @param identifier the quest identifier
     * @return a future containing the optional quest with collections loaded
     */
    @NotNull
    public CompletableFuture<Optional<Quest>> findByIdentifierWithCollections(@NotNull final String identifier) {
        return CompletableFuture.supplyAsync(() -> {
            final EntityManager em = entityManagerFactory.createEntityManager();
            try {
                // First: Load quest with tasks using entity graph
                final EntityGraph<Quest> taskGraph = em.createEntityGraph(Quest.class);
                taskGraph.addAttributeNodes("tasks");
                
                final Map<String, Object> taskHints = new HashMap<>();
                taskHints.put("jakarta.persistence.fetchgraph", taskGraph);
                
                final List<Quest> questsWithTasks = em.createQuery(
                    "SELECT q FROM Quest q WHERE q.identifier = :identifier",
                    Quest.class
                )
                .setParameter("identifier", identifier)
                .setHint("jakarta.persistence.fetchgraph", taskGraph)
                .getResultList();
                
                if (questsWithTasks.isEmpty()) {
                    return Optional.empty();
                }
                
                final Quest quest = questsWithTasks.get(0);
                
                // Second: Load quest rewards with BaseReward using JOIN FETCH
                em.createQuery(
                    "SELECT DISTINCT q FROM Quest q " +
                    "LEFT JOIN FETCH q.rewards r " +
                    "LEFT JOIN FETCH r.reward " +
                    "WHERE q.id = :id",
                    Quest.class
                )
                .setParameter("id", quest.getId())
                .getSingleResult();
                
                return Optional.of(quest);
            } finally {
                if (em.isOpen()) {
                    em.close();
                }
            }
        }, executor);
    }
}
