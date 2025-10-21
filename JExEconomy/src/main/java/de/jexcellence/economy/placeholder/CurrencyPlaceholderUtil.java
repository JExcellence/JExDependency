package de.jexcellence.economy.placeholder;

import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.UserCurrency;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Utility class for handling currency-related placeholders in the JExEconomyImpl system.
 * <p>
 * This class provides comprehensive methods to retrieve and format currency and player balance information
 * for use in placeholder systems, such as PlaceholderAPI or custom in-game formatting.
 * It supports various formatting styles, currency information retrieval, and seamless integration with
 * player and currency repositories.
 * </p>
 *
 * <h3>Core Functionality:</h3>
 * <ul>
 *   <li><strong>Currency Information:</strong> Retrieve currency symbols, prefixes, suffixes, and identifiers</li>
 *   <li><strong>Balance Formatting:</strong> Format player balances with multiple number format options</li>
 *   <li><strong>Placeholder Processing:</strong> Process dynamic placeholder strings for scoreboard and chat integration</li>
 *   <li><strong>Statistical Data:</strong> Calculate total currency in circulation and top player balances</li>
 * </ul>
 *
 * <h3>Supported Placeholder Formats:</h3>
 * <ul>
 *   <li><code>currency_{identifier}_{infoType}</code> - Currency information placeholders</li>
 *   <li><code>player_currency_{identifier}_{format}</code> - Raw player balance placeholders</li>
 *   <li><code>player_formatted_currency_{identifier}_{format}</code> - Fully formatted balance placeholders</li>
 * </ul>
 *
 * <h3>Number Formatting Options:</h3>
 * <ul>
 *   <li><strong>amount:</strong> Standard decimal format (e.g., 1234.56)</li>
 *   <li><strong>amount-rounded:</strong> Rounded to whole numbers (e.g., 1235)</li>
 *   <li><strong>amount-rounded-dots:</strong> German-style formatting with dots (e.g., 1.235)</li>
 * </ul>
 *
 * @author JExcellence
 * @see Currency
 * @see UserCurrency
 * @see Player
 */
public class CurrencyPlaceholderUtil {
	
	/**
	 * The main JExEconomyImpl plugin instance for accessing repositories and services.
	 */
	private final JExEconomyImpl jexEconomyImpl;
	
	/**
	 * German-style number formatter for displaying amounts with dot separators.
	 */
	private final NumberFormat germanStyleNumberFormat;
	
	/**
	 * Standard decimal formatter for displaying amounts with two decimal places.
	 */
	private final NumberFormat standardDecimalFormat;
	
	/**
	 * Constructs a new {@code CurrencyPlaceholderUtil} for the given plugin instance.
	 * <p>
	 * Initializes number formatters for German-style and standard decimal formatting.
	 * The German formatter uses dots as thousand separators with no decimal places,
	 * while the decimal formatter maintains two decimal places for precision.
	 * </p>
	 *
	 * @param jexEconomyImpl the main JExEconomyImpl plugin instance, must not be null
	 * @throws IllegalArgumentException if the plugin instance is null
	 */
	public CurrencyPlaceholderUtil(
		final @NotNull JExEconomyImpl jexEconomyImpl
	) {
		
		this.jexEconomyImpl = jexEconomyImpl;
		
		this.germanStyleNumberFormat = NumberFormat.getInstance(Locale.GERMANY);
		this.germanStyleNumberFormat.setMaximumFractionDigits(0);
		
		this.standardDecimalFormat = NumberFormat.getInstance();
		this.standardDecimalFormat.setMinimumFractionDigits(2);
		this.standardDecimalFormat.setMaximumFractionDigits(2);
	}
	
