package de.jexcellence.economy.command.player.currencies;

import com.raindropcentral.commands.PlayerCommand;
import com.raindropcentral.commands.utility.Command;
import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.currency.CurrenciesActionOverviewView;
import de.jexcellence.economy.database.entity.Currency;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Primary command handler for player-initiated currency management operations.
 * <p>
 * This class serves as the main entry point for players to interact with the currency system
 * through a graphical user interface. When executed, it opens the currency management GUI
 * where players can perform all currency-related operations including creation, editing,
 * viewing, and deletion (with appropriate permissions).
 * </p>
 *
 * <h3>GUI-Based Operations:</h3>
 * <ul>
 *   <li><strong>Currency Creation:</strong> Create new currencies through anvil GUI</li>
 *   <li><strong>Currency Editing:</strong> Edit existing currency properties (coming soon)</li>
 *   <li><strong>Currency Deletion:</strong> Remove currencies from the system (coming soon)</li>
 *   <li><strong>Currency Overview:</strong> View all currencies with leaderboards</li>
 *   <li><strong>Balance Reset:</strong> Reset currency balances for all players</li>
 * </ul>
 *
 * <h3>Permission Integration:</h3>
 * <p>
 * The command system integrates with the {@link ECurrenciesPermission} framework to ensure
 * that only authorized users can access the GUI. Individual operations within the GUI
 * perform their own permission checks as needed.
 * </p>
 *
 * <h3>User Interface Integration:</h3>
 * <p>
 * The command opens the {@link CurrenciesActionOverviewView} which provides an intuitive
 * graphical interface for all currency management operations, eliminating the need for
 * complex command-line syntax.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 * @see PlayerCommand
 * @see CurrenciesActionOverviewView
 * @see ECurrenciesPermission
 */
@Command
public class PCurrencies extends PlayerCommand {
	
	/**
	 * The main JExEconomy plugin instance providing access to core services and repositories.
	 * <p>
	 * This instance serves as the primary gateway to the plugin's infrastructure,
	 * including database repositories, service adapters, executor services, and
	 * user interface frameworks required for currency operations.
	 * </p>
	 */
        private static final int DEFAULT_PAGE_SIZE = 10;

        private final JExEconomy jexEconomyImpl;
        private final CurrencyCommandHandler currencyCommandHandler;
	
	/**
	 * Constructs a new player currency command handler with the specified configuration and plugin instance.
	 * <p>
	 * This constructor initializes the command handler with the necessary dependencies to
	 * open the currency management GUI. It establishes connections to the plugin's
	 * infrastructure for accessing the view framework.
	 * </p>
	 *
	 * <h3>Initialization Process:</h3>
	 * <ul>
	 *   <li>Registers the command with the parent command framework</li>
	 *   <li>Establishes connection to the JExEconomy plugin instance</li>
	 *   <li>Configures permission integration and validation</li>
	 * </ul>
	 *
	 * @param commandSection the command section configuration containing command metadata and settings, must not be null
	 * @param jexEconomy the main JExEconomy plugin instance providing access to services and repositories, must not be null
	 * @throws IllegalArgumentException if either parameter is null
	 */
	public PCurrencies(
		final @NotNull PCurrenciesSection commandSection,
		final @NotNull JExEconomy jexEconomy
	) {
		super(commandSection);
        this.jexEconomyImpl = jexEconomy;
        this.currencyCommandHandler = new CurrencyCommandHandler(this.jexEconomyImpl);
    }
	
