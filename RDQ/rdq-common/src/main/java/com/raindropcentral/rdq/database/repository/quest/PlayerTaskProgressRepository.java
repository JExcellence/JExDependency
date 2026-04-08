package com.raindropcentral.rdq.database.repository.quest;

import com.raindropcentral.rdq.database.entity.quest.PlayerQuestProgress;
import com.raindropcentral.rdq.database.entity.quest.PlayerTaskProgress;
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
 * Repository for managing persistent {@link PlayerTaskProgress} entities.
 * <p>
 * This repository provides access to player task progress records, which track
 * progress on individual quest tasks. Task progress is typically loaded as part
 * of quest progress and is not cached separately.
 *
 * <p>
 * Most operations on task progress should go through the PlayerQuestProgress entity
 * and its bidirectional relationship. This repository provides additional query methods
 * for specific use cases like statistics and bulk operations.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class PlayerTaskProgressRepository extends CachedRepository<PlayerTaskProgress, Long, Long> {
    
    private final EntityManagerFactory entityManagerFactory;
    private final ExecutorService executor;
    
    /**
     * Constructs a new {@code PlayerTaskProgressRepository} for managing {@link PlayerTaskProgress} entities.
     *
     * @param executor             the {@link ExecutorService} used for executing repository operations asynchronously
     * @param entityManagerFactory the {@link EntityManagerFactory} used to create and manage entity managers for persistence operations
     * @param entityClass          the entity class
     * @param keyExtractor         function to extract the cache key from the entity
     */
    public PlayerTaskProgressRepository(
        final @NotNull ExecutorService executor,
        final @NotNull EntityManagerFactory entityManagerFactory,
        @NotNull Class<PlayerTaskProgress> entityClass,
        @NotNull Function<PlayerTaskProgress, Long> keyExtractor
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
     * Finds all task progress records for a specific quest progress.
     * <p>
     * This method is used when task progress needs to be loaded separately
     * from the quest progress entity.
     *
     * @param questProgress the quest progress
     * @return CompletableFuture containing the list of task progress records
     */
    @NotNull
    public CompletableFuture<List<PlayerTaskProgress>> findByQuestProgress(@NotNull final PlayerQuestProgress questProgress) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                return em.createQuery(
                    "SELECT t FROM PlayerTaskProgress t WHERE t.questProgress = :questProgress ORDER BY t.task.orderIndex",
                    PlayerTaskProgress.class
                )
                .setParameter("questProgress", questProgress)
                .getResultList();
            } finally {
                em.close();
            }
        }, executor);
    }
    
    /**
     * Finds a specific task progress record.
     * <p>
     * This method is used to look up progress for a specific task within a quest.
     *
     * @param questProgress the quest progress
     * @param task          the quest task
     * @return CompletableFuture containing the optional task progress record
     */
    @NotNull
    public CompletableFuture<Optional<PlayerTaskProgress>> findByQuestProgressAndTask(
        @NotNull final PlayerQuestProgress questProgress,
        @NotNull final QuestTask task
    ) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                var results = em.createQuery(
                    "SELECT t FROM PlayerTaskProgress t WHERE t.questProgress = :questProgress AND t.task = :task",
                    PlayerTaskProgress.class
                )
                .setParameter("questProgress", questProgress)
                .setParameter("task", task)
                .getResultList();
                
                return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
            } finally {
                em.close();
            }
        }, executor);
    }
    
    /**
     * Finds all completed task progress records for a quest progress.
     * <p>
     * This method is used to check which tasks have been completed.
     *
     * @param questProgress the quest progress
     * @return CompletableFuture containing the list of completed task progress records
     */
    @NotNull
    public CompletableFuture<List<PlayerTaskProgress>> findCompletedByQuestProgress(@NotNull final PlayerQuestProgress questProgress) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                return em.createQuery(
                    "SELECT t FROM PlayerTaskProgress t " +
                    "WHERE t.questProgress = :questProgress AND t.completed = true " +
                    "ORDER BY t.completedAt DESC",
                    PlayerTaskProgress.class
                )
                .setParameter("questProgress", questProgress)
                .getResultList();
            } finally {
                em.close();
            }
        }, executor);
    }
    
    /**
     * Finds all incomplete task progress records for a quest progress.
     * <p>
     * This method is used to display remaining tasks to complete.
     *
     * @param questProgress the quest progress
     * @return CompletableFuture containing the list of incomplete task progress records
     */
    @NotNull
    public CompletableFuture<List<PlayerTaskProgress>> findIncompleteByQuestProgress(@NotNull final PlayerQuestProgress questProgress) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                return em.createQuery(
                    "SELECT t FROM PlayerTaskProgress t " +
                    "WHERE t.questProgress = :questProgress AND t.completed = false " +
                    "ORDER BY t.task.orderIndex",
                    PlayerTaskProgress.class
                )
                .setParameter("questProgress", questProgress)
                .getResultList();
            } finally {
                em.close();
            }
        }, executor);
    }
    
    /**
     * Finds task progress with requirement progress eagerly loaded.
     * <p>
     * This method uses JOIN FETCH to load requirement progress in a single query,
     * avoiding N+1 query problems when displaying task details.
     *
     * @param questProgress the quest progress
     * @param task          the quest task
     * @return CompletableFuture containing the optional task progress with eagerly loaded requirements
     */
    @NotNull
    public CompletableFuture<Optional<PlayerTaskProgress>> findByQuestProgressAndTaskWithRequirements(
        @NotNull final PlayerQuestProgress questProgress,
        @NotNull final QuestTask task
    ) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                var results = em.createQuery(
                    "SELECT DISTINCT t FROM PlayerTaskProgress t " +
                    "LEFT JOIN FETCH t.requirementProgress " +
                    "WHERE t.questProgress = :questProgress AND t.task = :task",
                    PlayerTaskProgress.class
                )
                .setParameter("questProgress", questProgress)
                .setParameter("task", task)
                .getResultList();
                
                return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
            } finally {
                em.close();
            }
        }, executor);
    }
    
    /**
     * Finds all task progress for a quest progress with requirement progress eagerly loaded.
     * <p>
     * This method is used when displaying full quest details with all task requirements.
     *
     * @param questProgress the quest progress
     * @return CompletableFuture containing the list of task progress with eagerly loaded requirements
     */
    @NotNull
    public CompletableFuture<List<PlayerTaskProgress>> findByQuestProgressWithRequirements(@NotNull final PlayerQuestProgress questProgress) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                return em.createQuery(
                    "SELECT DISTINCT t FROM PlayerTaskProgress t " +
                    "LEFT JOIN FETCH t.requirementProgress " +
                    "WHERE t.questProgress = :questProgress " +
                    "ORDER BY t.task.orderIndex",
                    PlayerTaskProgress.class
                )
                .setParameter("questProgress", questProgress)
                .getResultList();
            } finally {
                em.close();
            }
        }, executor);
    }
    
    /**
     * Counts the number of completed tasks for a quest progress.
     * <p>
     * This method is used for progress calculations and statistics.
     *
     * @param questProgress the quest progress
     * @return CompletableFuture containing the count of completed tasks
     */
    @NotNull
    public CompletableFuture<Long> countCompletedByQuestProgress(@NotNull final PlayerQuestProgress questProgress) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                return em.createQuery(
                    "SELECT COUNT(t) FROM PlayerTaskProgress t " +
                    "WHERE t.questProgress = :questProgress AND t.completed = true",
                    Long.class
                )
                .setParameter("questProgress", questProgress)
                .getSingleResult();
            } finally {
                em.close();
            }
        }, executor);
    }
    
    /**
     * Finds all task progress records for a specific task across all players.
     * <p>
     * This method is used for task statistics and to see how many players
     * have completed a specific task.
     *
     * @param task the quest task
     * @return CompletableFuture containing the list of task progress records
     */
    @NotNull
    public CompletableFuture<List<PlayerTaskProgress>> findByTask(@NotNull final QuestTask task) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                return em.createQuery(
                    "SELECT t FROM PlayerTaskProgress t WHERE t.task = :task ORDER BY t.completedAt DESC",
                    PlayerTaskProgress.class
                )
                .setParameter("task", task)
                .getResultList();
            } finally {
                em.close();
            }
        }, executor);
    }
    
    /**
     * Counts how many players have completed a specific task.
     * <p>
     * This method is used for task statistics and leaderboards.
     *
     * @param task the quest task
     * @return CompletableFuture containing the count of completions
     */
    @NotNull
    public CompletableFuture<Long> countCompletedByTask(@NotNull final QuestTask task) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                return em.createQuery(
                    "SELECT COUNT(t) FROM PlayerTaskProgress t WHERE t.task = :task AND t.completed = true",
                    Long.class
                )
                .setParameter("task", task)
                .getSingleResult();
            } finally {
                em.close();
            }
        }, executor);
    }
}
