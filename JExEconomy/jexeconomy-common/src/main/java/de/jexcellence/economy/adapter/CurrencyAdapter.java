package de.jexcellence.economy.adapter;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.CurrencyLog;
import de.jexcellence.economy.database.entity.User;
import de.jexcellence.economy.database.entity.UserCurrency;
import de.jexcellence.economy.database.repository.CurrencyRepository;
import de.jexcellence.economy.event.*;
import de.jexcellence.economy.type.EChangeType;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Primary implementation of the ICurrencyAdapter interface providing comprehensive currency management operations.
 *
 * <p>This adapter serves as the main entry point for all currency-related operations within the JExEconomy system.
 * It provides asynchronous implementations for balance management, transaction processing, entity creation,
 * and query operations while ensuring thread safety and proper error handling. Enhanced with event system
 * for external plugin integration and operation monitoring.
 *
 * <p>All operations fire appropriate events that can be listened to by other plugins or internal systems
 * for logging, monitoring, and integration purposes. The adapter maintains separation of concerns by
 * focusing solely on currency operations while delegating logging to event listeners.
 *
 * <p><strong>Core Functionality:</strong>
 * <ul>
 *   <li><strong>Balance Operations:</strong> Retrieve player balances with caching and optimization</li>
 *   <li><strong>Transaction Processing:</strong> Handle deposits and withdrawals with validation and persistence</li>
 *   <li><strong>Entity Management:</strong> Create and manage players, currencies, and their relationships</li>
 *   <li><strong>Query Services:</strong> Efficient lookup of currency data and player associations</li>
 *   <li><strong>Error Handling:</strong> Comprehensive error reporting and logging</li>
 *   <li><strong>Event System:</strong> Fire events for currency operations and balance changes</li>
 * </ul>
 *
 * <p><strong>Design Principles:</strong>
 * <ul>
 *   <li><strong>Asynchronous Operations:</strong> All database operations execute on background threads</li>
 *   <li><strong>Thread Safety:</strong> Safe for concurrent access from multiple threads</li>
 *   <li><strong>Error Resilience:</strong> Graceful handling of database errors and edge cases</li>
 *   <li><strong>Performance Optimization:</strong> Efficient database queries and caching strategies</li>
 *   <li><strong>Event Integration:</strong> Comprehensive event firing for external plugin integration</li>
 *   <li><strong>Separation of Concerns:</strong> Focused on currency operations, delegates logging to events</li>
 * </ul>
 *
 * <p><strong>Implementation Notes:</strong>
 *
 * <p>This implementation uses the JExEconomy plugin's repository layer for data access and leverages
 * the plugin's executor service for asynchronous operations. All monetary operations include
 * appropriate validation and atomic updates to ensure data consistency. Events are fired for
 * all significant operations to allow external plugins to integrate with the currency system.
 * Logging is handled by event listeners to maintain clean separation of concerns.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 * @see ICurrencyAdapter
 * @see CurrencyResponse
 */
public class CurrencyAdapter implements ICurrencyAdapter {

    /**
     * Logger instance for recording currency adapter operations and errors.
 *
 * <p>Used for debugging, monitoring, and error tracking throughout the adapter's lifecycle.
     * All significant operations and errors are logged with appropriate severity levels.
     * Database logging is handled separately through event listeners.
     */
    private static final Logger ADAPTER_LOGGER = CentralLogger.getLoggerByName("JExEconomy");

    /**
     * Reference to the currency runtime providing access to all system components.
 *
 * <p>Provides access to repositories, executor services, and other plugin components
     * required for currency operations. This reference is used throughout the adapter
     * to access the plugin's infrastructure and services.
     */
    private JExEconomy jexEconomyImpl;

    /**
     * Constructs a new CurrencyAdapter with the specified currency runtime.
 *
 * <p>Initializes the adapter with access to the runtime's repositories, executor services,
     * and other infrastructure components required for currency operations.
     *
     * @param jexEconomyImpl the currency runtime providing access to repositories and services, must not be null
     * @throws IllegalArgumentException if runtime is null
     */
    public CurrencyAdapter(final @NotNull JExEconomy jexEconomyImpl) {
        this.jexEconomyImpl = jexEconomyImpl;
    }

    /**
     * Retrieves the current balance of a player for a specific currency.
 *
 * <p>This method performs an asynchronous lookup of the player's balance in the specified
     * currency. If the player has no account for the given currency, the balance will be 0.0.
     * The operation is safe for offline players and will not cause server lag.
     *
     * @param targetOfflinePlayer the player whose balance should be retrieved, must not be null
     * @param targetCurrency the currency for which to retrieve the balance, must not be null
     * @return a CompletableFuture containing the player's balance as a Double value
     * @throws IllegalArgumentException if either parameter is null
     */
    @Override
    public @NotNull CompletableFuture<Double> getBalance(
            final @NotNull OfflinePlayer targetOfflinePlayer,
            final @NotNull Currency targetCurrency
    ) {
        return this.findUserCurrencyEntity(targetOfflinePlayer, targetCurrency)
                .thenApplyAsync(
                        userCurrencyEntity -> {
                            if (userCurrencyEntity != null) {
                                return userCurrencyEntity.getBalance();
                            } else {
                                return 0.0;
                            }
                        },
                        this.jexEconomyImpl.getExecutor()
                );
    }

