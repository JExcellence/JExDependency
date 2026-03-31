package de.jexcellence.economy.command.player.currencies;

import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.UserCurrency;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Comprehensive command handler for currency management operations within the JExEconomy system.
 *
 * <p>This class provides a complete command-line interface for currency administration, offering
 * functionality for creating, deleting, editing, listing, and displaying detailed information
 * about currencies. It serves as the primary interface between player commands and the
 * underlying currency management system.
 *
 * <p><strong>Supported Operations:</strong>
 * <ul>
 *   <li><strong>Currency Creation:</strong> Create new currencies with customizable properties</li>
 *   <li><strong>Currency Deletion:</strong> Remove existing currencies from the system</li>
 *   <li><strong>Currency Listing:</strong> Display all available currencies with their properties</li>
 *   <li><strong>Currency Editing:</strong> Modify existing currency properties (symbol, prefix, suffix, identifier)</li>
 *   <li><strong>Currency Information:</strong> Show detailed information about specific currencies</li>
 *   <li><strong>Tab Completion:</strong> Provide intelligent auto-completion for currency identifiers and fields</li>
 * </ul>
 *
 * <p><strong>Design Principles:</strong>
 * <ul>
 *   <li><strong>Asynchronous Operations:</strong> All database operations execute on background threads</li>
 *   <li><strong>Internationalization:</strong> All user feedback uses the modern Polyglot API localization system</li>
 *   <li><strong>Input Validation:</strong> Comprehensive validation of command arguments and parameters</li>
 *   <li><strong>Error Handling:</strong> Graceful handling of edge cases and error conditions</li>
 *   <li><strong>User Experience:</strong> Clear feedback messages and helpful usage information</li>
 * </ul>
 *
 * <p><strong>Integration Points:</strong>
 * <ul>
 *   <li>Integrates with JExEconomy plugin repositories for data persistence</li>
 *   <li>Uses CurrencyAdapter for standardized currency operations</li>
 *   <li>Leverages Polyglot API for localized user messaging</li>
 *   <li>Provides tab completion support for enhanced user experience</li>
 * </ul>
 *
 * <p><strong>Security Considerations:</strong>
 * <ul>
 *   <li>All operations validate currency existence before modification</li>
 *   <li>Identifier uniqueness is enforced during creation and editing</li>
 *   <li>Asynchronous operations prevent server lag during database access</li>
 * </ul>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 * @see JExEconomy
 * @see Currency
 * @see UserCurrency
 */
public class CurrencyCommandHandler {

	/**
	 * Reference to the main JExEconomy plugin instance.
 *
 * <p>Provides access to currency repositories, adapter services, executor services,
	 * and other plugin infrastructure required for executing currency management
	 * operations and maintaining data consistency.
	 */
	private final JExEconomy jexEconomyImpl;

	/**
	 * Constructs a new currency command handler with access to the plugin infrastructure.
 *
 * <p>Initializes the handler with the necessary dependencies to perform currency
	 * management operations, including access to repositories, adapters, and
	 * executor services for asynchronous operations.
	 *
	 * @param jexEconomyImpl the main JExEconomy plugin instance providing access to services, must not be null
	 * @throws IllegalArgumentException if jexEconomyImpl is null
	 */
	public CurrencyCommandHandler(final @NotNull JExEconomy jexEconomyImpl) {
		this.jexEconomyImpl = jexEconomyImpl;
	}

