package de.jexcellence.economy.adapter;

import de.jexcellence.economy.adapter.CurrencyResponse;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.User;
import de.jexcellence.economy.database.entity.UserCurrency;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Core interface defining the contract for currency management operations within the JExEconomyImpl system.
 * <p>
 * This interface serves as the primary abstraction layer for all currency-related operations,
 * providing a comprehensive API for balance management, transaction processing, and entity
 * lifecycle management. All operations are designed to be asynchronous to prevent blocking
 * the main server thread during database operations.
 * </p>
 *
 * <h3>Core Functionality Areas:</h3>
 * <ul>
 *   <li><strong>Balance Operations:</strong> Retrieve player balances for specific currencies</li>
 *   <li><strong>Transaction Management:</strong> Handle deposits and withdrawals with validation</li>
 *   <li><strong>Entity Management:</strong> Create and manage players, currencies, and relationships</li>
 *   <li><strong>Query Operations:</strong> Retrieve currency data and player associations</li>
 *   <li><strong>Validation Services:</strong> Check currency existence and data integrity</li>
 * </ul>
 *
 * <h3>Design Principles:</h3>
 * <ul>
 *   <li><strong>Asynchronous Operations:</strong> All methods return CompletableFuture for non-blocking execution</li>
 *   <li><strong>Type Safety:</strong> Comprehensive use of @NotNull and @Nullable annotations</li>
 *   <li><strong>Flexibility:</strong> Support for both entity-based and player-based operations</li>
 *   <li><strong>Consistency:</strong> Standardized response patterns using CurrencyResponse</li>
 * </ul>
 *
 * <h3>Implementation Notes:</h3>
 * <p>
 * Implementations of this interface should ensure thread safety, proper error handling,
 * and efficient database access patterns. All monetary operations should include appropriate
 * validation to prevent negative balances and ensure data integrity.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 * @see CurrencyResponse
 * @see Currency
 * @see UserCurrency
 * @see User
 */
public interface ICurrencyAdapter {
	
	/**
	 * Retrieves the current balance of a player for a specific currency.
	 * <p>
	 * This method performs an asynchronous lookup of the player's balance in the specified
	 * currency. If the player has no account for the given currency, the balance will be 0.0.
	 * The operation is safe for offline players and will not cause server lag.
	 * </p>
	 *
	 * <h3>Behavior:</h3>
	 * <ul>
	 *   <li>Returns 0.0 if the player has no account for the currency</li>
	 *   <li>Returns the actual balance if an account exists</li>
	 *   <li>Handles both online and offline players seamlessly</li>
	 *   <li>Performs database lookup asynchronously</li>
	 * </ul>
	 *
	 * @param targetOfflinePlayer the player whose balance should be retrieved, must not be null
	 * @param targetCurrency the currency for which to retrieve the balance, must not be null
	 * @return a CompletableFuture containing the player's balance as a Double value
	 * @throws IllegalArgumentException if either parameter is null
	 */
	@NotNull CompletableFuture<Double> getBalance(
		final @NotNull OfflinePlayer targetOfflinePlayer,
		final @NotNull Currency targetCurrency
	);
	
	/**
	 * Retrieves the balance from an existing UserCurrency entity.
	 * <p>
	 * This method provides direct access to the balance stored in a UserCurrency entity.
	 * It's more efficient than the player-based method when you already have the
	 * UserCurrency entity available, as it avoids additional database lookups.
	 * </p>
	 *
	 * <h3>Use Cases:</h3>
	 * <ul>
	 *   <li>When processing multiple currencies for the same player</li>
	 *   <li>During batch operations or reporting</li>
	 *   <li>When the UserCurrency entity is already loaded</li>
	 *   <li>For performance-critical balance checks</li>
	 * </ul>
	 *
	 * @param userCurrencyEntity the UserCurrency entity containing the balance, must not be null
	 * @return a CompletableFuture containing the balance as a Double value
	 * @throws IllegalArgumentException if the userCurrencyEntity is null
	 */
	@NotNull CompletableFuture<Double> getBalance(
		final @NotNull UserCurrency userCurrencyEntity
	);
	