    /**
     * Retrieves the balance from an existing UserCurrency entity.
 *
 * <p>This method provides direct access to the balance stored in a UserCurrency entity.
     * It's more efficient than the player-based method when you already have the
     * UserCurrency entity available, as it avoids additional database lookups.
     *
     * @param userCurrencyEntity the UserCurrency entity containing the balance, must not be null
     * @return a CompletableFuture containing the balance as a Double value
     * @throws IllegalArgumentException if the userCurrencyEntity is null
     */
    @Override
    public @NotNull CompletableFuture<Double> getBalance(final @NotNull UserCurrency userCurrencyEntity) {
        return CompletableFuture.supplyAsync(
                userCurrencyEntity::getBalance,
                this.jexEconomyImpl.getExecutor()
        );
    }

    /**
     * Deposits a specified amount of currency into a player's account.
 *
 * <p>This method performs a secure deposit operation, adding the specified amount to the
     * player's existing balance for the given currency. The operation includes validation
     * to ensure the amount is positive and the currency is valid. If the player doesn't
     * have an account for the currency, one will be created automatically.
     *
     * @param targetOfflinePlayer the player to deposit currency to, must not be null
     * @param targetCurrency the currency to deposit, must not be null
     * @param depositAmount the amount to deposit, must be positive
     * @return a CompletableFuture containing a CurrencyResponse with operation details
     * @throws IllegalArgumentException if player or currency is null, or amount is not positive
     */
    @Override
    public @NotNull CompletableFuture<CurrencyResponse> deposit(
            final @NotNull OfflinePlayer targetOfflinePlayer,
            final @NotNull Currency targetCurrency,
            final double depositAmount
    ) {
        return this.findUserCurrencyEntity(targetOfflinePlayer, targetCurrency)
                .thenApplyAsync(
                        userCurrencyEntity -> {
                            if (userCurrencyEntity == null) {
                                return this.createFailureResponse(
                                        depositAmount,
                                        0.0,
                                        "Player account not found for UUID: " + targetOfflinePlayer.getUniqueId()
                                );
                            }
                            return this.executeTransactionOperation(
                                    userCurrencyEntity,
                                    depositAmount,
                                    UserCurrency::deposit,
                                    "deposited",
                                    "to",
                                    EChangeType.DEPOSIT,
                                    "Deposit via adapter",
                                    targetOfflinePlayer.getPlayer()
                            );
                        },
                        this.jexEconomyImpl.getExecutor()
                );
    }

    /**
     * Executes deposit.
     */
    @Override
    public @NotNull CompletableFuture<CurrencyResponse> deposit(@NotNull OfflinePlayer targetOfflinePlayer, @NotNull String targetCurrencyIdentifier, double depositAmount) {
        return this.findUserCurrencyEntity(targetOfflinePlayer, targetCurrencyIdentifier)
                .thenApplyAsync(
                        userCurrencyEntity -> {
                            if (userCurrencyEntity == null) {
                                return this.createFailureResponse(
                                        depositAmount,
                                        0.0,
                                        "Player account not found for UUID: " + targetOfflinePlayer.getUniqueId()
                                );
                            }
                            return this.executeTransactionOperation(
                                    userCurrencyEntity,
                                    depositAmount,
                                    UserCurrency::deposit,
                                    "deposited",
                                    "to",
                                    EChangeType.DEPOSIT,
                                    "Deposit via adapter",
                                    targetOfflinePlayer.getPlayer()
                            );
                        },
                        this.jexEconomyImpl.getExecutor()
                );
    }

    /**
     * Deposits a specified amount into an existing UserCurrency entity.
 *
 * <p>This method provides direct deposit functionality for UserCurrency entities,
     * offering better performance when the entity is already available. The operation
     * updates the entity's balance and persists the changes to the database.
     *
     * @param userCurrencyEntity the UserCurrency entity to deposit to, must not be null
     * @param depositAmount the amount to deposit, must be positive
     * @return a CompletableFuture containing a CurrencyResponse with operation details
     * @throws IllegalArgumentException if entity is null or amount is not positive
     */
    @Override
    public @NotNull CompletableFuture<CurrencyResponse> deposit(
            final @NotNull UserCurrency userCurrencyEntity,
            final double depositAmount
    ) {
        return CompletableFuture.supplyAsync(
                () -> this.executeTransactionOperation(
                        userCurrencyEntity,
                        depositAmount,
                        UserCurrency::deposit,
                        "deposited",
                        "to",
                        EChangeType.DEPOSIT,
                        "Direct deposit via adapter",
                        Bukkit.getPlayer(userCurrencyEntity.getPlayer().getUniqueId())
                ),
                this.jexEconomyImpl.getExecutor()
        );
    }

    /**
     * Withdraws a specified amount of currency from a player's account.
 *
 * <p>This method performs a secure withdrawal operation, removing the specified amount
     * from the player's balance for the given currency. The operation includes validation
     * to ensure sufficient funds are available and the amount is positive. Insufficient
     * funds will result in a failure response without modifying the balance.
     *
     * @param targetOfflinePlayer the player to withdraw currency from, must not be null
     * @param targetCurrency the currency to withdraw, must not be null
     * @param withdrawalAmount the amount to withdraw, must be positive
     * @return a CompletableFuture containing a CurrencyResponse with operation details
     * @throws IllegalArgumentException if player or currency is null, or amount is not positive
     */
    @Override
    public @NotNull CompletableFuture<CurrencyResponse> withdraw(
            final @NotNull OfflinePlayer targetOfflinePlayer,
            final @NotNull Currency targetCurrency,
            final double withdrawalAmount
    ) {
        return this.findUserCurrencyEntity(targetOfflinePlayer, targetCurrency)
                .thenApplyAsync(
                        userCurrencyEntity -> {
                            if (userCurrencyEntity == null) {
                                return this.createFailureResponse(
                                        withdrawalAmount,
                                        0.0,
                                        "Player account not found for UUID: " + targetOfflinePlayer.getUniqueId()
                                );
                            }
                            return this.executeTransactionOperation(
                                    userCurrencyEntity,
                                    withdrawalAmount,
                                    UserCurrency::withdraw,
                                    "withdrawn",
                                    "from",
                                    EChangeType.WITHDRAW,
                                    "Withdrawal via adapter",
                                    targetOfflinePlayer.getPlayer()
                            );
                        },
                        this.jexEconomyImpl.getExecutor()
                );
    }