	/**
	 * Handles the creation of a new currency with comprehensive validation and account initialization.
 *
 * <p>This method creates a new currency entity with the specified parameters and automatically
	 * initializes player accounts for all existing users. The operation includes validation
	 * to prevent duplicate currency identifiers and provides detailed feedback to the user.
	 *
	 * <p><strong>Operation Flow:</strong>
	 * <ol>
	 *   <li>Validates command arguments and parameter count</li>
	 *   <li>Checks for existing currency with the same identifier</li>
	 *   <li>Creates new currency entity with specified properties</li>
	 *   <li>Initializes player accounts for all existing users</li>
	 *   <li>Provides success/failure feedback to the user</li>
	 * </ol>
	 *
	 * <p><strong>Parameter Requirements:</strong>
	 * <ul>
	 *   <li><strong>args[1]:</strong> Currency identifier (required, must be unique)</li>
	 *   <li><strong>args[2]:</strong> Currency symbol (required)</li>
	 *   <li><strong>args[3]:</strong> Currency prefix (optional, defaults to empty string)</li>
	 *   <li><strong>args[4]:</strong> Currency suffix (optional, defaults to empty string)</li>
	 * </ul>
	 *
	 * @param commandExecutingPlayer the player executing the currency creation command, must not be null
	 * @param commandArguments command arguments containing currency parameters, must not be null
	 */
	public void createCurrency(
		final @NotNull Player commandExecutingPlayer,
		final @NotNull String[] commandArguments
	) {
		if (commandArguments.length < 3) {
			this.sendUsageMessage(
				commandExecutingPlayer,
				"create <identifier> <symbol> [prefix] [suffix]"
			);
			return;
		}

		final String currencyIdentifier = commandArguments[1];
		final String currencySymbol = commandArguments[2];
		final String currencyPrefix = commandArguments.length > 3 ? commandArguments[3] : "";
		final String currencySuffix = commandArguments.length > 4 ? commandArguments[4] : "";

		this.jexEconomyImpl.getCurrencyAdapter().hasGivenCurrency(currencyIdentifier)
		                   .thenAcceptAsync(
			                           currencyExists -> {
				                           if (currencyExists) {
					                           this.sendCurrencyAlreadyExistsMessage(commandExecutingPlayer, currencyIdentifier);
					                           return;
				                           }

				                           this.executeNewCurrencyCreation(
					                           commandExecutingPlayer,
					                           currencyIdentifier,
					                           currencySymbol,
					                           currencyPrefix,
					                           currencySuffix
				                           );
			                           },
			                           this.jexEconomyImpl.getExecutor()
		                           );
	}

	/**
	 * Handles the deletion of an existing currency with validation and confirmation.
 *
 * <p>This method removes a currency from the system after validating its existence.
	 * The operation includes comprehensive error handling and provides clear feedback
	 * about the success or failure of the deletion operation.
	 *
	 * <p><strong>Operation Flow:</strong>
	 * <ol>
	 *   <li>Validates command arguments and parameter count</li>
	 *   <li>Verifies currency exists in the system</li>
	 *   <li>Locates currency entity in the database</li>
	 *   <li>Performs deletion operation</li>
	 *   <li>Provides success/failure feedback to the user</li>
	 * </ol>
	 *
	 * <p><strong>Parameter Requirements:</strong>
	 * <ul>
	 *   <li><strong>args[1]:</strong> Currency identifier (required, must exist)</li>
	 * </ul>
	 *
	 * @param commandExecutingPlayer the player executing the currency deletion command, must not be null
	 * @param commandArguments command arguments containing the currency identifier, must not be null
	 */
	public void deleteCurrency(
		final @NotNull Player commandExecutingPlayer,
		final @NotNull String[] commandArguments
	) {
		if (commandArguments.length < 2) {
			this.sendUsageMessage(
				commandExecutingPlayer,
				"delete <identifier>"
			);
			return;
		}

		final String currencyIdentifier = commandArguments[1];

		this.jexEconomyImpl.getCurrencyAdapter().hasGivenCurrency(currencyIdentifier)
		                   .thenAcceptAsync(
			                           currencyExists -> {
				                           if (!currencyExists) {
					                           this.sendCurrencyNotFoundMessage(commandExecutingPlayer, currencyIdentifier);
					                           return;
				                           }

				                           this.executeCurrencyDeletion(commandExecutingPlayer, currencyIdentifier);
			                           },
			                           this.jexEconomyImpl.getExecutor()
		                           );
	}

	/**
	 * Lists all available currencies with their properties and statistics.
 *
 * <p>This method retrieves all currencies from the database and displays them
	 * in a formatted list with comprehensive information about each currency.
	 * If no currencies exist, it provides appropriate feedback to the user.
	 *
	 * <p><strong>Display Information:</strong>
	 * <ul>
	 *   <li>Currency identifier and symbol</li>
	 *   <li>Prefix and suffix formatting</li>
	 *   <li>Total count of available currencies</li>
	 *   <li>Empty state message if no currencies exist</li>
	 * </ul>
	 *
	 * @param commandExecutingPlayer the player requesting the currency list, must not be null
	 */
        public void listCurrencies(final @NotNull Player commandExecutingPlayer) {
                this.listCurrencies(
                        commandExecutingPlayer,
                        CurrencyListQuery.defaultQuery()
                );
        }