	/**
	 * Deposits a specified amount of currency into a player's account.
	 * <p>
	 * This method performs a secure deposit operation, adding the specified amount to the
	 * player's existing balance for the given currency. The operation includes validation
	 * to ensure the amount is positive and the currency is valid. If the player doesn't
	 * have an account for the currency, one will be created automatically.
	 * </p>
	 *
	 * <h3>Operation Details:</h3>
	 * <ul>
	 *   <li>Validates that the deposit amount is positive</li>
	 *   <li>Creates player account if it doesn't exist</li>
	 *   <li>Updates the balance atomically to prevent race conditions</li>
	 *   <li>Returns detailed response with new balance and operation status</li>
	 * </ul>
	 *
	 * <h3>Error Conditions:</h3>
	 * <ul>
	 *   <li>Negative or zero amounts will result in failure response</li>
	 *   <li>Invalid currency will result in failure response</li>
	 *   <li>Database errors will be captured in the response</li>
	 * </ul>
	 *
	 * @param targetOfflinePlayer the player to deposit currency to, must not be null
	 * @param targetCurrency the currency to deposit, must not be null
	 * @param depositAmount the amount to deposit, must be positive
	 * @return a CompletableFuture containing a CurrencyResponse with operation details
	 * @throws IllegalArgumentException if player or currency is null, or amount is not positive
	 */
	@NotNull CompletableFuture<CurrencyResponse> deposit(
		final @NotNull OfflinePlayer targetOfflinePlayer,
		final @NotNull Currency targetCurrency,
		final double depositAmount
	);
	
	/**
	 * Deposits a specified amount into an existing UserCurrency entity.
	 * <p>
	 * This method provides direct deposit functionality for UserCurrency entities,
	 * offering better performance when the entity is already available. The operation
	 * updates the entity's balance and persists the changes to the database.
	 * </p>
	 *
	 * <h3>Performance Benefits:</h3>
	 * <ul>
	 *   <li>Avoids additional entity lookups</li>
	 *   <li>Reduces database round trips</li>
	 *   <li>Optimal for batch operations</li>
	 *   <li>Direct entity manipulation</li>
	 * </ul>
	 *
	 * @param userCurrencyEntity the UserCurrency entity to deposit to, must not be null
	 * @param depositAmount the amount to deposit, must be positive
	 * @return a CompletableFuture containing a CurrencyResponse with operation details
	 * @throws IllegalArgumentException if entity is null or amount is not positive
	 */
	@NotNull CompletableFuture<CurrencyResponse> deposit(
		final @NotNull UserCurrency userCurrencyEntity,
		final double depositAmount
	);
	
	/**
	 * Withdraws a specified amount of currency from a player's account.
	 * <p>
	 * This method performs a secure withdrawal operation, removing the specified amount
	 * from the player's balance for the given currency. The operation includes validation
	 * to ensure sufficient funds are available and the amount is positive. Insufficient
	 * funds will result in a failure response without modifying the balance.
	 * </p>
	 *
	 * <h3>Validation Checks:</h3>
	 * <ul>
	 *   <li>Verifies withdrawal amount is positive</li>
	 *   <li>Confirms sufficient balance exists</li>
	 *   <li>Ensures player account exists for the currency</li>
	 *   <li>Prevents negative balance scenarios</li>
	 * </ul>
	 *
	 * <h3>Transaction Safety:</h3>
	 * <ul>
	 *   <li>Atomic balance updates prevent race conditions</li>
	 *   <li>Rollback capability on database errors</li>
	 *   <li>Detailed error reporting in response</li>
	 *   <li>Balance verification before and after operation</li>
	 * </ul>
	 *
	 * @param targetOfflinePlayer the player to withdraw currency from, must not be null
	 * @param targetCurrency the currency to withdraw, must not be null
	 * @param withdrawalAmount the amount to withdraw, must be positive
	 * @return a CompletableFuture containing a CurrencyResponse with operation details
	 * @throws IllegalArgumentException if player or currency is null, or amount is not positive
	 */
	@NotNull CompletableFuture<CurrencyResponse> withdraw(
		final @NotNull OfflinePlayer targetOfflinePlayer,
		final @NotNull Currency targetCurrency,
		final double withdrawalAmount
	);
	
