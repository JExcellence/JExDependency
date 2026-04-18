package de.jexcellence.economy.database.repository;

import de.jexcellence.economy.database.entity.TransactionLog;
import de.jexcellence.jehibernate.repository.base.AbstractCrudRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for {@link TransactionLog} entities.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public class TransactionLogRepository extends AbstractCrudRepository<TransactionLog, Long> {

    /**
     * Creates a transaction log repository.
     *
     * @param executor    the executor for async operations
     * @param emf         the entity manager factory
     * @param entityClass the entity class
     */
    public TransactionLogRepository(@NotNull ExecutorService executor,
                                    @NotNull EntityManagerFactory emf,
                                    @NotNull Class<TransactionLog> entityClass) {
        super(executor, emf, entityClass);
    }

    /**
     * Finds recent transaction logs for a player.
     *
     * @param playerUuid the player's UUID
     * @param limit      maximum results
     * @return ordered list (newest first)
     */
    public @NotNull List<TransactionLog> findByPlayerUuid(@NotNull UUID playerUuid, int limit) {
        return query()
                .and("playerUuid", playerUuid)
                .orderByDesc("timestamp")
                .limit(limit)
                .list();
    }

    /**
     * Finds recent transaction logs for a player asynchronously.
     *
     * @param playerUuid the player's UUID
     * @param limit      maximum results
     * @return future resolving to ordered list
     */
    public @NotNull CompletableFuture<List<TransactionLog>> findByPlayerUuidAsync(
            @NotNull UUID playerUuid, int limit) {
        return query()
                .and("playerUuid", playerUuid)
                .orderByDesc("timestamp")
                .limit(limit)
                .listAsync();
    }

    /**
     * Finds recent transaction logs whose cached {@code playerName} matches
     * the supplied value case-insensitively. Useful for admin views where the
     * UUID is unknown.
     *
     * @param playerName the player's current Minecraft name
     * @param limit      maximum results
     * @return future resolving to ordered list
     */
    public @NotNull CompletableFuture<List<TransactionLog>> findByPlayerNameAsync(
            @NotNull String playerName, int limit) {
        return CompletableFuture.supplyAsync(() -> withReadOnly(ctx ->
                ctx.getEntityManager()
                        .createQuery(
                                "SELECT t FROM TransactionLog t " +
                                "WHERE lower(t.playerName) = lower(:name) " +
                                "ORDER BY t.timestamp DESC",
                                TransactionLog.class)
                        .setParameter("name", playerName)
                        .setMaxResults(limit)
                        .getResultList()
        ));
    }

    /**
     * Finds recent transaction logs for a currency.
     *
     * @param currencyId the currency database ID
     * @param limit      maximum results
     * @return ordered list (newest first)
     */
    public @NotNull List<TransactionLog> findByCurrency(long currencyId, int limit) {
        return query()
                .and("currency.id", currencyId)
                .orderByDesc("timestamp")
                .limit(limit)
                .list();
    }

    /**
     * Finds recent transaction logs for a currency asynchronously.
     *
     * @param currencyId the currency database ID
     * @param limit      maximum results
     * @return future resolving to ordered list
     */
    public @NotNull CompletableFuture<List<TransactionLog>> findByCurrencyAsync(
            long currencyId, int limit) {
        return query()
                .and("currency.id", currencyId)
                .orderByDesc("timestamp")
                .limit(limit)
                .listAsync();
    }

    /**
     * Deletes all transaction logs strictly older than the given cutoff timestamp.
     *
     * <p>Issues a single JPQL {@code DELETE} and returns the number of rows removed.
     * Intended for admin purge operations; callers should clamp the cutoff to a
     * safe retention window (e.g. 30+ days).
     *
     * @param cutoff any timestamp earlier than this is purged
     * @return future resolving to the number of deleted rows
     */
    public @NotNull CompletableFuture<Integer> deleteOlderThanAsync(@NotNull Instant cutoff) {
        return CompletableFuture.supplyAsync(() -> {
            var count = new int[1];
            withSessionVoid(ctx ->
                    count[0] = ctx.getEntityManager()
                            .createQuery("DELETE FROM TransactionLog t WHERE t.timestamp < :cutoff")
                            .setParameter("cutoff", cutoff)
                            .executeUpdate());
            return count[0];
        });
    }
}
