package de.jexcellence.economy.service;

import de.jexcellence.economy.database.entity.TransactionLog;
import de.jexcellence.economy.database.repository.TransactionLogRepository;
import de.jexcellence.jexplatform.logging.JExLogger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Persists {@link TransactionLog} entities asynchronously and provides history queries.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public class TransactionLogger {

    private final TransactionLogRepository repository;
    private final JExLogger logger;

    /**
     * Creates a transaction logger.
     *
     * @param repository the transaction log repository
     * @param logger     the logger for error reporting
     */
    public TransactionLogger(@NotNull TransactionLogRepository repository,
                             @NotNull JExLogger logger) {
        this.repository = repository;
        this.logger = logger;
    }

    /**
     * Persists a transaction log entry asynchronously. Errors are logged but never propagated.
     *
     * @param entry the log entry to persist
     */
    public void log(@NotNull TransactionLog entry) {
        repository.createAsync(entry).exceptionally(ex -> {
            logger.error("Failed to persist transaction log: {}", ex.getMessage());
            return null;
        });
    }

    /**
     * Retrieves recent transaction history for a player.
     *
     * @param playerUuid the player's UUID
     * @param limit      maximum number of entries to return
     * @return future resolving to an ordered list (newest first)
     */
    public @NotNull CompletableFuture<List<TransactionLog>> getPlayerHistory(
            @NotNull UUID playerUuid, int limit) {
        return repository.findByPlayerUuidAsync(playerUuid, limit);
    }

    /**
     * Retrieves recent transaction history for a currency.
     *
     * @param currencyId the currency's database ID
     * @param limit      maximum number of entries to return
     * @return future resolving to an ordered list (newest first)
     */
    public @NotNull CompletableFuture<List<TransactionLog>> getCurrencyHistory(
            long currencyId, int limit) {
        return repository.findByCurrencyAsync(currencyId, limit);
    }
}