	/**
	 * Retrieves currency information based on the provided identifier and information type.
	 * <p>
	 * This method searches for a currency by its identifier (case-insensitive) and returns
	 * the requested information type. If the currency is not found or the information type
	 * is invalid, an empty string is returned.
	 * </p>
	 *
	 * <h3>Supported Information Types:</h3>
	 * <ul>
	 *   <li><strong>name:</strong> Returns the currency identifier</li>
	 *   <li><strong>symbol:</strong> Returns the currency symbol</li>
	 *   <li><strong>currencies:</strong> Returns prefix + identifier + suffix</li>
	 *   <li><strong>prefix:</strong> Returns the currency prefix</li>
	 *   <li><strong>suffix:</strong> Returns the currency suffix</li>
	 * </ul>
	 *
	 * @param currencyIdentifier the currency identifier to search for, must not be null
	 * @param requestedInfoType the type of information to retrieve, must not be null
	 * @return the requested currency information, or an empty string if not found or invalid type
	 * @throws IllegalArgumentException if any parameter is null
	 */
	public @NotNull String getCurrencyInfo(
		final @NotNull String currencyIdentifier,
		final @NotNull String requestedInfoType
	) {
		
		final Map<Long, Currency> availableCurrencies = this.jexEconomyImpl.getCurrencies();
		
		final Optional<Currency> targetCurrency = availableCurrencies.values().stream()
		                                                             .filter(currencyEntity -> currencyEntity.getIdentifier().equalsIgnoreCase(currencyIdentifier))
		                                                             .findFirst();
		
		return targetCurrency.map(foundCurrency -> switch (requestedInfoType) {
			case "name" -> foundCurrency.getIdentifier();
			case "symbol" -> foundCurrency.getSymbol();
			case "currencies" -> foundCurrency.getPrefix() + foundCurrency.getIdentifier() + foundCurrency.getSuffix();
			case "prefix" -> foundCurrency.getPrefix();
			case "suffix" -> foundCurrency.getSuffix();
			default -> "";
		}).orElse("");
	}
	
	/**
	 * Retrieves a player's currency balance with various formatting options.
	 * <p>
	 * This method searches for the specified currency and player combination, then formats
	 * the balance according to the requested format type. If the currency or player balance
	 * is not found, "N/A" is returned.
	 * </p>
	 *
	 * <h3>Supported Format Types:</h3>
	 * <ul>
	 *   <li><strong>amount:</strong> Standard decimal format with two decimal places</li>
	 *   <li><strong>amount-rounded:</strong> Rounded to whole numbers</li>
	 *   <li><strong>amount-rounded-dots:</strong> German-style formatting with dot separators</li>
	 * </ul>
	 *
	 * @param targetPlayerId the UUID of the player, must not be null
	 * @param currencyIdentifier the currency identifier (case-insensitive), must not be null
	 * @param formatType the format type for the balance display, must not be null
	 * @return the formatted balance, or "N/A" if not found
	 * @throws IllegalArgumentException if any parameter is null
	 */
	public @NotNull String getPlayerBalance(
		final @NotNull UUID targetPlayerId,
		final @NotNull String currencyIdentifier,
		final @NotNull String formatType
	) {
		
		final Optional<Currency> targetCurrency = this.jexEconomyImpl.getCurrencies().values().stream()
		                                                             .filter(currencyEntity -> currencyEntity.getIdentifier().equalsIgnoreCase(currencyIdentifier))
		                                                             .findFirst();
		
		if (
			targetCurrency.isEmpty()
		) {
			return "N/A";
		}
		
		final UserCurrency playerCurrencyAssociation = this.jexEconomyImpl.getUserCurrencyRepository().findByAttributes(
			Map.of(
				"player.uniqueId",
				targetPlayerId,
				"currency.id",
				targetCurrency.get().getId()
			)
		);
		
		if (
			playerCurrencyAssociation == null
		) {
			return "N/A";
		}
		
		final double currentBalance = playerCurrencyAssociation.getBalance();
		
		return switch (formatType.toLowerCase()) {
			case "amount" -> this.standardDecimalFormat.format(currentBalance);
			case "amount-rounded" -> String.format("%.0f", currentBalance);
			case "amount-rounded-dots" -> this.germanStyleNumberFormat.format(currentBalance);
			default -> String.format("%.2f", currentBalance);
		};
	}
	
