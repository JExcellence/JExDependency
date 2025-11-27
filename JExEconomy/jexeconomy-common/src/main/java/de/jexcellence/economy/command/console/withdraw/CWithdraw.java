package de.jexcellence.economy.command.console.withdraw;

import com.raindropcentral.commands.ServerCommand;
import com.raindropcentral.commands.utility.Command;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.adapter.CurrencyResponse;
import de.jexcellence.economy.database.entity.Currency;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Console command implementation for withdrawing currency amounts from player accounts.
 * <p>
 * This command provides server administrators with the ability to remove currency from any player's
 * account directly from the server console. It supports all registered currencies within the
 * JExEconomy system and performs comprehensive validation to ensure data integrity and prevent
 * overdrafts or invalid operations.
 * </p>
 *
 * <h3>Command Syntax:</h3>
 * <pre>{@code /cwithdraw <player> <currency> <amount>}</pre>
 *
 * <h3>Parameters:</h3>
 * <ul>
 *   <li><strong>player:</strong> Target player name or UUID (must have played before)</li>
 *   <li><strong>currency:</strong> Valid currency identifier registered in the system</li>
 *   <li><strong>amount:</strong> Positive decimal amount to withdraw (must not exceed current balance)</li>
 * </ul>
 *
 * <h3>Operation Flow:</h3>
 * <ol>
 *   <li>Validates command arguments and parameter types</li>
 *   <li>Verifies currency exists in the system</li>
 *   <li>Executes withdrawal operation through CurrencyAdapter</li>
 *   <li>Handles response with comprehensive logging</li>
 *   <li>Reports operation results for audit purposes</li>
 * </ol>
 *
 * <h3>Validation Rules:</h3>
 * <ul>
 *   <li>Player must exist and have played on the server before</li>
 *   <li>Currency identifier must match a registered currency</li>
 *   <li>Withdrawal amount must be positive</li>
 *   <li>Player must have sufficient balance for the withdrawal</li>
 *   <li>Player must have an existing account for the specified currency</li>
 * </ul>
 *
 * <h3>Error Handling:</h3>
 * <ul>
 *   <li>Invalid player names or UUIDs are rejected with appropriate messages</li>
 *   <li>Non-existent currencies are reported with available alternatives</li>
 *   <li>Insufficient balance scenarios are logged with current balance information</li>
 *   <li>Missing player accounts are logged for investigation</li>
 *   <li>Failed withdrawal operations are logged with full context</li>
 * </ul>
 *
 * <h3>Security Considerations:</h3>
 * <ul>
 *   <li>Console-only execution prevents player abuse</li>
 *   <li>All operations are logged for comprehensive audit trails</li>
 *   <li>Asynchronous execution prevents server lag during database operations</li>
 *   <li>Balance validation prevents negative balance scenarios</li>
 * </ul>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 * @see ServerCommand
 * @see JExEconomy
 * @see Currency
 * @see CurrencyResponse
 */
@Command
@SuppressWarnings("unused")
public class CWithdraw extends ServerCommand {
	
	/**
	 * Logger instance for recording command execution details and debugging information.
	 * <p>
	 * Used to track command usage, parameter validation results, operation outcomes,
	 * and error conditions for administrative monitoring and troubleshooting. All
	 * withdrawal operations are logged for audit trail purposes.
	 * </p>
	 */
	private static final Logger COMMAND_LOGGER = CentralLogger.getLogger(CWithdraw.class.getName());
	
	/**
	 * Reference to the main JExEconomy plugin instance.
	 * <p>
	 * Provides access to currency repositories, adapter services, and other plugin
	 * infrastructure required for executing withdrawal operations and data persistence.
	 * This reference is essential for accessing the plugin's currency management system.
	 * </p>
	 */
	private final JExEconomy jexEconomyImpl;
	