        /**
         * Lists currencies for the given player using the provided query options.
 *
 * <p>This overload allows callers to customize pagination, sorting, and filtering behaviour while
         * still delegating the asynchronous retrieval and message formatting to the handler.
         *
         * @param commandExecutingPlayer the player requesting the currency list, must not be null
         * @param listQuery the configuration describing pagination, sorting, and filtering options, must not be null
         */
        public void listCurrencies(
                final @NotNull Player commandExecutingPlayer,
                final @NotNull CurrencyListQuery listQuery
        ) {
                Objects.requireNonNull(commandExecutingPlayer, "commandExecutingPlayer");
                Objects.requireNonNull(listQuery, "listQuery");

                final int sanitizedPage = Math.max(1, listQuery.page());
                final int sanitizedPageSize = Math.max(1, Math.min(listQuery.pageSize(), CurrencyListQuery.MAXIMUM_PAGE_SIZE));

                CompletableFuture.supplyAsync(
                        () -> this.jexEconomyImpl.getCurrencyRepository().findAll(0, CurrencyListQuery.MAXIMUM_PAGE_SIZE),
                        this.jexEconomyImpl.getExecutor()
                ).thenAcceptAsync(
                        availableCurrencies -> {
                                if (availableCurrencies == null || availableCurrencies.isEmpty()) {
                                        this.sendEmptyCurrencyListMessage(commandExecutingPlayer);
                                        return;
                                }

                                final List<Currency> filteredCurrencies = availableCurrencies.stream()
                                                                                               .filter(listQuery.filter())
                                                                                               .sorted(listQuery.sortComparator())
                                                                                               .collect(Collectors.toList());

                                if (filteredCurrencies.isEmpty()) {
                                        this.sendEmptyCurrencyListMessage(commandExecutingPlayer);
                                        return;
                                }

                                final int fromIndex = Math.min(filteredCurrencies.size(), (sanitizedPage - 1) * sanitizedPageSize);
                                if (fromIndex >= filteredCurrencies.size()) {
                                        this.sendEmptyCurrencyListMessage(commandExecutingPlayer);
                                        return;
                                }

                                final int toIndex = Math.min(filteredCurrencies.size(), fromIndex + sanitizedPageSize);
                                final List<Currency> pageCurrencies = filteredCurrencies.subList(fromIndex, toIndex);

                                this.sendCurrencyListHeader(commandExecutingPlayer, filteredCurrencies.size());
                                this.sendCurrencyListEntries(commandExecutingPlayer, pageCurrencies);
                        },
                        this.jexEconomyImpl.getExecutor()
                );
        }

	/**
	 * Edits a specific field of an existing currency with validation and persistence.
 *
 * <p>This method allows modification of currency properties including symbol, prefix,
	 * suffix, and identifier. It includes comprehensive validation to ensure data
	 * integrity and prevent conflicts with existing currencies.
	 *
	 * <p><strong>Editable Fields:</strong>
	 * <ul>
	 *   <li><strong>symbol:</strong> The currency symbol displayed in transactions</li>
	 *   <li><strong>prefix:</strong> Text displayed before currency amounts</li>
	 *   <li><strong>suffix:</strong> Text displayed after currency amounts</li>
	 *   <li><strong>identifier:</strong> Unique currency identifier (validated for uniqueness)</li>
	 * </ul>
	 *
	 * <p><strong>Parameter Requirements:</strong>
	 * <ul>
	 *   <li><strong>args[1]:</strong> Currency identifier (required, must exist)</li>
	 *   <li><strong>args[2]:</strong> Field name to edit (required, must be valid)</li>
	 *   <li><strong>args[3]:</strong> New value for the field (required)</li>
	 * </ul>
	 *
	 * @param commandExecutingPlayer the player executing the currency edit command, must not be null
	 * @param commandArguments command arguments containing currency identifier, field, and value, must not be null
	 */
	public void editCurrency(
		final @NotNull Player commandExecutingPlayer,
		final @NotNull String[] commandArguments
	) {
		if (commandArguments.length < 4) {
			this.sendUsageMessage(
				commandExecutingPlayer,
				"edit <identifier> <field> <value>"
			);
			this.sendEditableFieldsMessage(commandExecutingPlayer);
			return;
		}

		final String currencyIdentifier = commandArguments[1];
		final String fieldToEdit = commandArguments[2].toLowerCase();
		final String newFieldValue = commandArguments[3];

		this.jexEconomyImpl.getCurrencyAdapter().hasGivenCurrency(currencyIdentifier)
		                   .thenAcceptAsync(
			                           currencyExists -> {
				                           if (!currencyExists) {
					                           this.sendCurrencyNotFoundMessage(commandExecutingPlayer, currencyIdentifier);
					                           return;
				                           }

				                           this.executeCurrencyFieldEdit(
					                           commandExecutingPlayer,
					                           currencyIdentifier,
					                           fieldToEdit,
					                           newFieldValue
				                           );
			                           },
			                           this.jexEconomyImpl.getExecutor()
		                           );
	}

