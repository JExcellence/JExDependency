package de.jexcellence.economy.command.console.deposit;

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
 * Console command implementation for depositing currency amounts into player accounts.
 * <p>
 * This command provides server administrators with the ability to add currency to any player's
 * account directly from the server console. It supports all registered currencies within the
 * JExEconomy system and performs comprehensive validation to ensure data integrity.
 * </p>
 *
 * <h3>Command Syntax:</h3>
 * <pre>{@code /cdeposit <player> <currency> <amount>}</pre>
 *
 * <h3>Parameters:</h3>
 * <ul>
 *   <li><strong>player:</strong> Target player name or UUID (must have played before)</li>
 *   <li><strong>currency:</strong> Valid currency identifier registered in the system</li>
 *   <li><strong>amount:</strong> Positive decimal amount to deposit</li>
 * </ul>
 *
 * <h3>Operation Flow:</h3>
 * <ol>
 *   <li>Validates command arguments and parameter types</li>
 *   <li>Verifies currency exists in the system</li>
 *   <li>Uses CurrencyAdapter for proper deposit operation with event firing</li>
 *   <li>Handles response and logs operation results</li>
 * </ol>
 *
 * <h3>Error Handling:</h3>
 * <ul>
 *   <li>Invalid player names or UUIDs are rejected</li>
 *   <li>Non-existent currencies are reported with available alternatives</li>
 *   <li>Failed deposit operations are logged with context</li>
 * </ul>
 *
 * <h3>Security Considerations:</h3>
 * <ul>
 *   <li>Console-only execution prevents player abuse</li>
 *   <li>All operations are logged via CurrencyAdapter events</li>
 *   <li>Asynchronous execution prevents server lag</li>
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
public class
CDeposit extends ServerCommand {
	
	/**
	 * Logger instance for recording command execution details and debugging information.
	 * <p>
	 * Used to track command usage, parameter validation results, operation outcomes,
	 * and error conditions for administrative monitoring and troubleshooting.
	 * </p>
	 */
	private static final Logger COMMAND_LOGGER = CentralLogger.getLoggerByName("JExEconomy");
	
	/**
	 * Reference to the main JExEconomy plugin instance.
	 * <p>
	 * Provides access to currency repositories, adapter services, and other plugin
	 * infrastructure required for executing deposit operations and data persistence.
	 * </p>
	 */
	private final JExEconomy jexEconomyImpl;
	
	/**
	 * Constructs a new console deposit command handler with the specified configuration.
	 * <p>
	 * Initializes the command with access to the plugin's currency management infrastructure
	 * and configures it according to the provided command section settings.
	 * </p>
	 *
	 * @param commandSection the command configuration section defining permissions and settings, must not be null
	 * @param jexEconomy the main JExEconomy plugin instance providing access to services, must not be null
	 * @throws IllegalArgumentException if either parameter is null
	 */
	public CDeposit(
		final @NotNull CDepositSection commandSection,
		final @NotNull JExEconomy jexEconomy
	) {
		super(commandSection);
		this.jexEconomyImpl = jexEconomy;
	}
	
	/**
	 * Processes console-initiated deposit commands with comprehensive validation and error handling.
	 * <p>
	 * This method handles the complete deposit workflow from parameter validation through
	 * the CurrencyAdapter service. It performs asynchronous operations to prevent server lag
	 * and provides detailed logging for administrative monitoring.
	 * </p>
	 *
	 * <h3>Parameter Validation:</h3>
	 * <ul>
	 *   <li>Validates player existence and play history</li>
	 *   <li>Confirms currency identifier matches registered currencies</li>
	 *   <li>Ensures deposit amount is positive and valid</li>
	 * </ul>
	 *
	 * <h3>Asynchronous Processing:</h3>
	 * <ul>
	 *   <li>Deposit operation executed through CurrencyAdapter</li>
	 *   <li>Events are fired automatically for logging</li>
	 *   <li>Database updates handled without blocking main thread</li>
	 * </ul>
	 *
	 * @param consoleCommandSender the console sender executing the command, must not be null
	 * @param commandLabel the command label used for invocation, must not be null
	 * @param commandArguments the command arguments array containing player, currency, and amount, must not be null
	 * @throws IllegalArgumentException if required parameters are invalid or missing
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
		
		final double depositAmount = this.doubleParameter(
			commandArguments,
			2
		);
		
		final Currency targetCurrency = this.findCurrencyByIdentifier(currencyIdentifier);
		
		if (targetCurrency == null) {
			this.handleUnknownCurrency(currencyIdentifier);
			return;
		}
		
		this.executeDepositOperation(
			targetOfflinePlayer,
			targetCurrency,
			depositAmount
		);
	}
	
	/**
	 * Locates a currency entity by its unique identifier within the plugin's currency cache.
	 * <p>
	 * This method performs an efficient lookup of currency entities using the in-memory
	 * cache maintained by the plugin. It provides fast access to currency data without
	 * requiring database queries during command execution.
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
	 * is provided, including a list of all available currencies to assist administrators
	 * in correcting their commands.
	 * </p>
	 *
	 * @param invalidCurrencyIdentifier the currency identifier that was not found, must not be null
	 */
	private void handleUnknownCurrency(final @NotNull String invalidCurrencyIdentifier) {
		COMMAND_LOGGER.log(
			Level.WARNING,
			String.format(
				"Currency deposit failed - unknown currency identifier: '%s'",
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
	 * Executes the deposit operation using the CurrencyAdapter service.
	 * <p>
	 * This method uses the CurrencyAdapter's deposit method to ensure proper event firing
	 * and logging. The adapter handles all the complex logic including account validation,
	 * balance updates, and event notifications.
	 * </p>
	 *
	 * @param targetOfflinePlayer the player to receive the currency deposit, must not be null
	 * @param targetCurrency the currency entity for the deposit operation, must not be null
	 * @param depositAmount the amount to deposit into the player's account
	 */
	private void executeDepositOperation(
		final @NotNull OfflinePlayer targetOfflinePlayer,
		final @NotNull Currency targetCurrency,
		final double depositAmount
	) {
		COMMAND_LOGGER.log(
			Level.INFO,
			String.format(
				"Executing console deposit: player=%s, currency=%s, amount=%.2f",
				targetOfflinePlayer.getUniqueId(),
				targetCurrency.getIdentifier(),
				depositAmount
			)
		);
		
		this.jexEconomyImpl.getCurrencyAdapter()
		                   .deposit(targetOfflinePlayer, targetCurrency, depositAmount)
		                   .thenAcceptAsync(
			               currencyResponse -> this.handleDepositResponse(
				               currencyResponse,
				               targetOfflinePlayer,
				               targetCurrency,
				               depositAmount
			               ),
			               this.jexEconomyImpl.getExecutor()
		               )
		                   .exceptionally(throwable -> {
			               COMMAND_LOGGER.log(
				               Level.SEVERE,
				               String.format(
					               "Console deposit operation failed due to async exception: player=%s, currency=%s, amount=%.2f",
					               targetOfflinePlayer.getUniqueId(),
					               targetCurrency.getIdentifier(),
					               depositAmount
				               ),
				               throwable
			               );
			               return null;
		               });
	}
	
	/**
	 * Handles the response from the CurrencyAdapter deposit operation.
	 * <p>
	 * This method processes the CurrencyResponse returned by the adapter and logs
	 * the appropriate success or failure information. The detailed transaction
	 * logging is handled automatically by the adapter's event system.
	 * </p>
	 *
	 * @param response the response from the deposit operation, must not be null
	 * @param targetOfflinePlayer the player who received the deposit, must not be null
	 * @param targetCurrency the currency that was deposited, must not be null
	 * @param depositAmount the amount that was deposited
	 */
	private void handleDepositResponse(
		final @NotNull CurrencyResponse response,
		final @NotNull OfflinePlayer targetOfflinePlayer,
		final @NotNull Currency targetCurrency,
		final double depositAmount
	) {
		if (response.operationStatus().equals(CurrencyResponse.ResponseType.SUCCESS)) {
			COMMAND_LOGGER.log(
				Level.INFO,
				String.format(
					"Console deposit completed successfully: player=%s, currency=%s, amount=%.2f, new_balance=%.2f",
					targetOfflinePlayer.getUniqueId(),
					targetCurrency.getIdentifier(),
					depositAmount,
					response.resultingBalance()
				)
			);
		} else {
			COMMAND_LOGGER.log(
				Level.WARNING,
				String.format(
					"Console deposit failed: player=%s, currency=%s, amount=%.2f, error=%s",
					targetOfflinePlayer.getUniqueId(),
					targetCurrency.getIdentifier(),
					depositAmount,
					response.failureMessage()
				)
			);
		}
	}
}