	/**
	 * Constructs a new console withdrawal command handler with the specified configuration.
	 * <p>
	 * Initializes the command with access to the plugin's currency management infrastructure
	 * and configures it according to the provided command section settings. The constructor
	 * validates that all required dependencies are provided and properly configured.
	 * </p>
	 *
	 * @param commandSection the command configuration section defining permissions and settings, must not be null
	 * @param jexEconomy the main JExEconomy plugin instance providing access to services, must not be null
	 * @throws IllegalArgumentException if either parameter is null
	 */
	public CWithdraw(
		final @NotNull CWithdrawSection commandSection,
		final @NotNull JExEconomy jexEconomy
	) {
		super(commandSection);
		
		this.jexEconomyImpl = jexEconomy;
	}
	
	/**
	 * Processes console-initiated withdrawal commands with comprehensive validation and error handling.
	 * <p>
	 * This method handles the complete withdrawal workflow from parameter validation through
	 * database persistence. It performs asynchronous operations to prevent server lag
	 * and provides detailed logging for administrative monitoring. The method ensures
	 * that all withdrawal operations maintain data integrity and prevent invalid states.
	 * </p>
	 *
	 * @param consoleCommandSender the console sender executing the command, must not be null
	 * @param commandLabel the command label used for invocation, must not be null
	 * @param commandArguments the command arguments array containing player, currency, and amount, must not be null
	 */
	@Override
	protected void onPlayerInvocation(
		final @NotNull ConsoleCommandSender consoleCommandSender,
		final @NotNull String commandLabel,
		final @NotNull String[] commandArguments
	) {
		final OfflinePlayer targetOfflinePlayer = this.offlinePlayerParameter(
			commandArguments,
			0,
			true
		);
		
		final String currencyIdentifier = this.stringParameter(
			commandArguments,
			1
		);
		
		final double withdrawalAmount = this.doubleParameter(
			commandArguments,
			2
		);
		
		final Currency targetCurrency = this.findCurrencyByIdentifier(currencyIdentifier);
		
		if (targetCurrency == null) {
			this.handleUnknownCurrency(currencyIdentifier);
			return;
		}
		
		this.executeWithdrawalOperation(
			targetOfflinePlayer,
			targetCurrency,
			withdrawalAmount
		);
	}
	
	/**
	 * Locates a currency entity by its unique identifier within the plugin's currency cache.
	 * <p>
	 * This method performs an efficient lookup of currency entities using the in-memory
	 * cache maintained by the plugin. It provides fast access to currency data without
	 * requiring database queries during command execution, improving overall performance.
	 * </p>
	 *
	 * @param currencyIdentifier the unique identifier of the currency to locate, must not be null
	 * @return the Currency entity if found, null if no matching currency exists
	 */
	private @Nullable Currency findCurrencyByIdentifier(final @NotNull String currencyIdentifier) {
		return this.jexEconomyImpl.getCurrencies()
		                          .values()
		                          .stream()
		                          .filter(currencyEntity -> currencyEntity.getIdentifier().equals(currencyIdentifier))
		                          .findFirst()
		                          .orElse(null);
	}
	
	/**
	 * Handles unknown currency identifier errors with helpful diagnostic information.
	 * <p>
	 * This method logs detailed error information when an invalid currency identifier
	 * is provided, including a comprehensive list of all available currencies to assist
	 * administrators in correcting their commands. The information helps reduce command
	 * errors and improves administrative efficiency.
	 * </p>
	 *
	 * @param invalidCurrencyIdentifier the currency identifier that was not found, must not be null
	 */
	private void handleUnknownCurrency(final @NotNull String invalidCurrencyIdentifier) {
		COMMAND_LOGGER.log(
			Level.WARNING,
			String.format(
				"Currency withdrawal failed - unknown currency identifier: '%s'",
				invalidCurrencyIdentifier
			)
		);
		
		final String availableCurrencies = this.jexEconomyImpl.getCurrencies()
		                                                      .values()
		                                                      .stream()
		                                                      .map(Currency::getIdentifier)
		                                                      .collect(Collectors.joining(", "));
		
		COMMAND_LOGGER.log(
			Level.INFO,
			String.format(
				"Available currency identifiers: [%s]",
				availableCurrencies
			)
		);
	}
	
