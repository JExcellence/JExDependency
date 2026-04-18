package de.jexcellence.economy.api;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Public API for the JExEconomy multi-currency economy system.
 *
 * <p>Third-party plugins should depend on this interface rather than
 * the internal {@code EconomyService}. Retrieve via Bukkit's
 * {@link org.bukkit.plugin.ServicesManager} or the convenience
 * accessor {@link JExEconomyAPI#get()}.
 *
 * <p>All mutating operations are asynchronous and return
 * {@link CompletableFuture}s. Never call {@code .join()} on the
 * main thread — use {@code .thenAccept()} or similar instead.
 *
 * <h2>Example usage:</h2>
 * <pre>{@code
 * EconomyProvider api = JExEconomyAPI.get();
 *
 * // Check balance
 * api.getBalance(player, "coins").thenAccept(balance ->
 *     player.sendMessage("You have " + balance + " coins"));
 *
 * // Deposit
 * api.deposit(player, "coins", 100.0, null, "quest reward")
 *     .thenAccept(result -> {
 *         if (result.isSuccess()) {
 *             player.sendMessage("Received 100 coins!");
 *         }
 *     });
 * }</pre>
 *
 * @author JExcellence
 * @since 3.0.0
 */
public interface EconomyProvider {

    // ── Currency queries ────────────────────────────────────────────────────────

    /**
     * Returns all registered currencies, keyed by identifier.
     *
     * @return unmodifiable map of currency identifier → snapshot
     */
    @NotNull Map<String, CurrencySnapshot> getCurrencies();

    /**
     * Looks up a currency by its unique identifier.
     *
     * @param identifier the currency identifier (e.g. "coins")
     * @return the currency snapshot, or empty if not found
     */
    @NotNull Optional<CurrencySnapshot> getCurrency(@NotNull String identifier);

    // ── Balance queries ─────────────────────────────────────────────────────────

    /**
     * Retrieves the current balance for a player and currency.
     *
     * @param player     the player
     * @param currencyId the currency identifier
     * @return future resolving to the balance, or {@code 0.0} if no account exists
     */
    @NotNull CompletableFuture<Double> getBalance(@NotNull OfflinePlayer player,
                                                  @NotNull String currencyId);

    /**
     * Checks whether a player has an account for the given currency.
     *
     * @param player     the player
     * @param currencyId the currency identifier
     * @return future resolving to {@code true} if an account exists
     */
    @NotNull CompletableFuture<Boolean> hasAccount(@NotNull OfflinePlayer player,
                                                   @NotNull String currencyId);

    /**
     * Checks whether a player has at least the specified amount.
     *
     * @param player     the player
     * @param currencyId the currency identifier
     * @param amount     the amount to check
     * @return future resolving to {@code true} if the balance is sufficient
     */
    @NotNull CompletableFuture<Boolean> has(@NotNull OfflinePlayer player,
                                            @NotNull String currencyId,
                                            double amount);

    // ── Transactions ────────────────────────────────────────────────────────────

    /**
     * Deposits an amount into a player's account.
     *
     * @param player     the target player
     * @param currencyId the currency identifier
     * @param amount     the amount to deposit (must be positive)
     * @param initiator  the player who initiated the deposit, or {@code null} for server-initiated
     * @param reason     optional human-readable reason
     * @return future resolving to the transaction result
     */
    @NotNull CompletableFuture<TransactionResult> deposit(@NotNull OfflinePlayer player,
                                                          @NotNull String currencyId,
                                                          double amount,
                                                          @Nullable Player initiator,
                                                          @Nullable String reason);

    /**
     * Withdraws an amount from a player's account.
     *
     * @param player     the target player
     * @param currencyId the currency identifier
     * @param amount     the amount to withdraw (must be positive)
     * @param initiator  the player who initiated the withdrawal, or {@code null} for server-initiated
     * @param reason     optional human-readable reason
     * @return future resolving to the transaction result
     */
    @NotNull CompletableFuture<TransactionResult> withdraw(@NotNull OfflinePlayer player,
                                                           @NotNull String currencyId,
                                                           double amount,
                                                           @Nullable Player initiator,
                                                           @Nullable String reason);

    /**
     * Transfers an amount from one player to another.
     *
     * <p>If the withdrawal succeeds but the deposit fails, the withdrawn
     * amount is automatically rolled back to the sender.
     *
     * @param sender     the sending player
     * @param recipient  the receiving player
     * @param currencyId the currency identifier
     * @param amount     the amount to transfer (must be positive)
     * @return future resolving to the transaction result
     */
    @NotNull CompletableFuture<TransactionResult> transfer(@NotNull Player sender,
                                                           @NotNull OfflinePlayer recipient,
                                                           @NotNull String currencyId,
                                                           double amount);

    // ── Leaderboard ─────────────────────────────────────────────────────────────

    /**
     * Returns the top accounts for a currency, ordered by balance descending.
     *
     * @param currencyId the currency identifier
     * @param limit      maximum number of results
     * @return future resolving to ordered list of account snapshots (highest balance first)
     */
    @NotNull CompletableFuture<List<AccountSnapshot>> getTopAccounts(@NotNull String currencyId,
                                                                     int limit);
}