	/**
	 * Handles the execution of currency commands initiated by players.
	 * <p>
	 * This method serves as the primary entry point for currency command operations.
	 * It performs permission validation and opens the graphical user interface
	 * for currency management. All currency operations are now handled through
	 * the GUI interface for a more intuitive user experience.
	 * </p>
	 *
	 * <h3>Execution Flow:</h3>
	 * <ol>
	 *   <li>Validates base currency command permissions</li>
	 *   <li>Opens the currency management GUI interface</li>
	 * </ol>
	 *
	 * <h3>Permission Validation:</h3>
	 * <p>
	 * The command requires the base currencies permission to access the GUI.
	 * Individual operations within the GUI will perform their own permission
	 * checks as needed.
	 * </p>
	 *
	 * @param player the player executing the currency command, must not be null
	 * @param commandLabel the command label used to invoke this command, must not be null
	 * @param commandArguments the arguments provided with the command, must not be null
	 */
	@Override
	protected void onPlayerInvocation(
		final @NotNull Player player,
		final @NotNull String commandLabel,
		final @NotNull String[] commandArguments
	) {
                if (
                        this.hasNoPermission(
                                player,
                                ECurrenciesPermission.CURRENCIES
                        )
                ) {
                        return;
                }

                if (commandArguments.length == 0) {
                        this.openOverviewInterface(player);
                        return;
                }

                final ActionContext actionContext = this.resolveActionContext(commandArguments);

                switch (actionContext.action()) {
                        case CREATE -> {
                                if (this.hasNoPermission(player, ECurrenciesPermission.CREATE)) {
                                        return;
                                }
                                this.currencyCommandHandler.createCurrency(player, commandArguments);
                        }
                        case DELETE -> {
                                if (this.hasNoPermission(player, ECurrenciesPermission.DELETE)) {
                                        return;
                                }
                                this.currencyCommandHandler.deleteCurrency(player, commandArguments);
                        }
                        case EDIT -> {
                                if (this.hasNoPermission(player, ECurrenciesPermission.EDIT)) {
                                        return;
                                }
                                this.currencyCommandHandler.editCurrency(player, commandArguments);
                        }
                        case INFO -> this.currencyCommandHandler.showCurrencyInfo(player, commandArguments);
                        case HELP -> this.openOverviewInterface(player);
                        case OVERVIEW -> this.handleOverviewAction(player, actionContext.parameters());
                        default -> this.handleOverviewAction(player, actionContext.parameters());
                }
        }

	/**
	 * Provides tab completion for the currency command.
	 * <p>
	 * Since this command only opens a GUI interface, no tab completion
	 * suggestions are provided. All operations are handled through the
	 * graphical user interface.
	 * </p>
	 *
	 * @param tabCompletionRequestingPlayer the player requesting tab completion, must not be null
	 * @param commandLabel the command label being completed, must not be null
	 * @param commandArguments the current command arguments for context, must not be null
	 * @return an empty list since no tab completion is needed for GUI-only command, never null
	 */
	@Override
	protected @NotNull List<String> onPlayerTabCompletion(
		final @NotNull Player tabCompletionRequestingPlayer,
		final @NotNull String commandLabel,
		final @NotNull String[] commandArguments
	) {
                if (commandArguments.length == 0) {
                        return this.firstArgumentSuggestions("");
                }

                if (commandArguments.length == 1) {
                        return this.firstArgumentSuggestions(commandArguments[0]);
                }

                if (!this.isOverviewAlias(commandArguments[0])) {
                        return new ArrayList<>();
                }

                return switch (commandArguments.length) {
                        case 2 -> List.of("1", "2", "3", "4", "5");
                        case 3 -> List.of("10", "25", "50", "100");
                        case 4 -> List.of("identifier", "symbol", "prefix", "suffix");
                        case 5 -> List.of("asc", "desc");
                        case 6 -> List.of("identifier=", "symbol=", "prefix=", "suffix=");
                        default -> new ArrayList<>();
                };
        }

        private void handleOverviewAction(
                final @NotNull Player player,
                final @NotNull String[] overviewArguments
        ) {
                if (this.hasNoPermission(player, ECurrenciesPermission.OVERVIEW)) {
                        return;
                }

                final int page = this.parsePositiveIntegerOrDefault(overviewArguments, 0, 1);
                final int pageSize = this.parsePositiveIntegerOrDefault(overviewArguments, 1, DEFAULT_PAGE_SIZE);
                final Comparator<Currency> sortComparator = this.resolveSortComparator(overviewArguments, 2, 3);

                Predicate<Currency> filterPredicate = currency -> true;
                if (overviewArguments.length > 4) {
                        final String filterExpression = overviewArguments[4];
                        final Optional<Predicate<Currency>> resolvedFilter = this.resolveFilterPredicate(filterExpression);
                        if (resolvedFilter.isEmpty()) {
                                this.currencyCommandHandler.notifyInvalidListFilter(player, filterExpression);
                                return;
                        }
                        filterPredicate = resolvedFilter.get();
                }

                this.currencyCommandHandler.listCurrencies(
                        player,
                        new CurrencyCommandHandler.CurrencyListQuery(
                                page,
                                pageSize,
                                sortComparator,
                                filterPredicate
                        )
                );
        }

        private void openOverviewInterface(final @NotNull Player player) {
                this.jexEconomyImpl.getViewFrame().open(
                        CurrenciesActionOverviewView.class,
                        player,
                        Map.of(
                                "plugin",
                                this.jexEconomyImpl
                        )
                );
        }