	/**
	 * Retrieves a fully formatted player balance with currency symbols and formatting.
	 * <p>
	 * This method combines the player's balance with the currency's prefix, symbol, and suffix
	 * to create a complete formatted string. The balance is formatted according to the specified
	 * format type, and the result includes all currency display elements.
	 * </p>
	 *
	 * <h3>Format Structure:</h3>
	 * <p>
	 * The returned string follows the pattern: <code>prefix + formatted_amount + symbol + suffix</code>
	 * </p>
	 *
	 * @param targetPlayerId the UUID of the player, must not be null
	 * @param currencyIdentifier the currency identifier (case-insensitive), must not be null
	 * @param formatType the format type for the balance display, must not be null
	 * @return the fully formatted balance string, or "N/A" if not found
	 * @throws IllegalArgumentException if any parameter is null
	 */
	public @NotNull String getFormattedPlayerBalance(
		final @NotNull UUID targetPlayerId,
		final @NotNull String currencyIdentifier,
		final @NotNull String formatType
	) {
		
		final Optional<Currency> targetCurrency = this.jexEconomyImpl.getCurrencies().values().stream()
		                                                             .filter(currencyEntity -> currencyEntity.getIdentifier().equalsIgnoreCase(currencyIdentifier))
		                                                             .findFirst();
		
		if (
			targetCurrency.isEmpty()
		) {
			return "N/A";
		}
		
		final String formattedBalance = getPlayerBalance(
			targetPlayerId,
			currencyIdentifier,
			formatType
		);
		
		if (
			formattedBalance.equals("N/A")
		) {
			return "N/A";
		}
		
		final Currency foundCurrency = targetCurrency.get();
		return foundCurrency.getPrefix() + formattedBalance + foundCurrency.getSymbol() + foundCurrency.getSuffix();
	}
	
	/**
	 * Processes a placeholder string for a specific player.
	 * <p>
	 * This method serves as the main entry point for placeholder resolution, handling various
	 * placeholder formats and delegating to appropriate methods for processing. It supports
	 * currency information placeholders, player balance placeholders, and formatted balance placeholders.
	 * </p>
	 *
	 * <h3>Supported Placeholder Patterns:</h3>
	 * <ul>
	 *   <li><code>currency_{identifier}_{infoType}</code> - Currency information</li>
	 *   <li><code>player_currency_{identifier}_{format}</code> - Raw player balance</li>
	 *   <li><code>player_formatted_currency_{identifier}_{format}</code> - Formatted player balance</li>
	 * </ul>
	 *
	 * @param targetPlayer the player for whom the placeholder is being processed, can be null
	 * @param placeholderParams the placeholder parameters string, must not be null
	 * @return the processed placeholder value, or null if not applicable or invalid
	 * @throws IllegalArgumentException if placeholderParams is null
	 */
	public @Nullable String processPlaceholder(
		final @Nullable Player targetPlayer,
		final @NotNull String placeholderParams
	) {
		
		if (
			targetPlayer == null
		) {
			return null;
		}
		
		if (
			placeholderParams.startsWith("currency_")
		) {
			final String[] parameterParts = placeholderParams.split("_");
			if (
				parameterParts.length < 3
			) {
				return null;
			}
			
			final String currencyIdentifier = parameterParts[1];
			final String requestedInfoType = parameterParts[2];
			
			return getCurrencyInfo(currencyIdentifier, requestedInfoType);
		}
		
		if (
			placeholderParams.startsWith("player_currency_")
		) {
			final String[] parameterParts = placeholderParams.split("_");
			if (
				parameterParts.length < 4
			) {
				return null;
			}
			
			final String currencyIdentifier = parameterParts[2];
			final String formatType = parameterParts[3];
			
			return getPlayerBalance(
				targetPlayer.getUniqueId(),
				currencyIdentifier,
				formatType
			);
		}
		
		if (
			placeholderParams.startsWith("player_formatted_currency_")
		) {
			final String[] parameterParts = placeholderParams.split("_");
			if (
				parameterParts.length < 4
			) {
				return null;
			}
			
			final String currencyIdentifier = parameterParts[3];
			final String formatType = parameterParts.length > 4 ? parameterParts[4] : "amount";
			
			return getFormattedPlayerBalance(
				targetPlayer.getUniqueId(),
				currencyIdentifier,
				formatType
			);
		}
		
		return null;
	}
	