	/**
	 * Shows detailed information about a specific currency including all properties.
 *
 * <p>This method retrieves and displays comprehensive information about a currency,
	 * including its identifier, symbol, prefix, suffix, and other relevant details.
	 * The information is formatted for easy reading and understanding.
	 *
	 * <p><strong>Displayed Information:</strong>
	 * <ul>
	 *   <li>Currency identifier and symbol</li>
	 *   <li>Prefix and suffix formatting strings</li>
	 *   <li>Currency creation and modification details</li>
	 *   <li>Usage statistics and player account information</li>
	 * </ul>
	 *
	 * <p><strong>Parameter Requirements:</strong>
	 * <ul>
	 *   <li><strong>args[1]:</strong> Currency identifier (required, must exist)</li>
	 * </ul>
	 *
	 * @param commandExecutingPlayer the player requesting currency information, must not be null
	 * @param commandArguments command arguments containing the currency identifier, must not be null
	 */
	public void showCurrencyInfo(
		final @NotNull Player commandExecutingPlayer,
		final @NotNull String[] commandArguments
	) {
		if (commandArguments.length < 2) {
			this.sendUsageMessage(
				commandExecutingPlayer,
				"info <identifier>"
			);
			return;
		}

		final String currencyIdentifier = commandArguments[1];

		CompletableFuture.supplyAsync(
			() -> this.jexEconomyImpl.getCurrencyRepository().findByAttributes(
				Map.of("identifier", currencyIdentifier)
			).orElse(null),
			this.jexEconomyImpl.getExecutor()
		).thenAcceptAsync(
			currencyEntity -> {
				if (currencyEntity == null) {
					this.sendCurrencyNotFoundMessage(commandExecutingPlayer, currencyIdentifier);
					return;
				}

				this.sendCurrencyInformationDisplay(commandExecutingPlayer, currencyEntity);
			},
			this.jexEconomyImpl.getExecutor()
		);
	}

	/**
	 * Retrieves a list of all available currency identifiers for tab completion support.
 *
 * <p>This method provides asynchronous access to currency identifiers for use in
	 * command tab completion, improving the user experience by offering intelligent
	 * auto-completion suggestions based on existing currencies in the system.
	 *
	 * @return a CompletableFuture containing a list of currency identifiers for tab completion
	 */
	public @NotNull CompletableFuture<List<String>> getCurrencyIdentifiers() {
		return CompletableFuture.supplyAsync(
			() -> this.jexEconomyImpl.getCurrencyRepository().findAll(0, 128)
			                         .stream()
			                         .map(Currency::getIdentifier)
			                         .toList(),
			this.jexEconomyImpl.getExecutor()
		);
	}

	/**
	 * Retrieves a list of editable currency fields for tab completion support.
 *
 * <p>This method provides a static list of field names that can be edited through
	 * the currency edit command, supporting tab completion for improved user experience
	 * and reduced command errors.
	 *
	 * @return a list of editable field names for tab completion
	 */
	public @NotNull List<String> getEditableFields() {
		return Arrays.asList(
			"symbol",
			"prefix",
			"suffix",
			"identifier"
		);
	}

	/**
	 * Executes the actual currency creation process with account initialization.
 *
 * <p>This private method handles the core currency creation logic, including
	 * entity creation, database persistence, and automatic player account
	 * initialization for all existing users in the system.
	 *
	 * @param commandExecutingPlayer the player executing the command, must not be null
	 * @param currencyIdentifier the unique identifier for the new currency, must not be null
	 * @param currencySymbol the symbol for the new currency, must not be null
	 * @param currencyPrefix the prefix for the new currency, must not be null
	 * @param currencySuffix the suffix for the new currency, must not be null
	 */
	private void executeNewCurrencyCreation(
		final @NotNull Player commandExecutingPlayer,
		final @NotNull String currencyIdentifier,
		final @NotNull String currencySymbol,
		final @NotNull String currencyPrefix,
		final @NotNull String currencySuffix
	) {
		final Currency newCurrencyEntity = new Currency(
			currencyPrefix,
			currencySuffix,
			currencyIdentifier,
			currencySymbol,
			Material.GOLD_INGOT
		);

		this.jexEconomyImpl.getCurrencyAdapter().createCurrency(newCurrencyEntity)
		                   .thenAcceptAsync(
			                           creationSuccessful -> {
				                           if (creationSuccessful) {
					                           this.sendCurrencyCreationSuccessMessage(commandExecutingPlayer, currencyIdentifier);
					                           this.initializePlayerAccountsForNewCurrency(
						                           commandExecutingPlayer,
						                           newCurrencyEntity,
						                           currencyIdentifier
					                           );
				                           } else {
					                           this.sendCurrencyCreationFailedMessage(commandExecutingPlayer);
				                           }
			                           },
			                           this.jexEconomyImpl.getExecutor()
		                           );
	}