        private @NotNull ActionContext resolveActionContext(final @NotNull String[] commandArguments) {
                if (commandArguments.length == 0) {
                        return new ActionContext(ECurrenciesAction.OVERVIEW, new String[0]);
                }

                final Optional<ECurrenciesAction> explicitAction = ECurrenciesAction.fromString(commandArguments[0]);
                if (explicitAction.isPresent()) {
                        return new ActionContext(
                                explicitAction.get(),
                                Arrays.copyOfRange(commandArguments, 1, commandArguments.length)
                        );
                }

                if (this.isOverviewAlias(commandArguments[0])) {
                        return new ActionContext(
                                ECurrenciesAction.OVERVIEW,
                                Arrays.copyOfRange(commandArguments, 1, commandArguments.length)
                        );
                }

                return new ActionContext(ECurrenciesAction.OVERVIEW, commandArguments.clone());
        }

        private @NotNull List<String> firstArgumentSuggestions(final @NotNull String currentToken) {
                final String normalizedToken = currentToken.toLowerCase(Locale.ROOT);
                final List<String> suggestions = new ArrayList<>();
                suggestions.add("overview");
                suggestions.add("help");
                suggestions.add("create");
                suggestions.add("delete");
                suggestions.add("edit");
                suggestions.add("info");
                suggestions.add("list");
                return suggestions.stream()
                                  .filter(suggestion -> suggestion.startsWith(normalizedToken))
                                  .toList();
        }

        private boolean isOverviewAlias(final @NotNull String argument) {
                return "overview".equalsIgnoreCase(argument) || "list".equalsIgnoreCase(argument);
        }

        private int parsePositiveIntegerOrDefault(
                final @NotNull String[] arguments,
                final int index,
                final int defaultValue
        ) {
                if (index >= arguments.length) {
                        return defaultValue;
                }

                try {
                        final int parsedValue = Integer.parseInt(arguments[index]);
                        return parsedValue > 0 ? parsedValue : defaultValue;
                } catch (final NumberFormatException ignored) {
                        return defaultValue;
                }
        }

        private @NotNull Comparator<Currency> resolveSortComparator(
                final @NotNull String[] arguments,
                final int fieldIndex,
                final int directionIndex
        ) {
                final String field = fieldIndex < arguments.length ? arguments[fieldIndex] : "identifier";
                final String direction = directionIndex < arguments.length ? arguments[directionIndex] : "asc";

                Comparator<Currency> comparator = switch (field.toLowerCase(Locale.ROOT)) {
                        case "symbol" -> Comparator.comparing(currency -> this.lowerCase(currency.getSymbol()));
                        case "prefix" -> Comparator.comparing(currency -> this.lowerCase(currency.getPrefix()));
                        case "suffix" -> Comparator.comparing(currency -> this.lowerCase(currency.getSuffix()));
                        default -> Comparator.comparing(currency -> this.lowerCase(currency.getIdentifier()));
                };

                if ("desc".equalsIgnoreCase(direction)) {
                        comparator = comparator.reversed();
                }

                return comparator;
        }

        private @NotNull Optional<Predicate<Currency>> resolveFilterPredicate(final @NotNull String filterExpression) {
                final int separatorIndex = filterExpression.indexOf('=');
                if (separatorIndex <= 0 || separatorIndex == filterExpression.length() - 1) {
                        return Optional.empty();
                }

                final String field = filterExpression.substring(0, separatorIndex).toLowerCase(Locale.ROOT);
                final String value = filterExpression.substring(separatorIndex + 1).trim();
                if (value.isEmpty()) {
                        return Optional.empty();
                }

                final String normalizedValue = value.toLowerCase(Locale.ROOT);
                return switch (field) {
                        case "identifier" -> Optional.of(currency -> this.lowerCase(currency.getIdentifier()).contains(normalizedValue));
                        case "symbol" -> Optional.of(currency -> this.lowerCase(currency.getSymbol()).contains(normalizedValue));
                        case "prefix" -> Optional.of(currency -> this.lowerCase(currency.getPrefix()).contains(normalizedValue));
                        case "suffix" -> Optional.of(currency -> this.lowerCase(currency.getSuffix()).contains(normalizedValue));
                        default -> Optional.empty();
                };
        }

        private @NotNull String lowerCase(final String value) {
                return value == null ? "" : value.toLowerCase(Locale.ROOT);
        }

        private record ActionContext(
                @NotNull ECurrenciesAction action,
                @NotNull String[] parameters
        ) {
        }
}