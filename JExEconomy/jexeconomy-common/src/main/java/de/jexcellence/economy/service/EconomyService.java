package de.jexcellence.economy.service;

import de.jexcellence.economy.api.AccountSnapshot;
import de.jexcellence.economy.api.ChangeType;
import de.jexcellence.economy.api.CurrencySnapshot;
import de.jexcellence.economy.api.EconomyProvider;
import de.jexcellence.economy.api.TransactionResult;
import de.jexcellence.economy.api.event.BalanceChangeEvent;
import de.jexcellence.economy.api.event.BalanceChangedEvent;
import de.jexcellence.economy.api.event.CurrencyCreateEvent;
import de.jexcellence.economy.api.event.CurrencyCreatedEvent;
import de.jexcellence.economy.api.event.CurrencyDeleteEvent;
import de.jexcellence.economy.api.event.CurrencyDeletedEvent;
import de.jexcellence.economy.database.entity.Account;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.EconomyPlayer;
import de.jexcellence.economy.database.entity.TransactionLog;
import de.jexcellence.economy.database.repository.AccountRepository;
import de.jexcellence.economy.database.repository.CurrencyRepository;
import de.jexcellence.economy.database.repository.PlayerRepository;
import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.jexplatform.scheduler.PlatformScheduler;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central economy implementation providing balance operations, currency management,
 * and transactional guarantees with event-driven hooks.
 *
 * <p>Implements {@link EconomyProvider} so third-party plugins can interact with
 * the economy using only the API module. Registered as a Bukkit service under
 * {@code EconomyProvider.class}.
 *
 * <p>All mutating operations run asynchronously; Bukkit events are dispatched
 * on the main thread via {@link PlatformScheduler#runSync(Runnable)}.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public class EconomyService implements EconomyProvider {

    private final CurrencyRepository currencyRepo;
    private final PlayerRepository playerRepo;
    private final AccountRepository accountRepo;
    private final TransactionLogger txLogger;
    private final JExLogger logger;
    private final PlatformScheduler scheduler;
    private final JavaPlugin plugin;

    private final Map<String, Currency> currencyCache = new ConcurrentHashMap<>();

    /**
     * Creates the economy service.
     *
     * @param currencyRepo currency repository
     * @param playerRepo   player repository
     * @param accountRepo  account repository
     * @param txLogger     transaction logger
     * @param logger       platform logger
     * @param scheduler    platform scheduler for main-thread event dispatch
     * @param plugin       owning plugin instance
     */
    public EconomyService(@NotNull CurrencyRepository currencyRepo,
                          @NotNull PlayerRepository playerRepo,
                          @NotNull AccountRepository accountRepo,
                          @NotNull TransactionLogger txLogger,
                          @NotNull JExLogger logger,
                          @NotNull PlatformScheduler scheduler,
                          @NotNull JavaPlugin plugin) {
        this.currencyRepo = currencyRepo;
        this.playerRepo = playerRepo;
        this.accountRepo = accountRepo;
        this.txLogger = txLogger;
        this.logger = logger;
        this.scheduler = scheduler;
        this.plugin = plugin;
    }

    // ── Cache management ────────────────────────────────────────────────────────

    /**
     * Loads all currencies from the database into the local cache.
     *
     * @return future that completes when the cache is populated
     */
    public @NotNull CompletableFuture<Void> loadCurrencies() {
        return currencyRepo.findAllAsync().thenAccept(currencies -> {
            currencyCache.clear();
            for (var c : currencies) {
                currencyCache.put(c.getIdentifier(), c);
            }
            logger.info("Loaded {} currencies into cache", currencyCache.size());
        });
    }

    /**
     * Seeds a default {@code coins} currency on first startup if the database is empty.
     *
     * <p>This guarantees that operators see a working plugin immediately after installation
     * without having to run a create command first. The seeded currency uses generic defaults
     * (identifier {@code coins}, symbol {@code $}, icon {@code GOLD_INGOT}) and can be freely
     * edited or deleted via {@code /currencies}.
     *
     * <p>Persists directly through the repository, bypassing the event system. This is
     * intentional — events cannot safely dispatch during {@code onEnable()} because the main
     * thread is blocked waiting for the seed to finish, which would deadlock
     * {@link #fireEventSync(Event)}.
     */
    public void seedDefaultCurrencyIfEmpty() {
        if (!currencyCache.isEmpty()) return;
        try {
            var defaultCurrency = new Currency("coins", "$", "", "", "GOLD_INGOT");
            var saved = currencyRepo.create(defaultCurrency);
            currencyCache.put(saved.getIdentifier(), saved);
            logger.info("Seeded default currency 'coins' (first startup)");
        } catch (Exception e) {
            logger.error("Failed to seed default currency", e);
        }
    }

    /**
     * Returns an unmodifiable view of all cached currency entities keyed by identifier.
     *
     * <p>Internal use only — third-party plugins should use {@link #getCurrencies()}.
     *
     * @return unmodifiable currency entity map
     */
    public @NotNull Map<String, Currency> getAllCurrencies() {
        return Collections.unmodifiableMap(currencyCache);
    }

    /**
     * Looks up a currency entity by identifier from the cache.
     *
     * <p>Internal use only — third-party plugins should use {@link #getCurrency(String)}.
     *
     * @param identifier the currency identifier
     * @return the currency entity, or empty if not cached
     */
    public @NotNull Optional<Currency> findCurrency(@NotNull String identifier) {
        return Optional.ofNullable(currencyCache.get(identifier));
    }

    // ── EconomyProvider implementation ──────────────────────────────────────────

    @Override
    public @NotNull Map<String, CurrencySnapshot> getCurrencies() {
        var result = new LinkedHashMap<String, CurrencySnapshot>();
        for (var entry : currencyCache.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toSnapshot());
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public @NotNull Optional<CurrencySnapshot> getCurrency(@NotNull String identifier) {
        return findCurrency(identifier).map(Currency::toSnapshot);
    }

    @Override
    public @NotNull CompletableFuture<Double> getBalance(@NotNull OfflinePlayer player,
                                                         @NotNull String currencyId) {
        var currency = currencyCache.get(currencyId);
        if (currency == null) {
            return CompletableFuture.completedFuture(0.0);
        }
        return getBalance(player, currency);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> hasAccount(@NotNull OfflinePlayer player,
                                                          @NotNull String currencyId) {
        var currency = currencyCache.get(currencyId);
        if (currency == null) {
            return CompletableFuture.completedFuture(false);
        }
        return getAccount(player, currency)
                .thenApply(Optional::isPresent);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> has(@NotNull OfflinePlayer player,
                                                    @NotNull String currencyId,
                                                    double amount) {
        return getBalance(player, currencyId)
                .thenApply(balance -> balance >= amount);
    }

    @Override
    public @NotNull CompletableFuture<TransactionResult> deposit(@NotNull OfflinePlayer player,
                                                                  @NotNull String currencyId,
                                                                  double amount,
                                                                  @Nullable Player initiator,
                                                                  @Nullable String reason) {
        var currency = currencyCache.get(currencyId);
        if (currency == null) {
            return CompletableFuture.completedFuture(
                    TransactionResult.failure(amount, 0, "Currency '" + currencyId + "' not found"));
        }
        return deposit(player, currency, amount, initiator, reason);
    }

    @Override
    public @NotNull CompletableFuture<TransactionResult> withdraw(@NotNull OfflinePlayer player,
                                                                   @NotNull String currencyId,
                                                                   double amount,
                                                                   @Nullable Player initiator,
                                                                   @Nullable String reason) {
        var currency = currencyCache.get(currencyId);
        if (currency == null) {
            return CompletableFuture.completedFuture(
                    TransactionResult.failure(amount, 0, "Currency '" + currencyId + "' not found"));
        }
        return withdraw(player, currency, amount, initiator, reason);
    }

    @Override
    public @NotNull CompletableFuture<TransactionResult> transfer(@NotNull Player sender,
                                                                   @NotNull OfflinePlayer recipient,
                                                                   @NotNull String currencyId,
                                                                   double amount) {
        var currency = currencyCache.get(currencyId);
        if (currency == null) {
            return CompletableFuture.completedFuture(
                    TransactionResult.failure(amount, 0, "Currency '" + currencyId + "' not found"));
        }
        return transfer(sender, recipient, currency, amount);
    }

    @Override
    public @NotNull CompletableFuture<List<AccountSnapshot>> getTopAccounts(
            @NotNull String currencyId, int limit) {
        var currency = currencyCache.get(currencyId);
        if (currency == null) {
            return CompletableFuture.completedFuture(List.of());
        }
        return getTopAccounts(currency, limit)
                .thenApply(accounts -> accounts.stream()
                        .map(Account::toSnapshot)
                        .toList());
    }

    // ── Internal balance operations (entity-based) ──────────────────────────────

    /**
     * Retrieves the current balance for a player and currency entity.
     *
     * @param player   the player
     * @param currency the currency entity
     * @return future resolving to the balance, or {@code 0.0} if no account exists
     */
    public @NotNull CompletableFuture<Double> getBalance(@NotNull OfflinePlayer player,
                                                         @NotNull Currency currency) {
        return playerRepo.findByUuidAsync(player.getUniqueId())
                .thenCompose(optPlayer -> optPlayer
                        .map(ep -> accountRepo.findByPlayerAndCurrencyAsync(ep, currency)
                                .thenApply(opt -> opt.map(Account::getBalance).orElse(0.0)))
                        .orElse(CompletableFuture.completedFuture(0.0)));
    }

    /**
     * Deposits an amount into a player's account.
     *
     * @param player    the target player
     * @param currency  the currency entity
     * @param amount    the amount to deposit (must be positive)
     * @param initiator the player who initiated the deposit, or {@code null}
     * @param reason    optional human-readable reason
     * @return future resolving to the transaction result
     */
    public @NotNull CompletableFuture<TransactionResult> deposit(
            @NotNull OfflinePlayer player, @NotNull Currency currency, double amount,
            @Nullable Player initiator, @Nullable String reason) {
        if (amount <= 0) {
            return CompletableFuture.completedFuture(
                    TransactionResult.failure(amount, 0, "Amount must be positive"));
        }
        return applyBalanceChange(player, currency, amount, ChangeType.DEPOSIT, initiator, reason);
    }

    /**
     * Withdraws an amount from a player's account.
     *
     * @param player    the target player
     * @param currency  the currency entity
     * @param amount    the amount to withdraw (must be positive)
     * @param initiator the player who initiated the withdrawal, or {@code null}
     * @param reason    optional human-readable reason
     * @return future resolving to the transaction result
     */
    public @NotNull CompletableFuture<TransactionResult> withdraw(
            @NotNull OfflinePlayer player, @NotNull Currency currency, double amount,
            @Nullable Player initiator, @Nullable String reason) {
        if (amount <= 0) {
            return CompletableFuture.completedFuture(
                    TransactionResult.failure(amount, 0, "Amount must be positive"));
        }
        return applyBalanceChange(player, currency, -amount, ChangeType.WITHDRAW, initiator, reason);
    }

    /**
     * Transfers an amount from one player to another.
     *
     * <p>If the withdrawal succeeds but the deposit fails, the withdrawn amount
     * is re-deposited to the sender as a rollback.
     *
     * @param sender    the sending player
     * @param recipient the receiving player
     * @param currency  the currency entity
     * @param amount    the amount to transfer (must be positive)
     * @return future resolving to the transaction result
     */
    public @NotNull CompletableFuture<TransactionResult> transfer(
            @NotNull Player sender, @NotNull OfflinePlayer recipient,
            @NotNull Currency currency, double amount) {
        if (amount <= 0) {
            return CompletableFuture.completedFuture(
                    TransactionResult.failure(amount, 0, "Amount must be positive"));
        }
        var transferReason = "Transfer to " + recipient.getName();
        return withdraw(sender, currency, amount, sender, transferReason).thenCompose(withdrawResult -> {
            if (withdrawResult.isFailed()) {
                return CompletableFuture.completedFuture(withdrawResult);
            }
            var receiveReason = "Transfer from " + sender.getName();
            return deposit(recipient, currency, amount, sender, receiveReason).thenCompose(depositResult -> {
                if (depositResult.isFailed()) {
                    return deposit(sender, currency, amount, null, "Rollback: transfer to "
                            + recipient.getName() + " failed").thenApply(rollback ->
                            TransactionResult.failure(amount, rollback.balance(),
                                    "Transfer failed: " + depositResult.error()));
                }
                return CompletableFuture.completedFuture(
                        TransactionResult.success(amount, withdrawResult.balance()));
            });
        });
    }

    /**
     * Sets an absolute balance for a player, firing the usual pre/post events
     * with the computed delta. Values below zero are rejected.
     *
     * <p>Internally this resolves the current balance, computes the delta, and
     * delegates to {@link #deposit} or {@link #withdraw} so every event, log
     * entry, and rollback path is reused.
     *
     * @param player    the target player
     * @param currency  the currency entity
     * @param newBalance the absolute balance to set (must be {@code >= 0})
     * @param initiator the player who initiated the change, or {@code null}
     * @param reason    optional human-readable reason
     * @return future resolving to the transaction result
     */
    public @NotNull CompletableFuture<TransactionResult> setBalance(
            @NotNull OfflinePlayer player, @NotNull Currency currency, double newBalance,
            @Nullable Player initiator, @Nullable String reason) {
        if (newBalance < 0) {
            return CompletableFuture.completedFuture(
                    TransactionResult.failure(newBalance, 0, "Balance cannot be negative"));
        }
        return getBalance(player, currency).thenCompose(current -> {
            var delta = newBalance - current;
            if (delta == 0) {
                return CompletableFuture.completedFuture(TransactionResult.success(0, current));
            }
            var resolvedReason = reason != null ? reason : "Set balance";
            return delta > 0
                    ? deposit(player, currency, delta, initiator, resolvedReason)
                    : withdraw(player, currency, -delta, initiator, resolvedReason);
        });
    }

    /**
     * Resets a player's balance for a currency to zero. Convenience wrapper
     * around {@link #setBalance} that records the operation with a dedicated
     * reason for audit trails.
     *
     * @param player    the target player
     * @param currency  the currency entity
     * @param initiator the player who initiated the reset, or {@code null}
     * @return future resolving to the transaction result
     */
    public @NotNull CompletableFuture<TransactionResult> resetBalance(
            @NotNull OfflinePlayer player, @NotNull Currency currency,
            @Nullable Player initiator) {
        return setBalance(player, currency, 0.0, initiator, "Balance reset");
    }

    /**
     * Reloads the currency cache from the database.
     *
     * <p>Blocks until the cache is fully repopulated. Callers should run this
     * off the main thread — the returned future completes after the DB fetch.
     *
     * @return future that completes when the cache has been refreshed
     */
    public @NotNull CompletableFuture<Void> reload() {
        return loadCurrencies();
    }

    // ── Currency management ─────────────────────────────────────────────────────

    /**
     * Creates a new currency, firing pre/post events.
     *
     * @param currency the currency entity to create
     * @param creator  the player creating the currency, or {@code null} if server-initiated
     * @return future resolving to {@code true} if the currency was created
     */
    public @NotNull CompletableFuture<Boolean> createCurrency(@NotNull Currency currency,
                                                              @Nullable Player creator) {
        return CompletableFuture.supplyAsync(() -> {
            var creatorUuid = creator != null ? creator.getUniqueId() : null;
            var preEvent = new CurrencyCreateEvent(currency.toSnapshot(), creatorUuid);
            var cancelled = fireEventSync(preEvent);
            if (cancelled) {
                logger.warn("Currency creation cancelled for '{}': {}",
                        currency.getIdentifier(), preEvent.getCancellationReason());
                return false;
            }
            var saved = currencyRepo.create(currency);
            currencyCache.put(saved.getIdentifier(), saved);
            scheduler.runSync(() -> Bukkit.getPluginManager()
                    .callEvent(new CurrencyCreatedEvent(saved.toSnapshot(), creatorUuid)));
            logger.info("Currency '{}' created", saved.getIdentifier());
            return true;
        });
    }

    /**
     * Deletes a currency by identifier, firing pre/post events with impact data.
     *
     * @param identifier the currency identifier
     * @param deleter    the player deleting the currency, or {@code null} if server-initiated
     * @return future resolving to {@code true} if the currency was deleted
     */
    public @NotNull CompletableFuture<Boolean> deleteCurrency(@NotNull String identifier,
                                                              @Nullable Player deleter) {
        return CompletableFuture.supplyAsync(() -> {
            var optCurrency = findCurrency(identifier);
            if (optCurrency.isEmpty()) {
                logger.warn("Cannot delete unknown currency '{}'", identifier);
                return false;
            }
            var currency = optCurrency.get();
            var affectedAccounts = (int) accountRepo.countByCurrency(currency);
            var topAccounts = accountRepo.findTopByCurrency(currency, Integer.MAX_VALUE);
            var totalBalance = topAccounts.stream().mapToDouble(Account::getBalance).sum();

            var deleterUuid = deleter != null ? deleter.getUniqueId() : null;
            var snapshot = currency.toSnapshot();
            var preEvent = new CurrencyDeleteEvent(snapshot, deleterUuid, affectedAccounts, totalBalance);
            var cancelled = fireEventSync(preEvent);
            if (cancelled) {
                logger.warn("Currency deletion cancelled for '{}': {}",
                        identifier, preEvent.getCancellationReason());
                return false;
            }
            currencyRepo.delete(currency.getId());
            currencyCache.remove(identifier);
            scheduler.runSync(() -> Bukkit.getPluginManager()
                    .callEvent(new CurrencyDeletedEvent(snapshot, deleterUuid,
                            affectedAccounts, totalBalance)));
            logger.info("Currency '{}' deleted ({} accounts affected)", identifier, affectedAccounts);
            return true;
        });
    }

    /**
     * Updates an existing currency's properties in the database and in-memory cache.
     *
     * <p>If the identifier changes, the old cache entry is removed and a new one is
     * added under the new identifier. All other properties are updated in-place on the
     * managed entity.
     *
     * @param originalIdentifier the current identifier used to locate the currency
     * @param updated            value-holder carrying the new field values
     * @param editor             the player making the change, or {@code null} if server-initiated
     * @return future resolving to {@code true} if the update was persisted
     */
    public @NotNull CompletableFuture<Boolean> updateCurrency(@NotNull String originalIdentifier,
                                                              @NotNull Currency updated,
                                                              @Nullable Player editor) {
        return CompletableFuture.supplyAsync(() -> {
            var optCurrency = findCurrency(originalIdentifier);
            if (optCurrency.isEmpty()) {
                logger.warn("Cannot update unknown currency '{}'", originalIdentifier);
                return false;
            }
            var currency = optCurrency.get();

            var oldIdentifier = currency.getIdentifier();
            if (!updated.getIdentifier().isEmpty()) currency.setIdentifier(updated.getIdentifier());
            if (!updated.getSymbol().isEmpty()) currency.setSymbol(updated.getSymbol());
            currency.setPrefix(updated.getPrefix());
            currency.setSuffix(updated.getSuffix());
            if (updated.getIcon() != null && !updated.getIcon().isEmpty()) {
                currency.setIcon(updated.getIcon());
            }

            currencyRepo.update(currency);

            if (!oldIdentifier.equals(currency.getIdentifier())) {
                currencyCache.remove(oldIdentifier);
            }
            currencyCache.put(currency.getIdentifier(), currency);

            logger.info("Currency '{}' updated (was '{}')", currency.getIdentifier(), oldIdentifier);
            return true;
        });
    }

    // ── Account management ──────────────────────────────────────────────────────

    /**
     * Retrieves the account for a player and currency.
     *
     * @param player   the player
     * @param currency the currency entity
     * @return future resolving to the account, or empty if none exists
     */
    public @NotNull CompletableFuture<Optional<Account>> getAccount(
            @NotNull OfflinePlayer player, @NotNull Currency currency) {
        return playerRepo.findByUuidAsync(player.getUniqueId())
                .thenCompose(optPlayer -> optPlayer
                        .map(ep -> accountRepo.findByPlayerAndCurrencyAsync(ep, currency))
                        .orElse(CompletableFuture.completedFuture(Optional.empty())));
    }

    /**
     * Retrieves or creates an account for the given economy player and currency.
     *
     * @param econPlayer the economy player
     * @param currency   the currency entity
     * @return future resolving to the existing or newly created account
     */
    public @NotNull CompletableFuture<Account> getOrCreateAccount(
            @NotNull EconomyPlayer econPlayer, @NotNull Currency currency) {
        return accountRepo.findByPlayerAndCurrencyAsync(econPlayer, currency)
                .thenCompose(opt -> {
                    if (opt.isPresent()) {
                        return CompletableFuture.completedFuture(opt.get());
                    }
                    var account = new Account(econPlayer, currency);
                    return accountRepo.createAsync(account);
                });
    }

    /**
     * Convenience overload that finds or creates the EconomyPlayer first,
     * then ensures an account exists for the given currency.
     *
     * <p>The player name defaults to a 16-char truncation of the UUID when unknown,
     * because {@code PLAYER_NAME} is capped at 16 (Minecraft username maximum).
     *
     * @param playerUuid the player's UUID
     * @param currency   the currency entity
     * @return future resolving to the existing or newly created account
     */
    public @NotNull CompletableFuture<Account> getOrCreateAccount(
            @NotNull UUID playerUuid, @NotNull Currency currency) {
        return getOrCreateAccount(playerUuid, playerUuid.toString().substring(0, 16), currency);
    }

    /**
     * Convenience overload that finds or creates the EconomyPlayer using the given
     * name, then ensures an account exists for the given currency.
     *
     * @param playerUuid the player's UUID
     * @param playerName the player's current Minecraft name (max 16 chars)
     * @param currency   the currency entity
     * @return future resolving to the existing or newly created account
     */
    public @NotNull CompletableFuture<Account> getOrCreateAccount(
            @NotNull UUID playerUuid, @NotNull String playerName, @NotNull Currency currency) {
        var safeName = playerName.length() > 16 ? playerName.substring(0, 16) : playerName;
        return playerRepo.findOrCreateAsync(playerUuid, safeName)
                .thenCompose(econPlayer -> getOrCreateAccount(econPlayer, currency));
    }

    /**
     * Retrieves all accounts for a player across all currencies.
     *
     * @param player the player
     * @return future resolving to the list of accounts
     */
    public @NotNull CompletableFuture<List<Account>> getAccounts(@NotNull OfflinePlayer player) {
        return playerRepo.findByUuidAsync(player.getUniqueId())
                .thenCompose(optPlayer -> optPlayer
                        .map(accountRepo::findByPlayerAsync)
                        .orElse(CompletableFuture.completedFuture(List.of())));
    }

    /**
     * Returns the top accounts for a currency, ordered by balance descending.
     *
     * @param currency the currency entity
     * @param limit    maximum number of results
     * @return future resolving to ordered list of account entities (highest balance first)
     */
    public @NotNull CompletableFuture<List<Account>> getTopAccounts(
            @NotNull Currency currency, int limit) {
        return accountRepo.findTopByCurrencyAsync(currency, limit);
    }

    // ── Internal ────────────────────────────────────────────────────────────────

    /**
     * Core balance-change logic shared by deposit and withdraw.
     */
    private @NotNull CompletableFuture<TransactionResult> applyBalanceChange(
            @NotNull OfflinePlayer player, @NotNull Currency currency, double signedAmount,
            @NotNull ChangeType changeType, @Nullable Player initiator, @Nullable String reason) {

        var uuid = player.getUniqueId();
        // PLAYER_NAME is capped at 16 chars (Minecraft max). UUIDs are 36, so clamp.
        var rawName = player.getName() != null ? player.getName() : uuid.toString();
        var name = rawName.length() > 16 ? rawName.substring(0, 16) : rawName;

        return playerRepo.findOrCreateAsync(uuid, name).thenCompose(econPlayer ->
                getOrCreateAccount(econPlayer, currency).thenCompose(account -> {
                    var oldBalance = account.getBalance();
                    var newBalance = oldBalance + signedAmount;

                    if (newBalance < 0) {
                        return CompletableFuture.completedFuture(
                                TransactionResult.failure(Math.abs(signedAmount), oldBalance,
                                        "Insufficient funds"));
                    }

                    var initiatorUuid = initiator != null ? initiator.getUniqueId() : null;
                    var snapshot = currency.toSnapshot();
                    var preEvent = new BalanceChangeEvent(econPlayer.getUniqueId(),
                            econPlayer.getPlayerName(), snapshot,
                            oldBalance, newBalance, changeType, reason, initiatorUuid);
                    var cancelled = fireEventSync(preEvent);
                    if (cancelled) {
                        return CompletableFuture.completedFuture(
                                TransactionResult.failure(Math.abs(signedAmount), oldBalance,
                                        preEvent.getCancellationReason() != null
                                                ? preEvent.getCancellationReason()
                                                : "Cancelled by event"));
                    }

                    account.setBalance(newBalance);
                    return accountRepo.updateAsync(account).thenApply(saved -> {
                        scheduler.runSync(() -> Bukkit.getPluginManager()
                                .callEvent(new BalanceChangedEvent(econPlayer.getUniqueId(),
                                        econPlayer.getPlayerName(), snapshot,
                                        oldBalance, newBalance, changeType, reason, initiatorUuid)));

                        txLogger.log(new TransactionLog(uuid, name, currency, changeType,
                                oldBalance, newBalance, Math.abs(signedAmount),
                                reason, initiatorUuid, true, null));

                        return TransactionResult.success(Math.abs(signedAmount), newBalance);
                    });
                }));
    }

    /**
     * Fires a cancellable event on the main thread and blocks until it completes.
     *
     * @return {@code true} if the event was cancelled
     */
    private boolean fireEventSync(@NotNull Event event) {
        var future = new CompletableFuture<Void>();
        scheduler.runSync(() -> {
            Bukkit.getPluginManager().callEvent(event);
            future.complete(null);
        });
        future.join();
        if (event instanceof Cancellable cancellable) {
            return cancellable.isCancelled();
        }
        return false;
    }
}