	/**
	 * Initializes player accounts for a newly created currency.
 *
 * <p>This private method creates UserCurrency entities for all existing users
	 * when a new currency is added to the system, ensuring that all players
	 * have accounts for the new currency with zero initial balance.
	 *
	 * @param commandExecutingPlayer the player who created the currency, must not be null
	 * @param newCurrencyEntity the newly created currency entity, must not be null
	 * @param currencyIdentifier the identifier of the new currency, must not be null
	 */
	private void initializePlayerAccountsForNewCurrency(
		final @NotNull Player commandExecutingPlayer,
		final @NotNull Currency newCurrencyEntity,
		final @NotNull String currencyIdentifier
	) {
		this.jexEconomyImpl.getUserRepository().findAllAsync(0, 128)
		                   .thenAcceptAsync(
			                           existingUsers -> {
				                           existingUsers.forEach(userEntity ->
					                                                 this.jexEconomyImpl.getUserCurrencyRepository().create(
						                                                 new UserCurrency(userEntity, newCurrencyEntity)
					                                                 )
				                           );

				                           this.sendPlayerAccountsCreatedMessage(
					                           commandExecutingPlayer,
					                           existingUsers.size(),
					                           currencyIdentifier
				                           );
			                           },
			                           this.jexEconomyImpl.getExecutor()
		                           );
	}

	/**
	 * Executes the actual currency deletion process with database cleanup.
 *
 * <p>This private method handles the core currency deletion logic, including
	 * entity lookup, database removal, and appropriate success/failure feedback.
	 *
	 * @param commandExecutingPlayer the player executing the deletion command, must not be null
	 * @param currencyIdentifier the identifier of the currency to delete, must not be null
	 */
	private void executeCurrencyDeletion(
		final @NotNull Player commandExecutingPlayer,
		final @NotNull String currencyIdentifier
	) {
		CompletableFuture.supplyAsync(
			() -> {
				final Currency currencyToDelete = this.jexEconomyImpl.getCurrencyRepository()
				                                                     .findByAttributes(Map.of("identifier", currencyIdentifier)).orElse(null);

				if (currencyToDelete != null) {
					this.jexEconomyImpl.getCurrencyRepository().delete(currencyToDelete.getId());
					return true;
				}
				return false;
			},
			this.jexEconomyImpl.getExecutor()
		).thenAcceptAsync(
			deletionSuccessful -> {
				if (deletionSuccessful) {
					this.sendCurrencyDeletionSuccessMessage(commandExecutingPlayer, currencyIdentifier);
				} else {
					this.sendCurrencyDeletionFailedMessage(commandExecutingPlayer);
				}
			},
			this.jexEconomyImpl.getExecutor()
		);
	}