    /**
     * Withdraws a specified amount from an existing UserCurrency entity.
 *
 * <p>This method provides direct withdrawal functionality for UserCurrency entities,
     * offering optimal performance when the entity is already loaded. The operation
     * validates sufficient funds and updates the entity balance atomically.
     *
     * @param userCurrencyEntity the UserCurrency entity to withdraw from, must not be null
     * @param withdrawalAmount the amount to withdraw, must be positive
     * @return a CompletableFuture containing a CurrencyResponse with operation details
     * @throws IllegalArgumentException if entity is null or amount is not positive
     */
    @Override
    public @NotNull CompletableFuture<CurrencyResponse> withdraw(
            final @NotNull UserCurrency userCurrencyEntity,
            final double withdrawalAmount
    ) {
        return CompletableFuture.supplyAsync(
                () -> this.executeTransactionOperation(
                        userCurrencyEntity,
                        withdrawalAmount,
                        UserCurrency::withdraw,
                        "withdrawn",
                        "from",
                        EChangeType.WITHDRAW,
                        "Direct withdrawal via adapter",
                        Bukkit.getPlayer(userCurrencyEntity.getPlayer().getUniqueId())
                ),
                this.jexEconomyImpl.getExecutor()
        );
    }

    /**
     * Creates a new player entity in the currency system.
 *
 * <p>This method initializes a new player record in the database, creating the necessary
     * foundation for currency operations. The player entity stores essential information
     * such as UUID and current name, and serves as the basis for all currency accounts.
     *
     * @param targetOfflinePlayer the player for whom to create an entity, must not be null
     * @return a CompletableFuture containing true if creation was successful, false otherwise
     * @throws IllegalArgumentException if the targetOfflinePlayer is null
     */
    @Override
    public @NotNull CompletableFuture<Boolean> createPlayer(final @NotNull OfflinePlayer targetOfflinePlayer) {
        return CompletableFuture.supplyAsync(
                () -> {
                    final String playerName = targetOfflinePlayer.getName();
                    if (playerName == null) {
                        ADAPTER_LOGGER.log(
                                Level.WARNING,
                                "Cannot create player entity - name is null for UUID: " + targetOfflinePlayer.getUniqueId()
                        );
                        return false;
                    }

                    final User existingUserEntity = this.jexEconomyImpl.getUserRepository()
                            .findByAttributes(Map.of("uniqueId", targetOfflinePlayer.getUniqueId())).orElse(null);

                    if (existingUserEntity != null) {
                        return false;
                    }

                    final User newUserEntity = new User(targetOfflinePlayer.getUniqueId(), playerName);
                    final User createdUserEntity = this.jexEconomyImpl.getUserRepository().create(newUserEntity);

                    return createdUserEntity != null;
                },
                this.jexEconomyImpl.getExecutor()
        );
    }

    /**
     * Creates a new currency entity in the system with event firing.
 *
 * <p>This method registers a new currency type in the database, making it available
     * for use throughout the system. The currency entity contains all configuration
     * information including symbol, prefix, suffix, and display properties. Events
     * are fired before and after creation to allow external plugins to integrate.
     *
     * @param newCurrencyEntity the currency entity to create, must not be null and must be valid
     * @return a CompletableFuture containing true if creation was successful, false otherwise
     * @throws IllegalArgumentException if the newCurrencyEntity is null or invalid
     */
    @Override
    public @NotNull CompletableFuture<Boolean> createCurrency(final @NotNull Currency newCurrencyEntity) {
        return this.createCurrency(newCurrencyEntity, null);
    }

