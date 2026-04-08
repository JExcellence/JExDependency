package com.raindropcentral.rdq.database.repository.quest;

import com.raindropcentral.rdq.database.entity.quest.QuestUser;
import de.jexcellence.hibernate.repository.CachedRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

/**
 * Repository for managing persistent {@link QuestUser} entities.
 * <p>
 * Uses {@link CachedRepository} built-in methods wherever possible.
 * The cache key is the {@code QuestUser} database ID ({@code Long}).
 * <p>
 * Task progress ({@code QuestUser.taskProgress}) is a lazy {@code List} — Hibernate
 * cannot JOIN FETCH two bags simultaneously. We therefore load {@code taskProgress}
 * in a single JOIN FETCH query and rely on the caller (or {@link QuestRepository})
 * to supply the already-cached {@code Quest.tasks} list separately.
 *
 * @author RaindropCentral
 * @version 2.0.0
 */
public class QuestUserRepository extends CachedRepository<QuestUser, Long, Long> {

    private final EntityManagerFactory entityManagerFactory;
    private final ExecutorService executor;

    /**
     * Constructs a new {@code QuestUserRepository}.
     *
     * @param executor             the executor service for async operations
     * @param entityManagerFactory the entity manager factory
     * @param entityClass          the entity class type
     * @param keyExtractor         function to extract the cache key from the entity
     */
    public QuestUserRepository(
            @NotNull final ExecutorService executor,
            @NotNull final EntityManagerFactory entityManagerFactory,
            @NotNull final Class<QuestUser> entityClass,
            @NotNull final Function<QuestUser, Long> keyExtractor
    ) {
        super(executor, entityManagerFactory, entityClass, keyExtractor);
        this.entityManagerFactory = entityManagerFactory;
        this.executor = executor;
    }

    /**
     * Finds all active (incomplete) quests for a player, with task progress eagerly loaded.
     * <p>
     * Uses a single JOIN FETCH for {@code taskProgress} only — {@code Quest.tasks} is
     * intentionally omitted here to avoid {@code MultipleBagFetchException}. Callers that
     * need task definitions should enrich results via {@link QuestRepository#findByIdentifier}.
     *
     * @param playerId the player's UUID
     * @return a future containing the list of active quest users
     */
    @NotNull
    public CompletableFuture<List<QuestUser>> findActiveByPlayer(@NotNull final UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            final var em = this.entityManagerFactory.createEntityManager();
            try {
                return em.createQuery(
                        "SELECT DISTINCT qu FROM QuestUser qu " +
                        "JOIN FETCH qu.quest " +
                        "LEFT JOIN FETCH qu.taskProgress " +
                        "WHERE qu.playerId = :playerId " +
                        "AND qu.completed = false " +
                        "ORDER BY qu.startedAt DESC",
                        QuestUser.class
                )
                .setParameter("playerId", playerId)
                .getResultList();
            } finally {
                if (em.isOpen()) em.close();
            }
        }, this.executor);
    }

    /**
     * Finds the active quest user for a specific player and quest, with task progress loaded.
     *
     * @param playerId        the player's UUID
     * @param questIdentifier the quest identifier
     * @return a future containing the optional quest user
     */
    @NotNull
    public CompletableFuture<Optional<QuestUser>> findActiveByPlayerAndQuest(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        return CompletableFuture.supplyAsync(() -> {
            final var em = this.entityManagerFactory.createEntityManager();
            try {
                return em.createQuery(
                        "SELECT qu FROM QuestUser qu " +
                        "JOIN FETCH qu.quest q " +
                        "LEFT JOIN FETCH qu.taskProgress " +
                        "WHERE qu.playerId = :playerId " +
                        "AND q.identifier = :questIdentifier " +
                        "AND qu.completed = false",
                        QuestUser.class
                )
                .setParameter("playerId", playerId)
                .setParameter("questIdentifier", questIdentifier)
                .getResultStream()
                .findFirst();
            } finally {
                if (em.isOpen()) em.close();
            }
        }, this.executor);
    }

    /**
     * Finds all completed quests for a player.
     *
     * @param playerId the player's UUID
     * @return a future containing the list of completed quest users
     */
    @NotNull
    public CompletableFuture<List<QuestUser>> findCompletedByPlayer(@NotNull final UUID playerId) {
        return findAllByAttributesAsync(Map.of("playerId", playerId, "completed", true));
    }

    /**
     * Counts the number of active quests for a player.
     *
     * @param playerId the player's UUID
     * @return a future containing the count of active quests
     */
    @NotNull
    public CompletableFuture<Integer> countActiveByPlayer(@NotNull final UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            final var em = this.entityManagerFactory.createEntityManager();
            try {
                return em.createQuery(
                        "SELECT COUNT(qu) FROM QuestUser qu " +
                        "WHERE qu.playerId = :playerId AND qu.completed = false",
                        Long.class
                )
                .setParameter("playerId", playerId)
                .getSingleResult()
                .intValue();
            } finally {
                if (em.isOpen()) em.close();
            }
        }, this.executor);
    }
}