	/**
	 * Executes the currency field editing process with validation and persistence.
 *
 * <p>This private method handles the core field editing logic, including
	 * field validation, value assignment, uniqueness checking for identifiers,
	 * and database persistence of changes.
	 *
	 * @param commandExecutingPlayer the player executing the edit command, must not be null
	 * @param currencyIdentifier the identifier of the currency to edit, must not be null
	 * @param fieldToEdit the name of the field to modify, must not be null
	 * @param newFieldValue the new value for the field, must not be null
	 */
	private void executeCurrencyFieldEdit(
		final @NotNull Player commandExecutingPlayer,
		final @NotNull String currencyIdentifier,
		final @NotNull String fieldToEdit,
		final @NotNull String newFieldValue
	) {
		CompletableFuture.supplyAsync(
			() -> {
				final Currency currencyToEdit = this.jexEconomyImpl.getCurrencyRepository()
				                                                   .findByAttributes(Map.of("identifier", currencyIdentifier)).orElse(null);

				if (currencyToEdit != null) {
					final boolean fieldUpdateSuccessful = this.updateCurrencyField(
						currencyToEdit,
						fieldToEdit,
						newFieldValue
					);

					if (fieldUpdateSuccessful) {
						this.jexEconomyImpl.getCurrencyRepository().update(currencyToEdit);
						return true;
					}
				}
				return false;
			},
			this.jexEconomyImpl.getExecutor()
		).thenAcceptAsync(
			editSuccessful -> {
				if (editSuccessful) {
					this.sendCurrencyEditSuccessMessage(
						commandExecutingPlayer,
						currencyIdentifier,
						fieldToEdit,
						newFieldValue
					);
				} else {
					this.sendCurrencyEditFailedMessage(commandExecutingPlayer);
				}
			},
			this.jexEconomyImpl.getExecutor()
		);
	}