	/**
	 * Withdraws a specified amount from an existing UserCurrency entity.
	 * <p>
	 * This method provides direct withdrawal functionality for UserCurrency entities,
	 * offering optimal performance when the entity is already loaded. The operation
	 * validates sufficient funds and updates the entity balance atomically.
	 * </p>
	 *
	 * <h3>Efficiency Features:</h3>
	 * <ul>
	 *   <li>Direct entity access without lookups</li>
	 *   <li>Reduced database operations</li>
	 *   <li>Immediate balance validation</li>
	 *   <li>Streamlined transaction processing</li>
	 * </ul>
	 *
	 * @param userCurrencyEntity the UserCurrency entity to withdraw from, must not be null
	 * @param withdrawalAmount the amount to withdraw, must be positive
	 * @return a CompletableFuture containing a CurrencyResponse with operation details
	 * @throws IllegalArgumentException if entity is null or amount is not positive
	 */
	@NotNull CompletableFuture<CurrencyResponse> withdraw(
		final @NotNull UserCurrency userCurrencyEntity,
		final double withdrawalAmount
	);
	
	/**
	 * Creates a new player entity in the currency system.
	 * <p>
	 * This method initializes a new player record in the database, creating the necessary
	 * foundation for currency operations. The player entity stores essential information
	 * such as UUID and current name, and serves as the basis for all currency accounts.
	 * </p>
	 *
	 * <h3>Creation Process:</h3>
	 * <ul>
	 *   <li>Extracts player UUID and current name</li>
	 *   <li>Checks for existing player records</li>
	 *   <li>Creates new User entity if none exists</li>
	 *   <li>Updates existing records if player name changed</li>
	 * </ul>
	 *
	 * <h3>Idempotent Behavior:</h3>
	 * <p>
	 * This method is designed to be idempotent - calling it multiple times with the same
	 * player will not create duplicate records but may update the player's current name
	 * if it has changed since the last update.
	 * </p>
	 *
	 * @param targetOfflinePlayer the player for whom to create an entity, must not be null
	 * @return a CompletableFuture containing true if creation was successful, false otherwise
	 * @throws IllegalArgumentException if the targetOfflinePlayer is null
	 */
	@NotNull CompletableFuture<Boolean> createPlayer(
		final @NotNull OfflinePlayer targetOfflinePlayer
	);
	
	/**
	 * Creates a new currency entity in the system.
	 * <p>
	 * This method registers a new currency type in the database, making it available
	 * for use throughout the system. The currency entity contains all configuration
	 * information including symbol, prefix, suffix, and display properties.
	 * </p>
	 *
	 * <h3>Validation Requirements:</h3>
	 * <ul>
	 *   <li>Currency identifier must be unique</li>
	 *   <li>Symbol must not be empty</li>
	 *   <li>All required fields must be populated</li>
	 *   <li>Icon material must be valid</li>
	 * </ul>
	 *
	 * <h3>Post-Creation Effects:</h3>
	 * <ul>
	 *   <li>Currency becomes available for player accounts</li>
	 *   <li>Automatic cache updates occur</li>
	 *   <li>System-wide currency list is refreshed</li>
	 *   <li>Events may be triggered for other plugins</li>
	 * </ul>
	 *
	 * @param newCurrencyEntity the currency entity to create, must not be null and must be valid
	 * @return a CompletableFuture containing true if creation was successful, false otherwise
	 * @throws IllegalArgumentException if the newCurrencyEntity is null or invalid
	 */
	@NotNull CompletableFuture<Boolean> createCurrency(
		final @NotNull Currency newCurrencyEntity
	);
	
	/**
	 * Checks whether a currency with the specified identifier exists in the system.
	 * <p>
	 * This method performs a lookup to determine if a currency with the given identifier
	 * is registered in the database. It's useful for validation before performing
	 * operations that require a specific currency to exist.
	 * </p>
	 *
	 * <h3>Lookup Behavior:</h3>
	 * <ul>
	 *   <li>Performs case-sensitive identifier matching</li>
	 *   <li>Checks both active and inactive currencies</li>
	 *   <li>Uses efficient caching when available</li>
	 *   <li>Returns false for null or empty identifiers</li>
	 * </ul>
	 *
	 * <h3>Common Use Cases:</h3>
	 * <ul>
	 *   <li>Validating user input in commands</li>
	 *   <li>Preventing duplicate currency creation</li>
	 *   <li>Configuration validation</li>
	 *   <li>API parameter validation</li>
	 * </ul>
	 *
	 * @param currencyIdentifier the identifier of the currency to check, may be null
	 * @return a CompletableFuture containing true if the currency exists, false otherwise
	 */
	@NotNull CompletableFuture<Boolean> hasGivenCurrency(
		final @Nullable String currencyIdentifier
	);
	
