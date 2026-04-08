package com.raindropcentral.rdq.database.repository.quest;

import com.raindropcentral.rdq.database.entity.quest.PlayerQuestProgress;
import com.raindropcentral.rdq.database.entity.quest.Quest;
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
 * Repository for managing persistent {@link PlayerQuestProgress} entities.
 * <p>
 * This repository provides cached access to player quest progress records, which track
 * active quest state for online players. Progress is loaded on player join and saved on
 * player quit, with periodic auto-save for crash protection.
 *
 * <p>
 * This repository follows the CachedRepository pattern for optimal performance with
 * frequently accessed player progress data. Use the cache layer (PlayerQuestProgressCache)
 * for instant access to online player progress.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class PlayerQuestProgressRepository extends CachedRepository<PlayerQuestProgress, Long, UUID> {
    
    private final EntityManagerFactory entityManagerFactory;
    private final ExecutorService executor;
    
    /**
     * Constructs a new {@code PlayerQuestProgressRepository} for managing {@link PlayerQuestProgress} entities.
     *
     * @param executor             the {@link ExecutorService} used for executing repository operations asynchronously
     * @param entityManagerFactory the {@link EntityManagerFactory} used to create and manage entity managers for persistence operations
     * @param entityClass          the entity class
     * @param keyExtractor         function to extract the cache key from the entity
     */
    public PlayerQuestProgressRepository(
        final @NotNull ExecutorService executor,
        final @NotNull EntityManagerFactory entityManagerFactory,
        @NotNull Class<PlayerQuestProgress> entityClass,
        @NotNull Function<PlayerQuestProgress, UUID> keyExtractor
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
     * Finds all quest progress records for a specific player.
     * <p>
     * This method is used to load all active quests when a player joins the server.
     * Results are ordered by start time (most recent first).
     *
     * @param playerId the player's UUID
     * @return CompletableFuture containing the list of progress records
     */
    @NotNull
    public CompletableFuture<List<PlayerQuestProgress>> findByPlayer(@NotNull final UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                return em.createQuery(
                    "SELECT p FROM PlayerQuestProgress p WHERE p.playerId = :playerId ORDER BY p.startedAt DESC",
                    PlayerQuestProgress.class
                )
                .setParameter("playerId", playerId)
                .getResultList();
            } finally {
                em.close();
            }
        }, executor);
    }
    
    /**
     * Finds a player's progress for a specific quest.
     * <p>
     * This method is used to check if a player has already started a quest
     * and to retrieve their current progress.
     *
     * @param playerId the player's UUID
     * @param questId  the quest ID
     * @return CompletableFuture containing the optional progress record
     */
    @NotNull
    public CompletableFuture<Optional<PlayerQuestProgress>> findByPlayerAndQuest(
        @NotNull final UUID playerId,
        @NotNull final Long questId
    ) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                var results = em.createQuery(
                    "SELECT p FROM PlayerQuestProgress p WHERE p.playerId = :playerId AND p.quest.id = :questId",
                    PlayerQuestProgress.class
                )
                .setParameter("playerId", playerId)
                .setParameter("questId", questId)
                .getResultList();
                
                return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
            } finally {
                em.close();
            }
        }, executor);
    }
    
    /**
     * Finds all active (not completed) quest progress records for a player.
     * <p>
     * This method is used to display a player's active quest list and to enforce
     * the maximum active quest limit.
     *
     * @param playerId the player's UUID
     * @return CompletableFuture containing the list of active progress records
     */
    @NotNull
    public CompletableFuture<List<PlayerQuestProgress>> findActiveByPlayer(@NotNull final UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                return em.createQuery(
                    "SELECT p FROM PlayerQuestProgress p " +
                    "WHERE p.playerId = :playerId AND p.completed = false " +
                    "ORDER BY p.startedAt DESC",
                    PlayerQuestProgress.class
                )
                .setParameter("playerId", playerId)
                .getResultList();
            } finally {
                em.close();
            }
        }, executor);
    }
    
    /**
     * Finds a player's progress with all task progress eagerly loaded.
     * <p>
     * This method uses JOIN FETCH to load task progress in a single query,
     * avoiding N+1 query problems when displaying quest details.
     *
     * @param playerId the player's UUID
     * @param questId  the quest ID
     * @return CompletableFuture containing the optional progress record with eagerly loaded tasks
     */
    @NotNull
    public CompletableFuture<Optional<PlayerQuestProgress>> findByPlayerAndQuestWithTasks(
        @NotNull final UUID playerId,
        @NotNull final Long questId
    ) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                var results = em.createQuery(
                    "SELECT DISTINCT p FROM PlayerQuestProgress p " +
                    "LEFT JOIN FETCH p.taskProgress " +
                    "WHERE p.playerId = :playerId AND p.quest.id = :questId",
                    PlayerQuestProgress.class
                )
                .setParameter("playerId", playerId)
                .setParameter("questId", questId)
                .getResultList();
                
                return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
            } finally {
                em.close();
            }
        }, executor);
    }
    
    /**
     * Finds all active quest progress for a player with task progress eagerly loaded.
     * <p>
     * This method is used when loading all player progress on join to minimize database queries.
     *
     * @param playerId the player's UUID
     * @return CompletableFuture containing the list of progress records with eagerly loaded tasks
     */
    @NotNull
    public CompletableFuture<List<PlayerQuestProgress>> findActiveByPlayerWithTasks(@NotNull final UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                return em.createQuery(
                    "SELECT DISTINCT p FROM PlayerQuestProgress p " +
                    "LEFT JOIN FETCH p.taskProgress " +
                    "WHERE p.playerId = :playerId AND p.completed = false " +
                    "ORDER BY p.startedAt DESC",
                    PlayerQuestProgress.class
                )
                .setParameter("playerId", playerId)
                .getResultList();
            } finally {
                em.close();
            }
        }, executor);
    }
    
    /**
     * Counts the number of active quests for a player.
     * <p>
     * This method is used to enforce the maximum active quest limit.
     *
     * @param playerId the player's UUID
     * @return CompletableFuture containing the count of active quests
     */
    @NotNull
    public CompletableFuture<Long> countActiveByPlayer(@NotNull final UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                return em.createQuery(
                    "SELECT COUNT(p) FROM PlayerQuestProgress p " +
                    "WHERE p.playerId = :playerId AND p.completed = false",
                    Long.class
                )
                .setParameter("playerId", playerId)
                .getSingleResult();
            } finally {
                em.close();
            }
        }, executor);
    }
    
    /**
     * Finds all progress records for a specific quest.
     * <p>
     * This method is used for quest statistics and to see how many players
     * are currently working on a quest.
     *
     * @param quest the quest
     * @return CompletableFuture containing the list of progress records
     */
    @NotNull
    public CompletableFuture<List<PlayerQuestProgress>> findByQuest(@NotNull final Quest quest) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                return em.createQuery(
                    "SELECT p FROM PlayerQuestProgress p WHERE p.quest = :quest ORDER BY p.startedAt DESC",
                    PlayerQuestProgress.class
                )
                .setParameter("quest", quest)
                .getResultList();
            } finally {
                em.close();
            }
        }, executor);
    }
    
    /**
     * Finds all active progress records for a specific quest.
     * <p>
     * This method is used to see how many players are currently working on a quest.
     *
     * @param quest the quest
     * @return CompletableFuture containing the list of active progress records
     */
    @NotNull
    public CompletableFuture<List<PlayerQuestProgress>> findActiveByQuest(@NotNull final Quest quest) {
        return CompletableFuture.supplyAsync(() -> {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                return em.createQuery(
                    "SELECT p FROM PlayerQuestProgress p " +
                    "WHERE p.quest = :quest AND p.completed = false " +
                    "ORDER BY p.startedAt DESC",
                    PlayerQuestProgress.class
                )
                .setParameter("quest", quest)
                .getResultList();
            } finally {
                em.close();
            }
        }, executor);
    }
    
    /**
     * Deletes all progress records for a specific player.
     * <p>
     * This method is used for data cleanup or player reset operations.
     * Use with caution as this permanently deletes all quest progress.
     *
     * @param playerId the player's UUID
     * @return CompletableFuture that completes when deletion is done
     */
    @NotNull
    public CompletableFuture<Void> deleteByPlayer(@NotNull final UUID playerId) {
        return CompletableFuture.runAsync(() -> {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                em.getTransaction().begin();
                em.createQuery("DELETE FROM PlayerQuestProgress p WHERE p.playerId = :playerId")
                    .setParameter("playerId", playerId)
                    .executeUpdate();
                em.getTransaction().commit();
            } catch (Exception e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            } finally {
                em.close();
            }
        }, executor);
    }
}