	/**
	 * Updates a specific field of a currency entity with validation.
 *
 * <p>This private method handles the actual field modification logic,
	 * including validation for identifier uniqueness and field existence.
	 *
	 * @param currencyEntity the currency entity to modify, must not be null
	 * @param fieldName the name of the field to update, must not be null
	 * @param newValue the new value for the field, must not be null
	 * @return true if the field was successfully updated, false otherwise
	 */
	private boolean updateCurrencyField(
		final @NotNull Currency currencyEntity,
		final @NotNull String fieldName,
		final @NotNull String newValue
	) {
		switch (fieldName) {
			case "symbol" -> {
				currencyEntity.setSymbol(newValue);
				return true;
			}
			case "prefix" -> {
				currencyEntity.setPrefix(newValue);
				return true;
			}
			case "suffix" -> {
				currencyEntity.setSuffix(newValue);
				return true;
			}
			case "identifier" -> {
				final Currency existingCurrencyWithIdentifier = this.jexEconomyImpl
					                                                .getCurrencyRepository()
					                                                .findByAttributes(Map.of("identifier", newValue)).orElse(null);

				if (existingCurrencyWithIdentifier != null) {
					return false;
				}

				currencyEntity.setIdentifier(newValue);
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	// ===== MESSAGE SENDING METHODS - USING CORRECT I18N API =====

	/**
	 * Sends currency list header message with count information.
 *
 * <p>This private method sends a formatted header message displaying
	 * the total number of available currencies in the system.
	 *
	 * @param targetPlayer the player to send the message to, must not be null
	 * @param currencyCount the total number of currencies available
	 */
	private void sendCurrencyListHeader(
		final @NotNull Player targetPlayer,
		final int currencyCount
	) {
		new I18n.Builder("currency.list.header", targetPlayer)
			.includePrefix()
			.withPlaceholder("count", currencyCount)
			.build().sendMessage();
	}

	/**
	 * Sends individual currency list entries with detailed information.
 *
 * <p>This private method iterates through the currency list and sends
	 * formatted entries displaying each currency's properties.
	 *
	 * @param targetPlayer the player to send the entries to, must not be null
	 * @param currencyList the list of currencies to display, must not be null
	 */
	private void sendCurrencyListEntries(
		final @NotNull Player targetPlayer,
		final @NotNull List<Currency> currencyList
	) {
		for (final Currency currencyEntity : currencyList) {
			new I18n.Builder("currency.list.entry", targetPlayer)
				.includePrefix()
				.withPlaceholder("identifier", currencyEntity.getIdentifier())
				.withPlaceholder("symbol", currencyEntity.getSymbol())
				.withPlaceholder("prefix", currencyEntity.getPrefix())
				.withPlaceholder("suffix", currencyEntity.getSuffix())
				.build().sendMessage();
		}
	}

	/**
	 * Sends comprehensive currency information display to the player.
 *
 * <p>This private method formats and sends detailed information about
	 * a specific currency, including all its properties and configuration.
	 *
	 * @param targetPlayer the player to send the information to, must not be null
	 * @param currencyEntity the currency entity to display information for, must not be null
	 */
	private void sendCurrencyInformationDisplay(
		final @NotNull Player targetPlayer,
		final @NotNull Currency currencyEntity
	) {
		// Send header message
		new I18n.Builder("currency.info.header", targetPlayer)
			.includePrefix()
			.withPlaceholder("identifier", currencyEntity.getIdentifier())
			.build().sendMessage();

		// Send details message
		new I18n.Builder("currency.info.details", targetPlayer)
			.includePrefix()
			.withPlaceholder("symbol", currencyEntity.getSymbol())
			.withPlaceholder("prefix", currencyEntity.getPrefix())
			.withPlaceholder("suffix", currencyEntity.getSuffix())
			.build().sendMessage();
	}

	/**
	 * Sends a usage message to the player for command syntax guidance.
 *
 * <p>This private method provides standardized usage information to help
	 * players understand the correct syntax for currency commands.
	 *
	 * @param targetPlayer the player to send the usage message to, must not be null
	 * @param usageSyntax the usage syntax string to display, must not be null
	 */
	private void sendUsageMessage(
		final @NotNull Player targetPlayer,
		final @NotNull String usageSyntax
	) {
		new I18n.Builder("currency.command.usage", targetPlayer)
			.includePrefix()
			.withPlaceholder("usage", usageSyntax)
			.build().sendMessage();
	}

	/**
	 * Sends information about editable currency fields to the player.
 *
 * <p>This private method provides guidance about which fields can be
	 * modified through the currency edit command.
	 *
	 * @param targetPlayer the player to send the fields information to, must not be null
	 */
	private void sendEditableFieldsMessage(final @NotNull Player targetPlayer) {
		new I18n.Builder("currency.edit.fields", targetPlayer)
			.includePrefix()
			.build().sendMessage();
	}

	/**
	 * Sends currency already exists error message to the player.
 *
 * <p>This private method notifies the player when attempting to create
	 * a currency with an identifier that already exists in the system.
	 *
	 * @param targetPlayer the player to send the error message to, must not be null
	 * @param currencyIdentifier the identifier that already exists, must not be null
	 */
	private void sendCurrencyAlreadyExistsMessage(
		final @NotNull Player targetPlayer,
		final @NotNull String currencyIdentifier
	) {
		new I18n.Builder("currency.create.already_exists", targetPlayer)
			.includePrefix()
			.withPlaceholder("identifier", currencyIdentifier)
			.build().sendMessage();
	}

	/**
	 * Sends currency not found error message to the player.
 *
 * <p>This private method notifies the player when attempting to operate
	 * on a currency that doesn't exist in the system.
	 *
	 * @param targetPlayer the player to send the error message to, must not be null
	 * @param currencyIdentifier the identifier that was not found, must not be null
	 */
	private void sendCurrencyNotFoundMessage(
		final @NotNull Player targetPlayer,
		final @NotNull String currencyIdentifier
	) {
		new I18n.Builder("currency.delete.not_found", targetPlayer)
			.includePrefix()
			.withPlaceholder("identifier", currencyIdentifier)
			.build().sendMessage();
	}

	/**
	 * Sends empty currency list message to the player.
 *
 * <p>This private method notifies the player when no currencies
	 * exist in the system during a list operation.
	 *
	 * @param targetPlayer the player to send the message to, must not be null
	 */
        private void sendEmptyCurrencyListMessage(final @NotNull Player targetPlayer) {
                new I18n.Builder("currency.list.empty", targetPlayer)
                        .includePrefix()
                        .build().sendMessage();
        }

        /**
         * Notifies the player that the provided list filter was invalid and therefore rejected.
         *
         * @param targetPlayer the player who should receive the notification, must not be null
         * @param filterExpression the filter expression that failed validation, must not be null
         */
        void notifyInvalidListFilter(
                final @NotNull Player targetPlayer,
                final @NotNull String filterExpression
        ) {
                new I18n.Builder("currency.list.invalid_filter", targetPlayer)
                        .includePrefix()
                        .withPlaceholder("filter", filterExpression)
                        .build().sendMessage();
        }

        /**
         * Immutable configuration describing pagination, sorting, and filtering behaviour for currency listings.
         */
        public record CurrencyListQuery(
                int page,
                int pageSize,
                @NotNull Comparator<Currency> sortComparator,
                @NotNull Predicate<Currency> filter
        ) {

                private static final int MAXIMUM_PAGE_SIZE = 128;

                public CurrencyListQuery {
                        Objects.requireNonNull(sortComparator, "sortComparator");
                        Objects.requireNonNull(filter, "filter");
                }

                /**
                 * Executes method.
                 */
                /**
                 * Executes this member.
                 */
                /**
                 * Executes defaultQuery.
                 */
                public static CurrencyListQuery defaultQuery() {
                        return new CurrencyListQuery(
                                1,
                                MAXIMUM_PAGE_SIZE,
                                Comparator.comparing(currency -> currency.getIdentifier().toLowerCase(java.util.Locale.ROOT)),
                                currency -> true
                        );
                }
        }

	/**
	 * Sends currency creation success message to the player.
 *
 * <p>This private method confirms successful currency creation
	 * with the specified identifier.
	 *
	 * @param targetPlayer the player to send the success message to, must not be null
	 * @param currencyIdentifier the identifier of the created currency, must not be null
	 */
	private void sendCurrencyCreationSuccessMessage(
		final @NotNull Player targetPlayer,
		final @NotNull String currencyIdentifier
	) {
		new I18n.Builder("currency.create.success", targetPlayer)
			.includePrefix()
			.withPlaceholder("identifier", currencyIdentifier)
			.build().sendMessage();
	}

	/**
	 * Sends currency creation failed message to the player.
 *
 * <p>This private method notifies the player when currency
	 * creation fails due to system errors.
	 *
	 * @param targetPlayer the player to send the error message to, must not be null
	 */
	private void sendCurrencyCreationFailedMessage(final @NotNull Player targetPlayer) {
		new I18n.Builder("currency.create.failed", targetPlayer)
			.includePrefix()
			.build().sendMessage();
	}

	/**
	 * Sends player accounts created confirmation message.
 *
 * <p>This private method confirms that player accounts have been
	 * successfully created for a new currency.
	 *
	 * @param targetPlayer the player to send the confirmation to, must not be null
	 * @param playerCount the number of player accounts created
	 * @param currencyIdentifier the identifier of the currency, must not be null
	 */
	private void sendPlayerAccountsCreatedMessage(
		final @NotNull Player targetPlayer,
		final int playerCount,
		final @NotNull String currencyIdentifier
	) {
		new I18n.Builder("currency.create.success.player_accounts_created", targetPlayer)
			.includePrefix()
			.withPlaceholder("player_amount", playerCount)
			.withPlaceholder("identifier", currencyIdentifier)
			.build().sendMessage();
	}

	/**
	 * Sends currency deletion success message to the player.
 *
 * <p>This private method confirms successful currency deletion
	 * with the specified identifier.
	 *
	 * @param targetPlayer the player to send the success message to, must not be null
	 * @param currencyIdentifier the identifier of the deleted currency, must not be null
	 */
	private void sendCurrencyDeletionSuccessMessage(
		final @NotNull Player targetPlayer,
		final @NotNull String currencyIdentifier
	) {
		new I18n.Builder("currency.delete.success", targetPlayer)
			.includePrefix()
			.withPlaceholder("identifier", currencyIdentifier)
			.build().sendMessage();
	}

	/**
	 * Sends currency deletion failed message to the player.
 *
 * <p>This private method notifies the player when currency
	 * deletion fails due to system errors.
	 *
	 * @param targetPlayer the player to send the error message to, must not be null
	 */
	private void sendCurrencyDeletionFailedMessage(final @NotNull Player targetPlayer) {
		new I18n.Builder("currency.delete.failed", targetPlayer)
			.includePrefix()
			.build().sendMessage();
	}

	/**
	 * Sends currency edit success message to the player.
 *
 * <p>This private method confirms successful currency field
	 * modification with details about the change.
	 *
	 * @param targetPlayer the player to send the success message to, must not be null
	 * @param currencyIdentifier the identifier of the edited currency, must not be null
	 * @param fieldName the name of the field that was edited, must not be null
	 * @param newValue the new value that was set, must not be null
	 */
	private void sendCurrencyEditSuccessMessage(
		final @NotNull Player targetPlayer,
		final @NotNull String currencyIdentifier,
		final @NotNull String fieldName,
		final @NotNull String newValue
	) {
		new I18n.Builder("currency.edit.success", targetPlayer)
			.includePrefix()
			.withPlaceholder("identifier", currencyIdentifier)
			.withPlaceholder("field", fieldName)
			.withPlaceholder("value", newValue)
			.build().sendMessage();
	}

	/**
	 * Sends currency edit failed message to the player.
 *
 * <p>This private method notifies the player when currency
	 * editing fails due to validation or system errors.
	 *
	 * @param targetPlayer the player to send the error message to, must not be null
	 */
	private void sendCurrencyEditFailedMessage(final @NotNull Player targetPlayer) {
		new I18n.Builder("currency.edit.failed", targetPlayer)
			.includePrefix()
			.build().sendMessage();
	}
}