	/**
	 * Creates a new player-currency relationship entity.
	 * <p>
	 * This method establishes a connection between a player and a currency by creating
	 * a UserCurrency entity. This entity represents the player's account for that specific
	 * currency and tracks their balance and transaction history.
	 * </p>
	 *
	 * <h3>Relationship Creation:</h3>
	 * <ul>
	 *   <li>Links existing User and Currency entities</li>
	 *   <li>Initializes balance to zero</li>
	 *   <li>Sets up transaction tracking</li>
	 *   <li>Enables currency operations for the player</li>
	 * </ul>
	 *
	 * <h3>Prerequisites:</h3>
	 * <ul>
	 *   <li>Both User and Currency entities must exist</li>
	 *   <li>No existing relationship should exist (prevents duplicates)</li>
	 *   <li>Both entities must be in valid state</li>
	 * </ul>
	 *
	 * @param targetPlayerEntity the User entity representing the player, must not be null
	 * @param targetCurrencyEntity the Currency entity to associate with the player, must not be null
	 * @return a CompletableFuture containing true if creation was successful, false otherwise
	 * @throws IllegalArgumentException if either entity is null
	 */
	@NotNull CompletableFuture<Boolean> createPlayerCurrency(
		final @NotNull User targetPlayerEntity,
		final @NotNull Currency targetCurrencyEntity
	);
	
	/**
	 * Retrieves all currency accounts associated with a specific player.
	 * <p>
	 * This method returns a comprehensive list of all UserCurrency entities for the
	 * specified player, providing access to all their currency accounts and balances.
	 * The list includes all currencies the player has accounts for, regardless of
	 * whether the balance is zero or positive.
	 * </p>
	 *
	 * <h3>Return Data:</h3>
	 * <ul>
	 *   <li>All UserCurrency entities for the player</li>
	 *   <li>Current balance for each currency</li>
	 *   <li>Currency metadata and configuration</li>
	 *   <li>Account creation timestamps</li>
	 * </ul>
	 *
	 * <h3>Performance Considerations:</h3>
	 * <ul>
	 *   <li>Results may be cached for frequently accessed players</li>
	 *   <li>Large currency lists may impact performance</li>
	 *   <li>Consider pagination for systems with many currencies</li>
	 * </ul>
	 *
	 * @param targetOfflinePlayer the player whose currency accounts should be retrieved, must not be null
	 * @return a CompletableFuture containing a List of UserCurrency entities, may be empty but never null
	 * @throws IllegalArgumentException if targetOfflinePlayer is null
	 */
	@NotNull CompletableFuture<List<UserCurrency>> getUserCurrencies(
		final @NotNull OfflinePlayer targetOfflinePlayer
	);
	
	/**
	 * Retrieves a specific currency account for a player by currency identifier.
	 * <p>
	 * This method performs a targeted lookup to find the UserCurrency entity that
	 * represents the specified player's account for the named currency. It's more
	 * efficient than retrieving all currencies when you only need one specific account.
	 * </p>
	 *
	 * <h3>Lookup Process:</h3>
	 * <ul>
	 *   <li>Validates currency identifier exists</li>
	 *   <li>Searches for player's account for that currency</li>
	 *   <li>Returns null if no account exists</li>
	 *   <li>Includes full entity data when found</li>
	 * </ul>
	 *
	 * <h3>Null Return Conditions:</h3>
	 * <ul>
	 *   <li>Currency identifier doesn't exist in system</li>
	 *   <li>Player has no account for the specified currency</li>
	 *   <li>Player entity doesn't exist in database</li>
	 * </ul>
	 *
	 * @param targetOfflinePlayer the player whose currency account should be retrieved, must not be null
	 * @param currencyIdentifier the identifier of the currency to retrieve, may be null
	 * @return a CompletableFuture containing the UserCurrency entity, or null if not found
	 * @throws IllegalArgumentException if targetOfflinePlayer is null
	 */
	@NotNull CompletableFuture<@Nullable UserCurrency> getUserCurrency(
		final @NotNull OfflinePlayer targetOfflinePlayer,
		final @Nullable String currencyIdentifier
	);
}