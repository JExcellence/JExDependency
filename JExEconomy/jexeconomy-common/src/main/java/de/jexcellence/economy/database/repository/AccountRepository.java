package de.jexcellence.economy.database.repository;

import de.jexcellence.economy.database.entity.Account;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.EconomyPlayer;
import de.jexcellence.jehibernate.repository.base.AbstractCrudRepository;
import jakarta.persistence.EntityManagerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Repository for {@link Account} entities.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public class AccountRepository extends AbstractCrudRepository<Account, Long> {

    /**
     * Creates an account repository.
     *
     * @param executor    the executor for async operations
     * @param emf         the entity manager factory
     * @param entityClass the entity class
     */
    public AccountRepository(@NotNull ExecutorService executor,
                             @NotNull EntityManagerFactory emf,
                             @NotNull Class<Account> entityClass) {
        super(executor, emf, entityClass);
    }

    /**
     * Finds an account by player and currency.
     *
     * @param player   the account owner
     * @param currency the currency
     * @return the account, or empty
     */
    public @NotNull Optional<Account> findByPlayerAndCurrency(
            @NotNull EconomyPlayer player,
            @NotNull Currency currency) {
        return query()
                .and("player", player)
                .and("currency", currency)
                .first();
    }

    /**
     * Finds an account by player and currency asynchronously.
     *
     * @param player   the account owner
     * @param currency the currency
     * @return future resolving to the account, or empty
     */
    public @NotNull CompletableFuture<Optional<Account>> findByPlayerAndCurrencyAsync(
            @NotNull EconomyPlayer player,
            @NotNull Currency currency) {
        return query()
                .and("player", player)
                .and("currency", currency)
                .firstAsync();
    }

    /**
     * Finds all accounts for a player.
     *
     * @param player the account owner
     * @return the list of accounts
     */
    public @NotNull List<Account> findByPlayer(@NotNull EconomyPlayer player) {
        return query().and("player", player).list();
    }

    /**
     * Finds all accounts for a player asynchronously.
     *
     * @param player the account owner
     * @return future resolving to the list of accounts
     */
    public @NotNull CompletableFuture<List<Account>> findByPlayerAsync(
            @NotNull EconomyPlayer player) {
        return query().and("player", player).listAsync();
    }

    /**
     * Finds top accounts by balance for a currency.
     *
     * @param currency the currency
     * @param limit    maximum number of results
     * @return ordered list of accounts (highest balance first)
     */
    public @NotNull List<Account> findTopByCurrency(@NotNull Currency currency, int limit) {
        return query()
                .and("currency", currency)
                .orderByDesc("balance")
                .limit(limit)
                .list();
    }

    /**
     * Finds top accounts by balance for a currency asynchronously.
     *
     * @param currency the currency
     * @param limit    maximum number of results
     * @return future resolving to ordered list of accounts
     */
    public @NotNull CompletableFuture<List<Account>> findTopByCurrencyAsync(
            @NotNull Currency currency, int limit) {
        return query()
                .and("currency", currency)
                .orderByDesc("balance")
                .limit(limit)
                .listAsync();
    }

    /**
     * Counts accounts for a currency.
     *
     * @param currency the currency
     * @return the number of accounts
     */
    public long countByCurrency(@NotNull Currency currency) {
        return query().and("currency", currency).count();
    }
}