	/**
	 * Retrieves the top players for a specific currency.
	 * <p>
	 * This method is currently a stub implementation and returns an empty map.
	 * Future implementations will query the database for the highest balance holders
	 * of the specified currency and return their names mapped to their balances.
	 * </p>
	 *
	 * @param currencyIdentifier the currency identifier (case-insensitive), must not be null
	 * @param resultLimit the maximum number of players to return, must be positive
	 * @return a future that will complete with a map of player names to their balances
	 * @throws IllegalArgumentException if currencyIdentifier is null or resultLimit is not positive
	 */
	public @NotNull CompletableFuture<Map<String, Double>> findTopByCurrency(
		final @NotNull String currencyIdentifier,
		final int resultLimit
	) {
		
		if (
			resultLimit <= 0
		) {
			throw new IllegalArgumentException("Result limit must be positive");
		}
		
		return CompletableFuture.completedFuture(new HashMap<>());
	}
	
	/**
	 * Formats a raw balance amount according to the specified format type.
	 * <p>
	 * This utility method provides consistent number formatting across the placeholder system.
	 * It supports the same format types as the balance retrieval methods and can be used
	 * independently for formatting currency amounts.
	 * </p>
	 *
	 * <h3>Supported Format Types:</h3>
	 * <ul>
	 *   <li><strong>amount:</strong> Standard decimal format with two decimal places</li>
	 *   <li><strong>amount-rounded:</strong> Rounded to whole numbers</li>
	 *   <li><strong>amount-rounded-dots:</strong> German-style formatting with dot separators</li>
	 * </ul>
	 *
	 * @param rawAmount the raw balance amount to format
	 * @param formatType the format type to apply, must not be null
	 * @return the formatted balance string
	 * @throws IllegalArgumentException if formatType is null
	 */
	public @NotNull String formatAmount(
		final double rawAmount,
		final @NotNull String formatType
	) {
		
		return switch (formatType.toLowerCase()) {
			case "amount" -> this.standardDecimalFormat.format(rawAmount);
			case "amount-rounded" -> String.format("%.0f", rawAmount);
			case "amount-rounded-dots" -> this.germanStyleNumberFormat.format(rawAmount);
			default -> String.format("%.2f", rawAmount);
		};
	}
	
	/**
	 * Retrieves the total amount of a specific currency in circulation.
	 * <p>
	 * This method calculates the sum of all player balances for the specified currency.
	 * The calculation is performed asynchronously to prevent blocking the main thread.
	 * Currently, this is a stub implementation that returns 0.0.
	 * </p>
	 *
	 * <h3>Future Implementation:</h3>
	 * <p>
	 * The complete implementation will query the user-currency repository to sum all
	 * balances for the specified currency and return the total circulation amount.
	 * </p>
	 *
	 * @param currencyIdentifier the currency identifier (case-insensitive), must not be null
	 * @return a future that will complete with the total amount in circulation
	 * @throws IllegalArgumentException if currencyIdentifier is null
	 */
	public @NotNull CompletableFuture<Double> getTotalCurrencyInCirculation(
		final @NotNull String currencyIdentifier
	) {
		
		return CompletableFuture.supplyAsync(
			() -> {
				final Optional<Currency> targetCurrency = this.jexEconomyImpl.getCurrencies().values().stream()
				                                                             .filter(currencyEntity -> currencyEntity.getIdentifier().equalsIgnoreCase(currencyIdentifier))
				                                                             .findFirst();
				
				if (
					targetCurrency.isEmpty()
				) {
					return 0.0;
				}
				
				return 0.0;
			},
			this.jexEconomyImpl.getExecutor()
		);
	}
}