    /**
     * Creates a new currency entity in the system with event firing and player context.
 *
 * <p>This method registers a new currency type in the database, making it available
     * for use throughout the system. The currency entity contains all configuration
     * information including symbol, prefix, suffix, and display properties. Events
     * are fired before and after creation to allow external plugins to integrate.
     *
     * @param newCurrencyEntity the currency entity to create, must not be null and must be valid
     * @param creator the player creating the currency (null if created by system/console)
     * @return a CompletableFuture containing true if creation was successful, false otherwise
     * @throws IllegalArgumentException if the newCurrencyEntity is null or invalid
     */
    public @NotNull CompletableFuture<Boolean> createCurrency(
            final @NotNull Currency newCurrencyEntity,
            final @Nullable Player creator
    ) {
        final CurrencyCreateEvent createEvent = new CurrencyCreateEvent(newCurrencyEntity, creator);

        this.jexEconomyImpl.getPlatform().getScheduler().runSync(() -> {
            Bukkit.getPluginManager().callEvent(createEvent);
        });

        if (createEvent.isCancelled()) {
            ADAPTER_LOGGER.log(Level.INFO, "Currency creation cancelled: " + createEvent.getCancelReason());
            if (creator != null) {
                new I18n.Builder("currency_adapter.creation_cancelled", creator).withPlaceholder("reason", createEvent.getCancelReason()).build().sendMessage();
            }
            return CompletableFuture.completedFuture(false);
        }

        final boolean currencyAlreadyExists = this.jexEconomyImpl.getCurrencies().values().stream()
                .map(Currency::getIdentifier)
                .anyMatch(existingIdentifier -> existingIdentifier.equals(newCurrencyEntity.getIdentifier()));

        if (currencyAlreadyExists) {
            ADAPTER_LOGGER.log(Level.WARNING, "Currency already exists: " + newCurrencyEntity.getIdentifier());
            if (creator != null) {
                new I18n.Builder("currency_adapter.already_exists", creator).withPlaceholder("currency_identifier", newCurrencyEntity.getIdentifier()).build().sendMessage();
            }
            return CompletableFuture.completedFuture(false);
        }

        return this.jexEconomyImpl.getCurrencyRepository().createAsync(newCurrencyEntity)
                .thenApply(currency -> {
                    if (currency == null) {
                        return false;
                    }

                    try {
                        this.jexEconomyImpl.getCurrencies().put(currency.getId(), currency);
                    } catch (UnsupportedOperationException e) {
                        ADAPTER_LOGGER.log(Level.FINE,
                                "Currency cache is unmodifiable, relying on repository to handle cache updates for currency: "
                                        + currency.getIdentifier());
                    }

                    final CurrencyCreatedEvent createdEvent = new CurrencyCreatedEvent(currency, creator);
                    this.jexEconomyImpl.getPlatform().getScheduler().runSync(() -> {
                        Bukkit.getPluginManager().callEvent(createdEvent);
                    });

                    this.createManagementLog(
                            currency,
                            "created",
                            creator,
                            true,
                            "Currency created successfully via CurrencyAdapter",
                            null
                    );

                    ADAPTER_LOGGER.log(Level.INFO, "Currency created successfully: " + currency.getIdentifier());
                    if (creator != null) {
                        new I18n.Builder("currency_adapter.created_successfully", creator).withPlaceholder("currency_identifier", currency.getIdentifier()).build().sendMessage();
                    }

                    return true;
                })
                .exceptionallyAsync(throwable -> {
                    ADAPTER_LOGGER.log(Level.WARNING,
                            "Failed to create currency with identifier: " + newCurrencyEntity.getIdentifier(),
                            throwable);

                    if (creator != null) {
                        new I18n.Builder("currency_adapter.creation_failed", creator).withPlaceholders(
                                Map.of("currency_identifier", newCurrencyEntity.getIdentifier(), "error_message", throwable.getMessage())
                        ).build().sendMessage();
                    }

                    this.createManagementLog(
                            newCurrencyEntity,
                            "creation_failed",
                            creator,
                            false,
                            "Currency creation failed via CurrencyAdapter",
                            throwable.getMessage()
                    );

                    return false;
                });
    }

    /**
     * Deletes a currency from the system with event firing.
 *
 * <p>This method removes a currency from the database and all associated player accounts.
     * Events are fired before and after deletion to allow external plugins to integrate
     * and potentially prevent deletion of important currencies.
     *
     * @param currencyIdentifier the identifier of the currency to delete, must not be null
     * @param deleter the player deleting the currency (null if deleted by system/console)
     * @return a CompletableFuture containing true if deletion was successful, false otherwise
     */
    public @NotNull CompletableFuture<Boolean> deleteCurrency(
            final @NotNull String currencyIdentifier,
            final @Nullable Player deleter
    ) {
        return CompletableFuture.supplyAsync(
                () -> this.jexEconomyImpl.getCurrencyRepository().findByAttributes(Map.of("identifier", currencyIdentifier)).orElse(null),
                this.jexEconomyImpl.getExecutor()
        ).thenCompose(currency -> {
                    if (currency == null) {
                        ADAPTER_LOGGER.log(Level.WARNING, "Currency not found for deletion: " + currencyIdentifier);
                        if (deleter != null) {
                            new I18n.Builder("currency_adapter.not_found", deleter).withPlaceholder("currency_identifier", currencyIdentifier).build().sendMessage();
                        }
                        return CompletableFuture.completedFuture(false);
                    }

                    return this.getAffectedPlayersCount(currency)
                            .thenCombine(this.getTotalBalance(currency), (playerCount, totalBalance) -> {

                                final CurrencyDeleteEvent deleteEvent = new CurrencyDeleteEvent(currency, deleter, playerCount, totalBalance);
                                this.jexEconomyImpl.getPlatform().getScheduler().runSync(() -> {
                                    Bukkit.getPluginManager().callEvent(deleteEvent);
                                });

                                if (deleteEvent.isCancelled()) {
                                    ADAPTER_LOGGER.log(Level.INFO, "Currency deletion cancelled: " + deleteEvent.getCancelReason());
                                    if (deleter != null) {
                                        new I18n.Builder("currency_adapter.deletion_cancelled", deleter).withPlaceholder("reason", deleteEvent.getCancelReason()).build().sendMessage();
                                    }
                                    return CompletableFuture.completedFuture(false);
                                }

                                return this.jexEconomyImpl.getCurrencyRepository().deleteAsync(currency.getId())
                                        .thenApply(success -> {
                                            if (success) {
                                                try {
                                                    this.jexEconomyImpl.getCurrencies().remove(currency.getId());
                                                } catch (
                                                        final UnsupportedOperationException exception
                                                ) {
                                                    ADAPTER_LOGGER.log(Level.FINE, "Currency cache is unmodifiable");
                                                }

                                                final CurrencyDeletedEvent deletedEvent = new CurrencyDeletedEvent(
                                                        currency, deleter, playerCount, totalBalance
                                                );
                                                this.jexEconomyImpl.getPlatform().getScheduler().runSync(() -> {
                                                    Bukkit.getPluginManager().callEvent(deletedEvent);
                                                });

                                                ADAPTER_LOGGER.log(Level.INFO, "Currency deleted successfully: " + currencyIdentifier);
                                                if (deleter != null) {
                                                    new I18n.Builder("currency_adapter.deleted_successfully", deleter).withPlaceholders(
                                                            Map.of(
                                                                    "currency_identifier", currencyIdentifier,
                                                                    "affected_players", String.valueOf(playerCount),
                                                                    "total_balance", String.format("%.2f", totalBalance)
                                                            )
                                                    ).build().sendMessage();
                                                }
                                            } else {
                                                ADAPTER_LOGGER.log(Level.WARNING, "Failed to delete currency: " + currencyIdentifier);
                                                if (deleter != null) {
                                                    new I18n.Builder("currency_adapter.deletion_failed", deleter).withPlaceholder("currency_identifier", currencyIdentifier).build().sendMessage();
                                                }
                                            }
                                            return success;
                                        });
                            })
                            .thenCompose(future -> future);
                });
    }

