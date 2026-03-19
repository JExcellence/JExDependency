package com.raindropcentral.rdq.database.repository.quest;

import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.entity.quest.QuestTask;
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
 * Repository for managing persistent {@link QuestTask} entities.
 * <p>
 * This repository provides cached access to quest tasks with methods for finding by identifier,
 * quest, and optional status. It follows the CachedRepository pattern for optimal performance
 * with frequently accessed task definitions.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class QuestTaskRepository extends CachedRepository<QuestTask, Long, String> {
    
    private final EntityManagerFactory entityManagerFactory;
    private final ExecutorService executor;
    
    /**
     * Constructs a new {@code QuestTaskRepository} for managing {@link QuestTask} entities.
     *
     * @param executor             the {@link ExecutorService} used for executing repository operations asynchronously
     * @param entityManagerFactory the {@link EntityManagerFactory} used to create and manage entity managers for persistence operations
     * @param entityClass          the entity class
     * @param keyExtractor         function to extract the cache key from the entity
     */
    public QuestTaskRepository(
        final @NotNull ExecutorService executor,
        final @NotNull EntityManagerFactory entityManagerFactory,
        @NotNull Class<QuestTask> entityClass,
        @NotNull Function<QuestTask, String> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
        this.entityManagerFactory = entityManagerFactory;
        this.executor = executor;
    }

    /**
     * Finds a quest task by its quest and unique identifier.
     * <p>
     * This method is used by the quest system to look up tasks within a specific quest.
     * </p>
     *
     * @param quest      the parent quest
     * @param identifier the task identifier
     * @return Optional containing the quest task if found
     */
    @NotNull
    public Optional<QuestTask> findByQuestAndIdentifier(@NotNull final Quest quest, @NotNull final String identifier) {
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            var results = em.createQuery(
                "SELECT t FROM QuestTask t WHERE t.quest = :quest AND t.identifier = :identifier",
                QuestTask.class
            )
            .setParameter("quest", quest)
            .setParameter("identifier", identifier)
            .getResultList();
            
            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } finally {
            em.close();
        }
    }
    
    /**
     * Finds a quest task by its quest and unique identifier asynchronously.
     * <p>
     * This method is the async version of {@link #findByQuestAndIdentifier(Quest, String)}.
     * </p>
     *
     * @param quest      the parent quest
     * @param identifier the task identifier
     * @return CompletableFuture containing the optional quest task
     */
    @NotNull
    public CompletableFuture<Optional<QuestTask>> findByQuestAndIdentifierAsync(
        @NotNull final Quest quest,
        @NotNull final String identifier
    ) {
        return CompletableFuture.supplyAsync(() -> findByQuestAndIdentifier(quest, identifier), executor);
    }
    
    /**
     * Finds all tasks belonging to a specific quest.
     * <p>
     * This method is used to retrieve all tasks for a quest, ordered by their sort order.
     * </p>
     *
     * @param quest the parent quest
     * @return CompletableFuture containing the list of tasks in the quest
     */
    @NotNull
    public CompletableFuture<List<QuestTask>> findByQuest(@NotNull final Quest quest) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                return em.createQuery(
                    "SELECT t FROM QuestTask t WHERE t.quest = :quest ORDER BY t.sortOrder",
                    QuestTask.class
                )
                .setParameter("quest", quest)
                .getResultList();
            } finally {
                em.close();
            }
        }, executor);
    }
    
    /**
     * Finds all optional tasks for a specific quest.
     * <p>
     * This method is used to filter tasks that are not required for quest completion.
     * </p>
     *
     * @param quest the parent quest
     * @return CompletableFuture containing the list of optional tasks
     */
    @NotNull
    public CompletableFuture<List<QuestTask>> findOptionalByQuest(@NotNull final Quest quest) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                return em.createQuery(
                    "SELECT t FROM QuestTask t WHERE t.quest = :quest AND t.optional = true ORDER BY t.sortOrder",
                    QuestTask.class
                )
                .setParameter("quest", quest)
                .getResultList();
            } finally {
                em.close();
            }
        }, executor);
    }
    
    /**
     * Finds all required (non-optional) tasks for a specific quest.
     * <p>
     * This method is used to retrieve tasks that must be completed for quest completion.
     * </p>
     *
     * @param quest the parent quest
     * @return CompletableFuture containing the list of required tasks
     */
    @NotNull
    public CompletableFuture<List<QuestTask>> findRequiredByQuest(@NotNull final Quest quest) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                return em.createQuery(
                    "SELECT t FROM QuestTask t WHERE t.quest = :quest AND t.optional = false ORDER BY t.sortOrder",
                    QuestTask.class
                )
                .setParameter("quest", quest)
                .getResultList();
            } finally {
                em.close();
            }
        }, executor);
    }
    
    /**
     * Finds all tasks for a quest with full details eagerly loaded.
     * <p>
     * This method uses JOIN FETCH to load requirements and rewards in a single query,
     * avoiding N+1 query problems when displaying task details.
     * </p>
     *
     * @param quest the parent quest
     * @return CompletableFuture containing the list of tasks with eagerly loaded details
     */
    @NotNull
    public CompletableFuture<List<QuestTask>> findByQuestWithDetails(@NotNull final Quest quest) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                return em.createQuery(
                    "SELECT DISTINCT t FROM QuestTask t " +
                    "LEFT JOIN FETCH t.requirements " +
                    "LEFT JOIN FETCH t.rewards " +
                    "WHERE t.quest = :quest " +
                    "ORDER BY t.sortOrder",
                    QuestTask.class
                )
                .setParameter("quest", quest)
                .getResultList();
            } finally {
                em.close();
            }
        }, executor);
    }
}