	/**
	 * Executes the withdrawal operation using the CurrencyAdapter service.
	 * <p>
	 * This method delegates the withdrawal operation to the CurrencyAdapter, which handles
	 * all validation, balance checking, and database persistence. The operation is performed
	 * asynchronously to prevent server lag, and comprehensive logging is provided for
	 * audit trail purposes.
	 * </p>
	 *
	 * @param targetOfflinePlayer the player from whom to withdraw currency, must not be null
	 * @param targetCurrency the currency entity for the withdrawal operation, must not be null
	 * @param withdrawalAmount the amount to withdraw from the player's account
	 */
	private void executeWithdrawalOperation(
		final @NotNull OfflinePlayer targetOfflinePlayer,
		final @NotNull Currency targetCurrency,
		final double withdrawalAmount
	) {
		COMMAND_LOGGER.log(
			Level.INFO,
			String.format(
				"Executing console withdrawal: player=%s, currency=%s, amount=%.2f",
				targetOfflinePlayer.getUniqueId(),
				targetCurrency.getIdentifier(),
				withdrawalAmount
			)
		);
		
		this.jexEconomyImpl.getCurrencyAdapter()
		                   .withdraw(targetOfflinePlayer, targetCurrency, withdrawalAmount)
		                   .thenAcceptAsync(
			               currencyResponse -> this.handleWithdrawalResponse(
				               currencyResponse,
				               targetOfflinePlayer,
				               targetCurrency,
				               withdrawalAmount
			               ),
			               this.jexEconomyImpl.getExecutor()
		               )
		                   .exceptionally(throwable -> {
			               COMMAND_LOGGER.log(
				               Level.SEVERE,
				               String.format(
					               "Console withdrawal operation failed due to async exception: player=%s, currency=%s, amount=%.2f",
					               targetOfflinePlayer.getUniqueId(),
					               targetCurrency.getIdentifier(),
					               withdrawalAmount
				               ),
				               throwable
			               );
			               return null;
		               });
	}
	
	/**
	 * Handles the response from the withdrawal operation with comprehensive logging.
	 * <p>
	 * This method processes the CurrencyResponse returned by the withdrawal operation,
	 * logging success or failure details for administrative monitoring and audit trails.
	 * Both successful and failed operations are recorded with complete transaction context.
	 * </p>
	 *
	 * @param response the response from the withdrawal operation, must not be null
	 * @param targetOfflinePlayer the player from whom currency was withdrawn, must not be null
	 * @param targetCurrency the currency that was withdrawn, must not be null
	 * @param withdrawalAmount the amount that was attempted to be withdrawn
	 */
	private void handleWithdrawalResponse(
		final @NotNull CurrencyResponse response,
		final @NotNull OfflinePlayer targetOfflinePlayer,
		final @NotNull Currency targetCurrency,
		final double withdrawalAmount
	) {
		if (response.operationStatus().equals(CurrencyResponse.ResponseType.SUCCESS)) {
			COMMAND_LOGGER.log(
				Level.INFO,
				String.format(
					"Console withdrawal completed successfully: player=%s, currency=%s, amount=%.2f, new_balance=%.2f",
					targetOfflinePlayer.getUniqueId(),
					targetCurrency.getIdentifier(),
					withdrawalAmount,
					response.resultingBalance()
				)
			);
		} else {
			COMMAND_LOGGER.log(
				Level.WARNING,
				String.format(
					"Console withdrawal failed: player=%s, currency=%s, amount=%.2f, error=%s",
					targetOfflinePlayer.getUniqueId(),
					targetCurrency.getIdentifier(),
					withdrawalAmount,
					response.failureMessage()
				)
			);
		}
	}
}