    /**
     * Checks whether a currency with the specified identifier exists in the system.
 *
 * <p>This method performs a lookup to determine if a currency with the given identifier
     * is registered in the database. It's useful for validation before performing
     * operations that require a specific currency to exist.
     *
     * @param currencyIdentifier the identifier of the currency to check, may be null
     * @return a CompletableFuture containing true if the currency exists, false otherwise
     */
    @Override
    public @NotNull CompletableFuture<Boolean> hasGivenCurrency(final @Nullable String currencyIdentifier) {
        return CompletableFuture.supplyAsync(
                () -> {
                    if (currencyIdentifier == null) {
                        return false;
                    }

                    final Currency foundCurrencyEntity = this.jexEconomyImpl.getCurrencyRepository().findByAttributes(Map.of("identifier", currencyIdentifier)).orElse(null);

                    return foundCurrencyEntity != null;
                },
                this.jexEconomyImpl.getExecutor()
        );
    }

    /**
     * Creates a new player-currency relationship entity.
 *
 * <p>This method establishes a connection between a player and a currency by creating
     * a UserCurrency entity. This entity represents the player's account for that specific
     * currency and tracks their balance and transaction history.
     *
     * @param targetPlayerEntity the User entity representing the player, must not be null
     * @param targetCurrencyEntity the Currency entity to associate with the player, must not be null
     * @return a CompletableFuture containing true if creation was successful, false otherwise
     * @throws IllegalArgumentException if either entity is null
     */
    @Override
    public @NotNull CompletableFuture<Boolean> createPlayerCurrency(
            final @NotNull User targetPlayerEntity,
            final @NotNull Currency targetCurrencyEntity
    ) {
        return CompletableFuture.supplyAsync(
                () -> {
                    final UserCurrency existingUserCurrencyEntity = this.jexEconomyImpl.getUserCurrencyRepository()
                            .findByAttributes(Map.of(
                                    "player.uniqueId", targetPlayerEntity.getUniqueId(),
                                    "currency.id", targetCurrencyEntity.getId()
                            )).orElse(null);

                    if (existingUserCurrencyEntity != null) {
                        return false;
                    }

                    final UserCurrency newUserCurrencyEntity = new UserCurrency(targetPlayerEntity, targetCurrencyEntity);
                    final UserCurrency createdUserCurrencyEntity = this.jexEconomyImpl.getUserCurrencyRepository()
                            .create(newUserCurrencyEntity);

                    return createdUserCurrencyEntity != null;
                },
                this.jexEconomyImpl.getExecutor()
        );
    }

    /**
     * Retrieves all currency accounts associated with a specific player.
 *
 * <p>This method returns a comprehensive list of all UserCurrency entities for the
     * specified player, providing access to all their currency accounts and balances.
     * The list includes all currencies the player has accounts for, regardless of
     * whether the balance is zero or positive.
     *
     * @param targetOfflinePlayer the player whose currency accounts should be retrieved, must not be null
     * @return a CompletableFuture containing a List of UserCurrency entities, may be empty but never null
     * @throws IllegalArgumentException if targetOfflinePlayer is null
     */
    @Override
    public @NotNull CompletableFuture<List<UserCurrency>> getUserCurrencies(final @NotNull OfflinePlayer targetOfflinePlayer) {
        return CompletableFuture.supplyAsync(
                () -> this.jexEconomyImpl.getUserCurrencyRepository()
                        .findAllByAttributes(Map.of("player.uniqueId", targetOfflinePlayer.getUniqueId())),
                this.jexEconomyImpl.getExecutor()
        );
    }

    /**
     * Retrieves a specific currency account for a player by currency identifier.
 *
 * <p>This method performs a targeted lookup to find the UserCurrency entity that
     * represents the specified player's account for the named currency. It's more
     * efficient than retrieving all currencies when you only need one specific account.
     *
     * @param targetOfflinePlayer the player whose currency account should be retrieved, must not be null
     * @param currencyIdentifier the identifier of the currency to retrieve, may be null
     * @return a CompletableFuture containing the UserCurrency entity, or null if not found
     * @throws IllegalArgumentException if targetOfflinePlayer is null
     */
    @Override
    public @NotNull CompletableFuture<@Nullable UserCurrency> getUserCurrency(
            final @NotNull OfflinePlayer targetOfflinePlayer,
            final @Nullable String currencyIdentifier
    ) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        if (currencyIdentifier == null) {
                            return null;
                        }

                        final List<UserCurrency> playerCurrencyEntities = this.jexEconomyImpl.getUserCurrencyRepository()
                                .findAllByAttributes(Map.of("player.uniqueId", targetOfflinePlayer.getUniqueId()));

                        return playerCurrencyEntities.stream()
                                .filter(userCurrencyEntity -> {
                                    final Currency currencyEntity = userCurrencyEntity.getCurrency();
                                    return currencyIdentifier.equals(currencyEntity.getIdentifier());
                                })
                                .findFirst()
                                .orElse(null);
                    } catch (final Exception databaseException) {
                        ADAPTER_LOGGER.log(
                                Level.WARNING,
                                "Failed to retrieve currency for player UUID: " + targetOfflinePlayer.getUniqueId() +
                                        ", currency identifier: " + currencyIdentifier,
                                databaseException
                        );
                        return null;
                    }
                },
                this.jexEconomyImpl.getExecutor()
        ).exceptionallyAsync(
                throwable -> {
                    ADAPTER_LOGGER.log(
                            Level.WARNING,
                            "Async operation failed for player UUID: " + targetOfflinePlayer.getUniqueId() +
                                    ", currency identifier: " + currencyIdentifier,
                            throwable
                    );
                    return null;
                },
                this.jexEconomyImpl.getExecutor()
        );
    }

    /**
     * Locates a UserCurrency entity for the specified player and currency combination.
 *
 * <p>This private helper method performs database lookup to find the UserCurrency entity
     * that represents the player's account for the specified currency. It's used internally
     * by other methods to avoid code duplication and ensure consistent lookup behavior.
     *
     * @param targetOfflinePlayer the player to find the currency account for, must not be null
     * @param targetCurrency the currency to find the account for, must not be null
     * @return a CompletableFuture containing the UserCurrency entity if found, or {@code null} otherwise
     */
    private @NotNull CompletableFuture<@Nullable UserCurrency> findUserCurrencyEntity(
            final @NotNull OfflinePlayer targetOfflinePlayer,
            final @NotNull Currency targetCurrency
    ) {
        return CompletableFuture.supplyAsync(
                () -> this.jexEconomyImpl.getUserCurrencyRepository()
                        .findByAttributes(Map.of(
                                "player.uniqueId", targetOfflinePlayer.getUniqueId(),
                                "currency.id", targetCurrency.getId()
                        )).orElse(null),
                this.jexEconomyImpl.getExecutor()
        );
    }

    private @NotNull CompletableFuture<@Nullable UserCurrency> findUserCurrencyEntity(
            final @NotNull OfflinePlayer targetOfflinePlayer,
            final @NotNull String targetCurrencyIdentifier
    ) {
        return CompletableFuture.supplyAsync(
                () -> this.jexEconomyImpl.getUserCurrencyRepository()
                        .findByAttributes(Map.of(
                                "player.uniqueId", targetOfflinePlayer.getUniqueId(),
                                "currency.identifier", targetCurrencyIdentifier
                        )).orElse(null),
                this.jexEconomyImpl.getExecutor()
        );
    }

    /**
     * Gets the number of players affected by a currency.
 *
 * <p>This helper method counts how many players have accounts for the specified currency.
     * Used for providing statistics in currency deletion events.
     *
     * @param currency the currency to count affected players for
     * @return a CompletableFuture containing the number of affected players
     */
    private @NotNull CompletableFuture<Integer> getAffectedPlayersCount(final @NotNull Currency currency) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        final List<UserCurrency> userCurrencies = this.jexEconomyImpl.getUserCurrencyRepository()
                                .findAllByAttributes(Map.of("currency.id", currency.getId()));
                        return userCurrencies.size();
                    } catch (final Exception e) {
                        ADAPTER_LOGGER.log(Level.WARNING, "Failed to get affected players count for currency: " + currency.getIdentifier(), e);
                        return 0;
                    }
                },
                this.jexEconomyImpl.getExecutor()
        );
    }

    /**
     * Gets the total balance across all players for a currency.
 *
 * <p>This helper method calculates the sum of all player balances for the specified currency.
     * Used for providing statistics in currency deletion events.
     *
     * @param currency the currency to calculate total balance for
     * @return a CompletableFuture containing the total balance
     */
    private @NotNull CompletableFuture<Double> getTotalBalance(final @NotNull Currency currency) {
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        final List<UserCurrency> userCurrencies = this.jexEconomyImpl.getUserCurrencyRepository()
                                .findAllByAttributes(Map.of("currency.id", currency.getId()));
                        return userCurrencies.stream()
                                .mapToDouble(UserCurrency::getBalance)
                                .sum();
                    } catch (final Exception e) {
                        ADAPTER_LOGGER.log(Level.WARNING, "Failed to get total balance for currency: " + currency.getIdentifier(), e);
                        return 0.0;
                    }
                },
                this.jexEconomyImpl.getExecutor()
        );
    }

    /**
     * Creates a standardized failure response with error logging.
 *
 * <p>This private helper method creates CurrencyResponse objects for failed operations
     * with consistent error handling and logging. It ensures all failure responses
     * follow the same format and are properly logged for debugging purposes.
     *
     * @param attemptedAmount the amount that was attempted in the failed operation
     * @param currentBalance the current balance at the time of failure
     * @param errorMessage the descriptive error message explaining the failure, must not be null
     * @return a CurrencyResponse indicating operation failure with the provided details
     */
    private @NotNull CurrencyResponse createFailureResponse(
            final double attemptedAmount,
            final double currentBalance,
            final @NotNull String errorMessage
    ) {
        final CurrencyResponse failureResponse = CurrencyResponse.createFailureResponse(
                attemptedAmount,
                currentBalance,
                errorMessage
        );

        ADAPTER_LOGGER.log(Level.FINE, errorMessage);
        return failureResponse;
    }

    /**
     * Executes a currency transaction operation with comprehensive error handling, logging, and event firing.
 *
 * <p>This private helper method provides a unified way to execute both deposit and withdrawal
     * operations with consistent validation, error handling, database persistence, and event firing.
     * It reduces code duplication and ensures all transaction operations follow the same patterns
     * while integrating with the event system. Database logging is handled by creating CurrencyLog entities.
     *
     * @param userCurrencyEntity the UserCurrency entity to operate on, must not be null
     * @param transactionAmount the amount to process in the transaction
     * @param transactionOperation the operation function to execute (deposit or withdraw)
     * @param operationVerb the past tense verb describing the operation for logging
     * @param operationPreposition the preposition used in success/failure messages
     * @param changeType the type of balance change for event firing
     * @param reason the reason for the balance change
     * @param initiator the player who initiated this change (null if system/console)
     * @return a CurrencyResponse indicating the result of the transaction operation
     */
    private @NotNull CurrencyResponse executeTransactionOperation(
            final @NotNull UserCurrency userCurrencyEntity,
            final double transactionAmount,
            final @NotNull BiFunction<UserCurrency, Double, Boolean> transactionOperation,
            final @NotNull String operationVerb,
            final @NotNull String operationPreposition,
            final @NotNull EChangeType changeType,
            final @NotNull String reason,
            final @Nullable Player initiator
    ) {
        final double oldBalance = userCurrencyEntity.getBalance();
        final double newBalance = changeType == EChangeType.DEPOSIT ? oldBalance + transactionAmount : oldBalance - transactionAmount;

        final BalanceChangeEvent changeEvent = new BalanceChangeEvent(
                userCurrencyEntity.getPlayer(),
                userCurrencyEntity.getCurrency(),
                oldBalance,
                newBalance,
                changeType,
                reason,
                initiator
        );
        this.jexEconomyImpl.getPlatform().getScheduler().runSync(() -> {
            Bukkit.getPluginManager().callEvent(changeEvent);
        });

        if (changeEvent.isCancelled()) {
            final String cancelMessage = "Balance change cancelled: " + changeEvent.getCancelReason();
            ADAPTER_LOGGER.log(Level.FINE, cancelMessage);

            // Log the cancelled transaction attempt
            this.createTransactionLog(
                    userCurrencyEntity,
                    changeType,
                    oldBalance,
                    oldBalance, // No change occurred
                    transactionAmount,
                    reason,
                    initiator,
                    false,
                    cancelMessage
            );

            return this.createFailureResponse(transactionAmount, oldBalance, cancelMessage);
        }

        final boolean operationSuccessful = transactionOperation.apply(userCurrencyEntity, transactionAmount);

        if (operationSuccessful) {
            this.jexEconomyImpl.getUserCurrencyRepository().update(userCurrencyEntity);

            final BalanceChangedEvent changedEvent = new BalanceChangedEvent(
                    userCurrencyEntity.getPlayer(),
                    userCurrencyEntity.getCurrency(),
                    oldBalance,
                    userCurrencyEntity.getBalance(),
                    changeType,
                    reason,
                    initiator
            );
            this.jexEconomyImpl.getPlatform().getScheduler().runSync(() -> {
                Bukkit.getPluginManager().callEvent(changedEvent);
            });

            final String successMessage = String.format(
                    "Successfully %s %.2f %s %s currency account",
                    operationVerb,
                    transactionAmount,
                    operationPreposition,
                    userCurrencyEntity.getCurrency().getIdentifier()
            );

            // Log the successful transaction
            this.createTransactionLog(
                    userCurrencyEntity,
                    changeType,
                    oldBalance,
                    userCurrencyEntity.getBalance(),
                    transactionAmount,
                    reason,
                    initiator,
                    true,
                    null
            );

            final CurrencyResponse successResponse = CurrencyResponse.createSuccessfulResponse(
                    transactionAmount,
                    userCurrencyEntity.getBalance()
            );

            ADAPTER_LOGGER.log(Level.FINE, successMessage);
            return successResponse;
        } else {
            final String baseVerb = operationVerb.endsWith("ed") ?
                    operationVerb.substring(0, operationVerb.length() - 2) :
                    operationVerb;

            final String failureMessage = String.format(
                    "Failed to %s %.2f %s %s currency account - insufficient funds or validation error",
                    baseVerb,
                    transactionAmount,
                    operationPreposition,
                    userCurrencyEntity.getCurrency().getIdentifier()
            );

            // Log the failed transaction
            this.createTransactionLog(
                    userCurrencyEntity,
                    changeType,
                    oldBalance,
                    oldBalance, // No change occurred
                    transactionAmount,
                    reason,
                    initiator,
                    false,
                    failureMessage
            );

            final CurrencyResponse failureResponse = CurrencyResponse.createFailureResponse(
                    transactionAmount,
                    userCurrencyEntity.getBalance(),
                    failureMessage
            );

            ADAPTER_LOGGER.log(Level.FINE, failureMessage);
            return failureResponse;
        }
    }

    /**
     * Creates and persists a transaction log entry for currency operations.
 *
 * <p>This helper method creates CurrencyLog entities for all transaction operations,
     * providing a comprehensive audit trail in the database. The logging is performed
     * asynchronously to avoid blocking the main transaction flow.
     *
     * @param userCurrencyEntity the UserCurrency entity involved in the transaction
     * @param operationType the type of operation (DEPOSIT, WITHDRAW, etc.)
     * @param oldBalance the balance before the operation
     * @param newBalance the balance after the operation
     * @param amount the amount involved in the transaction
     * @param reason the reason for the transaction
     * @param initiator the player who initiated the transaction (null if system)
     * @param success whether the operation was successful
     * @param errorMessage error message if the operation failed (null if successful)
     */
    private void createTransactionLog(
            final @NotNull UserCurrency userCurrencyEntity,
            final @NotNull EChangeType operationType,
            final double oldBalance,
            final double newBalance,
            final double amount,
            final @Nullable String reason,
            final @Nullable Player initiator,
            final boolean success,
            final @Nullable String errorMessage
    ) {
        CompletableFuture.runAsync(() -> {
            try {
                final CurrencyLog transactionLog = new CurrencyLog(
                        userCurrencyEntity.getPlayer().getUniqueId(),
                        userCurrencyEntity.getPlayer().getPlayerName(),
                        userCurrencyEntity.getCurrency(),
                        operationType,
                        oldBalance,
                        newBalance,
                        amount,
                        String.format("%s operation via CurrencyAdapter", operationType.name().toLowerCase()),
                        reason
                );

                // Set initiator information if available
                if (initiator != null) {
                    transactionLog.setInitiatorUuid(initiator.getUniqueId());
                    transactionLog.setInitiatorName(initiator.getName());
                    transactionLog.setIpAddress(initiator.getAddress() != null ?
                            initiator.getAddress().getAddress().getHostAddress() : null);
                }

                // Set success status and error message
                transactionLog.setSuccess(success);
                if (errorMessage != null) {
                    transactionLog.setErrorMessage(errorMessage);
                }

                // Add additional metadata
                final String metadata = String.format(
                        "{\"operation_source\":\"CurrencyAdapter\",\"amount\":%.2f,\"currency_id\":%d,\"player_uuid\":\"%s\"}",
                        amount,
                        userCurrencyEntity.getCurrency().getId(),
                        userCurrencyEntity.getPlayer().getUniqueId()
                );
                transactionLog.setMetadata(metadata);

                // Save to database
                this.jexEconomyImpl.getCurrencyLogRepository().create(transactionLog);

            } catch (final Exception e) {
                ADAPTER_LOGGER.log(Level.WARNING,
                        "Failed to create transaction log for operation: " + operationType, e);
            }
        }, this.jexEconomyImpl.getExecutor());
    }

    /**
     * Creates and persists a management log entry for currency operations.
 *
 * <p>This helper method creates CurrencyLog entities for currency management operations
     * such as creation and deletion, providing administrative audit trails.
     *
     * @param currency the currency involved in the operation
     * @param operation the operation performed (e.g., "created", "deleted")
     * @param initiator the player who initiated the operation (null if system)
     * @param success whether the operation was successful
     * @param details additional details about the operation
     * @param errorMessage error message if the operation failed (null if successful)
     */
    private void createManagementLog(
            final @NotNull Currency currency,
            final @NotNull String operation,
            final @Nullable Player initiator,
            final boolean success,
            final @Nullable String details,
            final @Nullable String errorMessage
    ) {
        CompletableFuture.runAsync(() -> {
            try {
                final CurrencyLog managementLog = new CurrencyLog(
                        de.jexcellence.economy.type.ELogType.MANAGEMENT,
                        success ? de.jexcellence.economy.type.ELogLevel.INFO : de.jexcellence.economy.type.ELogLevel.WARNING,
                        String.format("Currency %s: %s", operation, currency.getIdentifier())
                );

                // Set currency information
                managementLog.setCurrency(currency);
                managementLog.setDetails(details);

                // Set initiator information if available
                if (initiator != null) {
                    managementLog.setInitiatorUuid(initiator.getUniqueId());
                    managementLog.setInitiatorName(initiator.getName());
                    managementLog.setIpAddress(initiator.getAddress() != null ?
                            initiator.getAddress().getAddress().getHostAddress() : null);
                }

                // Set success status and error message
                managementLog.setSuccess(success);
                if (errorMessage != null) {
                    managementLog.setErrorMessage(errorMessage);
                }

                // Add metadata
                final String metadata = String.format(
                        "{\"operation\":\"%s\",\"currency_id\":%d,\"currency_identifier\":\"%s\",\"source\":\"CurrencyAdapter\"}",
                        operation,
                        currency.getId(),
                        currency.getIdentifier()
                );
                managementLog.setMetadata(metadata);

                // Save to database
                this.jexEconomyImpl.getCurrencyLogRepository().create(managementLog);

            } catch (final Exception e) {
                ADAPTER_LOGGER.log(Level.WARNING,
                        "Failed to create management log for currency operation: " + operation, e);
            }
        }, this.jexEconomyImpl.getExecutor());
    }
    
    /**
     * Retrieves all available currencies in the system.
 *
 * <p>This method provides access to all currencies that have been registered in the
     * JExEconomy system. The returned map is keyed by currency ID and contains the
     * full Currency entity for each registered currency.
 *
 * <p>This is useful for external plugins that need to:
     * <ul>
     *   <li>List all available currencies</li>
     *   <li>Register currency bridges for requirement systems</li>
     *   <li>Provide currency selection interfaces</li>
     *   <li>Validate currency identifiers</li>
     * </ul>
     *
     * @return an unmodifiable map of currency ID to Currency entity
     * @since 1.0.0
     */
    @NotNull
    public Map<Long, Currency> getAllCurrencies() {
        return this.jexEconomyImpl.getCurrencies();
    }
    
    /**
     * Gets the currency repository for direct database access.
 *
 * <p>This method provides access to the underlying currency repository,
     * allowing direct queries to the database. Useful for loading currencies
     * before they are cached in memory.
     *
     * @return the currency repository
     * @since 2.0.1
     */
    @NotNull
    public CurrencyRepository getCurrencyRepository() {
        return this.jexEconomyImpl.getCurrencyRepository();
    }
}
