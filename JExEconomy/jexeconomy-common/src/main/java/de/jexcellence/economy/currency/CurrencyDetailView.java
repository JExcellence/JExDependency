package de.jexcellence.economy.currency;

import com.raindropcentral.rplatform.utility.heads.view.Cancel;
import com.raindropcentral.rplatform.utility.heads.view.Leaderboard;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import com.raindropcentral.rplatform.view.ConfirmationView;

import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.UserCurrency;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

/**
 * Detail view for displaying comprehensive information about a specific currency.
 * <p>
 * This view provides a read-only display of all currency properties including
 * identifier, symbol, icon, prefix, suffix, and additional metadata. It also
 * offers administrative functions such as viewing the leaderboard and resetting
 * all player balances to zero for users with appropriate permissions.
 * </p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Currency Information Display:</strong> Shows all currency properties in an organized layout</li>
 *   <li><strong>Leaderboard Access:</strong> Provides quick navigation to currency leaderboard rankings</li>
 *   <li><strong>Administrative Functions:</strong> Allows authorized users to reset all player balances</li>
 *   <li><strong>Permission-Based Access:</strong> Restricts administrative functions to authorized users</li>
 * </ul>
 *
 * <h3>Layout Structure:</h3>
 * <ul>
 *   <li><strong>Currency Icon (i):</strong> Displays the currency's visual representation</li>
 *   <li><strong>Currency Symbol (s):</strong> Shows the currency's symbol and related information</li>
 *   <li><strong>Leaderboard (l):</strong> Provides access to currency rankings</li>
 *   <li><strong>Prefix Display (p):</strong> Shows currency prefix configuration</li>
 *   <li><strong>Suffix Display (f):</strong> Shows currency suffix configuration</li>
 *   <li><strong>Reset Function (r):</strong> Administrative balance reset functionality</li>
 * </ul>
 *
 * <h3>Administrative Permissions:</h3>
 * <p>
 * The reset functionality requires one of the following permissions:
 * </p>
 * <ul>
 *   <li><code>jexeconomy.admin.reset</code> - Specific reset permission</li>
 *   <li><code>jexeconomy.admin.*</code> - All administrative permissions</li>
 *   <li>Server operator status</li>
 * </ul>
 *
 * @author JExcellence
 * @see BaseView
 * @see Currency
 * @see UserCurrency
 */
public class CurrencyDetailView extends BaseView {
	
	/**
	 * Decimal formatter for displaying currency amounts with thousands separators.
	 */
	private static final DecimalFormat BALANCE_DECIMAL_FORMAT = new DecimalFormat("#,###.##");
	
	/**
	 * State holder for the main JExEconomy plugin instance.
	 */
	private final State<JExEconomy> jexEconomy = initialState("plugin");
	
	/**
	 * State holder for the currency being displayed in detail.
	 */
	private final State<Currency> targetCurrency = initialState("currency");
	
	/**
	 * Constructs a new {@code CurrencyDetailView} with the currencies overview as parent.
	 * <p>
	 * The view will display detailed information about a specific currency and provide
	 * navigation back to the currencies overview when closed.
	 * </p>
	 */
	public CurrencyDetailView() {
		super(CurrenciesOverviewView.class);
	}
	
	/**
	 * Returns the internationalization key for this view.
	 * <p>
	 * This key is used to load localized strings for the currency detail interface,
	 * including titles, labels, and formatting templates.
	 * </p>
	 *
	 * @return the i18n key for the currency detail UI
	 */
	@Override
	protected @NotNull String getKey() {
		return "currency_detail_ui";
	}
	
	/**
	 * Defines the layout structure for the currency detail view.
	 * <p>
	 * The layout organizes currency information and administrative functions
	 * in a user-friendly arrangement with clear visual separation.
	 * </p>
	 *
	 * <h3>Layout Mapping:</h3>
	 * <ul>
	 *   <li><strong>i:</strong> Currency icon display</li>
	 *   <li><strong>s:</strong> Currency symbol information</li>
	 *   <li><strong>l:</strong> Leaderboard access button</li>
	 *   <li><strong>p:</strong> Currency prefix display</li>
	 *   <li><strong>f:</strong> Currency suffix display</li>
	 *   <li><strong>r:</strong> Reset all balances button (admin only)</li>
	 * </ul>
	 *
	 * @return the layout pattern as a string array, never null
	 */
	@Override
	protected @NotNull String[] getLayout() {
		return new String[]{
			"         ",
			" is l pf ",
			"         ",
			"         ",
			"        r"
		};
	}
	
	/**
	 * Renders all currency detail elements and interactive components.
	 * <p>
	 * This method populates the view with currency information displays and
	 * interactive elements including the leaderboard access and administrative
	 * functions. Each element is positioned according to the layout configuration.
	 * </p>
	 *
	 * <h3>Rendered Elements:</h3>
	 * <ul>
	 *   <li>Currency icon with material information</li>
	 *   <li>Currency symbol display with context</li>
	 *   <li>Prefix and suffix configuration displays</li>
	 *   <li>Leaderboard navigation button</li>
	 *   <li>Administrative reset function (permission-based)</li>
	 * </ul>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 */
	@Override
	public void onFirstRender(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer
	) {
		final Currency       displayedCurrency = this.targetCurrency.get(renderContext);
		final JExEconomy pluginInstance    = this.jexEconomy.get(renderContext);
		
		this.renderCurrencyIcon(renderContext, contextPlayer, displayedCurrency);
		this.renderCurrencySymbol(renderContext, contextPlayer, displayedCurrency);
		this.renderCurrencyPrefix(renderContext, contextPlayer, displayedCurrency);
		this.renderCurrencySuffix(renderContext, contextPlayer, displayedCurrency);
		this.renderLeaderboardAccess(renderContext, contextPlayer, displayedCurrency);
		this.renderResetFunction(renderContext, contextPlayer, displayedCurrency, pluginInstance);
	}
	
	/**
	 * Renders the currency icon display element.
	 * <p>
	 * This method creates an interactive display showing the currency's icon
	 * material with detailed information about its visual representation.
	 * </p>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 * @param displayedCurrency the currency being displayed, must not be null
	 */
	private void renderCurrencyIcon(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer,
		final @NotNull Currency displayedCurrency
	) {
		renderContext
			.layoutSlot(
				'i',
				UnifiedBuilderFactory
					.item(displayedCurrency.getIcon())
					.setName(
						this.i18n(
							    "currency_icon.name",
							    contextPlayer
						    )
						    .with(
							    "currency_identifier",
							    displayedCurrency.getIdentifier()
						    )
						    .build().component()
					)
					.setLore(
						this.i18n(
							    "currency_icon.lore",
							    contextPlayer
						    )
						    .withAll(
							    Map.of(
								    "material_name",
								    displayedCurrency.getIcon().translationKey(),
								    "currency_identifier",
								    displayedCurrency.getIdentifier()
							    )
						    ).build().splitLines()
					)
					.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
					.build()
			);
	}
	
	/**
	 * Renders the currency symbol display element.
	 * <p>
	 * This method creates an interactive display showing the currency's symbol
	 * with contextual information about its usage and meaning.
	 * </p>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 * @param displayedCurrency the currency being displayed, must not be null
	 */
	private void renderCurrencySymbol(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer,
		final @NotNull Currency displayedCurrency
	) {
		renderContext.layoutSlot(
			's',
			UnifiedBuilderFactory
				.item(Material.GOLD_NUGGET)
				.setName(
					this.i18n(
						    "currency_symbol.name",
						    contextPlayer
					    )
					    .with(
						    "currency_symbol",
						    displayedCurrency.getSymbol()
					    )
					    .build().component())
				.setLore(
					this.i18n(
						    "currency_symbol.lore",
						    contextPlayer
					    )
					    .withAll(
						    Map.of(
							    "currency_symbol",
							    displayedCurrency.getSymbol(),
							    "currency_identifier",
							    displayedCurrency.getIdentifier()
						    )
					    ).build().splitLines()
				)
				.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
				.build()
		);
	}
	
	/**
	 * Renders the currency prefix display element.
	 * <p>
	 * This method creates an interactive display showing the currency's prefix
	 * configuration, including whether a prefix is set and its current value.
	 * </p>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 * @param displayedCurrency the currency being displayed, must not be null
	 */
	private void renderCurrencyPrefix(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer,
		final @NotNull Currency displayedCurrency
	) {
		final String currencyPrefix = displayedCurrency.getPrefix();
		final boolean hasPrefixConfigured = currencyPrefix != null && !currencyPrefix.isEmpty();
		
		renderContext.layoutSlot(
			'p',
			UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
			                     .setName(this.i18n(
				                     "currency_prefix.name",
				                     contextPlayer
			                     ).build().component())
			                     .setLore(this.i18n(
				                                  "currency_prefix.lore",
				                                  contextPlayer
			                                  )
			                                  .withAll(Map.of(
				                                  "currency_prefix",
				                                  hasPrefixConfigured ? currencyPrefix : "None",
				                                  "has_prefix",
				                                  hasPrefixConfigured ? "true" : "false"
			                                  ))
			                                  .build().splitLines())
			                     .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			                     .build()
		);
	}
	
	/**
	 * Renders the currency suffix display element.
	 * <p>
	 * This method creates an interactive display showing the currency's suffix
	 * configuration, including whether a suffix is set and its current value.
	 * </p>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 * @param displayedCurrency the currency being displayed, must not be null
	 */
	private void renderCurrencySuffix(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer,
		final @NotNull Currency displayedCurrency
	) {
		final String currencySuffix = displayedCurrency.getSuffix();
		final boolean hasSuffixConfigured = currencySuffix != null && !currencySuffix.isEmpty();
		
		renderContext.layoutSlot(
			'f',
			UnifiedBuilderFactory.item(Material.PAPER)
			                     .setName(this.i18n(
				                     "currency_suffix.name",
				                     contextPlayer
			                     ).build().component())
			                     .setLore(this.i18n(
				                                  "currency_suffix.lore",
				                                  contextPlayer
			                                  )
			                                  .withAll(Map.of(
				                                  "currency_suffix",
				                                  hasSuffixConfigured ? currencySuffix : "None",
				                                  "has_suffix",
				                                  hasSuffixConfigured ? "true" : "false"
			                                  ))
			                                  .build().splitLines())
			                     .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			                     .build()
		);
	}
	
	/**
	 * Renders the leaderboard access button.
	 * <p>
	 * This method creates an interactive button that allows users to navigate
	 * to the currency leaderboard view to see top player rankings.
	 * </p>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 * @param displayedCurrency the currency being displayed, must not be null
	 */
	private void renderLeaderboardAccess(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer,
		final @NotNull Currency displayedCurrency
	) {
		renderContext.layoutSlot(
			'l',
			UnifiedBuilderFactory.item(new Leaderboard().getHead(contextPlayer))
			                     .setName(this.i18n(
				                                  "leaderboard.name",
				                                  contextPlayer
			                                  )
			                                  .with(
				                                  "currency_identifier",
				                                  displayedCurrency.getIdentifier()
			                                  )
			                                  .build().component())
			                     .setLore(this.i18n(
				                                  "leaderboard.lore",
				                                  contextPlayer
			                                  )
			                                  .withAll(Map.of(
				                                  "currency_identifier",
				                                  displayedCurrency.getIdentifier(),
				                                  "currency_symbol",
				                                  displayedCurrency.getSymbol()
			                                  ))
			                                  .build().splitLines())
			                     .build()
		).onClick(clickContext -> {
			clickContext.openForPlayer(
				CurrencyLeaderboardView.class,
				Map.of(
					"plugin",
					this.jexEconomy.get(clickContext),
					"currency",
					this.targetCurrency.get(clickContext),
					"parentClazz",
					CurrencyDetailView.class,
					"initialData",
					clickContext.getInitialData()
				)
			);
		});
	}
	
	/**
	 * Renders the administrative reset function button.
	 * <p>
	 * This method creates a permission-restricted button that allows authorized
	 * users to reset all player balances for the displayed currency to zero.
	 * The button is only visible to users with appropriate permissions.
	 * </p>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 * @param displayedCurrency the currency being displayed, must not be null
	 * @param pluginInstance the main plugin instance, must not be null
	 */
	private void renderResetFunction(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer,
		final @NotNull Currency displayedCurrency,
		final @NotNull JExEconomy pluginInstance
	) {
		renderContext.layoutSlot(
			'r',
			UnifiedBuilderFactory.item(new Cancel().getHead(contextPlayer))
			                     .setName(this.i18n(
				                                  "reset_all.name",
				                                  contextPlayer
			                                  )
			                                  .with(
				                                  "currency_identifier",
				                                  displayedCurrency.getIdentifier()
			                                  )
			                                  .build().component())
			                     .setLore(this.i18n(
				                                  "reset_all.lore",
				                                  contextPlayer
			                                  )
			                                  .withAll(Map.of(
				                                  "currency_identifier",
				                                  displayedCurrency.getIdentifier(),
				                                  "currency_symbol",
				                                  displayedCurrency.getSymbol()
			                                  ))
			                                  .build().splitLines())
			                     .build()
		).displayIf(clickContext -> {
			final Player viewingPlayer = clickContext.getPlayer();
			return this.hasResetPermission(viewingPlayer);
		}).onClick(clickContext -> {
			this.handleResetAllBalances(
				clickContext,
				displayedCurrency,
				pluginInstance,
				contextPlayer
			);
		});
	}
	
	/**
	 * Checks if a player has permission to use the reset functionality.
	 * <p>
	 * This method verifies that the player has one of the required permissions
	 * or is a server operator, allowing them to access administrative functions.
	 * </p>
	 *
	 * @param targetPlayer the player to check permissions for, must not be null
	 * @return true if the player has reset permissions, false otherwise
	 */
	private boolean hasResetPermission(
		final @NotNull Player targetPlayer
	) {
		return targetPlayer.hasPermission("jexeconomy.admin.reset") ||
		       targetPlayer.hasPermission("jexeconomy.admin.*") ||
		       targetPlayer.isOp();
	}
	
	/**
	 * Handles the reset all balances functionality with confirmation dialog.
	 * <p>
	 * This method initiates the process of resetting all player balances for the
	 * specified currency. It first retrieves all user-currency associations,
	 * calculates the total amount to be reset, and then presents a confirmation
	 * dialog to the user before proceeding with the operation.
	 * </p>
	 *
	 * <h3>Process Flow:</h3>
	 * <ol>
	 *   <li>Retrieve all user-currency associations for the target currency</li>
	 *   <li>Calculate total amount and player count</li>
	 *   <li>Present confirmation dialog with detailed information</li>
	 *   <li>Execute reset operation if confirmed</li>
	 * </ol>
	 *
	 * @param clickContext the context from the button click, must not be null
	 * @param targetCurrency the currency to reset balances for, must not be null
	 * @param pluginInstance the main plugin instance, must not be null
	 * @param requestingPlayer the player requesting the reset, must not be null
	 */
	private void handleResetAllBalances(
		final @NotNull Context clickContext,
		final @NotNull Currency targetCurrency,
		final @NotNull JExEconomy pluginInstance,
		final @NotNull Player requestingPlayer
	) {
		pluginInstance
			.getUserCurrencyRepository()
			.findListByAttributesAsync(
				Map.of(
					"currency.id",
					targetCurrency.getId()
				)
			)
			.thenAcceptAsync(
				retrievedUserCurrencies -> {
					if (
						retrievedUserCurrencies == null ||
						retrievedUserCurrencies.isEmpty()
					) {
						this.i18n(
							    "reset_all.no_players",
							    requestingPlayer
						    )
						    .withPrefix()
						    .with(
							    "currency_identifier",
							    targetCurrency.getIdentifier()
							)
						    .send();
						return;
					}
					
					final double totalAmountToReset = retrievedUserCurrencies.stream()
					                                                         .mapToDouble(UserCurrency::getBalance)
					                                                         .sum();

                    pluginInstance.getPlatform().getScheduler().runSync(
                            () -> this.showResetConfirmationDialog(
                                    clickContext,
                                    targetCurrency,
                                    pluginInstance,
                                    requestingPlayer,
                                    retrievedUserCurrencies,
                                    totalAmountToReset
                            )
                    );
				},
				this.jexEconomy.get(clickContext).getExecutor()
			)
			.exceptionally(resetException -> {
				this.i18n(
					    "reset_all.error",
					    requestingPlayer
				    )
				    .withPrefix()
				    .with(
					    "error",
					    resetException.getMessage()
				    )
				    .send();
				return null;
			});
	}
	
	/**
	 * Shows the confirmation dialog for the reset operation.
	 * <p>
	 * This method creates and displays a confirmation dialog with detailed
	 * information about the reset operation, including the number of players
	 * affected and the total amount to be reset.
	 * </p>
	 *
	 * @param clickContext the context from the button click, must not be null
	 * @param targetCurrency the currency to reset balances for, must not be null
	 * @param pluginInstance the main plugin instance, must not be null
	 * @param requestingPlayer the player requesting the reset, must not be null
	 * @param userCurrenciesToReset the list of user currencies to reset, must not be null
	 * @param totalAmountToReset the total amount that will be reset
	 */
	private void showResetConfirmationDialog(
		final @NotNull Context clickContext,
		final @NotNull Currency targetCurrency,
		final @NotNull JExEconomy pluginInstance,
		final @NotNull Player requestingPlayer,
		final @NotNull List<UserCurrency> userCurrenciesToReset,
		final double totalAmountToReset
	) {
		new ConfirmationView.Builder()
			.withKey("currency_detail_ui.reset_all.confirm")
			.withMessageKey("currency_detail_ui.reset_all.confirm.message")
			.withInitialData(Map.of(
				"plugin",
				pluginInstance,
				"currency",
				targetCurrency,
				"userCurrencies",
				userCurrenciesToReset,
				"total_amount",
				totalAmountToReset,
				"player_count",
				userCurrenciesToReset.size(),
				"currency_identifier",
				targetCurrency.getIdentifier(),
				"currency_symbol",
				targetCurrency.getSymbol(),
				"total_amount_formatted",
				BALANCE_DECIMAL_FORMAT.format(totalAmountToReset)
			))
			.withCallback(confirmationResult -> {
				if (
					confirmationResult
				) {
					this.executeResetOperation(
						userCurrenciesToReset,
						targetCurrency,
						pluginInstance,
						requestingPlayer,
						totalAmountToReset
					);
				} else {
					this.handleResetCancellation(requestingPlayer, targetCurrency);
				}
			})
			.withParentView(CurrencyDetailView.class)
			.openFor(
				clickContext,
				requestingPlayer
			);
	}
	
	/**
	 * Handles the cancellation of the reset operation.
	 * <p>
	 * This method sends a cancellation message to the requesting player
	 * when they choose not to proceed with the reset operation.
	 * </p>
	 *
	 * @param requestingPlayer the player who cancelled the reset, must not be null
	 * @param targetCurrency the currency that was going to be reset, must not be null
	 */
	private void handleResetCancellation(
		final @NotNull Player requestingPlayer,
		final @NotNull Currency targetCurrency
	) {
		this.i18n(
			    "reset_all.cancelled",
			    requestingPlayer
		    )
		    .withPrefix()
		    .with(
			    "currency_identifier",
			    targetCurrency.getIdentifier()
		    )
		    .send();
	}
	
	/**
	 * Executes the actual reset operation after confirmation.
	 * <p>
	 * This method performs the balance reset operation by setting all player
	 * balances to zero and updating the database. It provides feedback to the
	 * requesting player about the operation's progress and results.
	 * </p>
	 *
	 * <h3>Operation Steps:</h3>
	 * <ol>
	 *   <li>Send processing notification to the requesting player</li>
	 *   <li>Reset all balances to zero</li>
	 *   <li>Update database records asynchronously</li>
	 *   <li>Send success confirmation with statistics</li>
	 * </ol>
	 *
	 * @param userCurrenciesToReset the list of user currencies to reset, must not be null
	 * @param targetCurrency the currency being reset, must not be null
	 * @param pluginInstance the main plugin instance, must not be null
	 * @param requestingPlayer the player who requested the reset, must not be null
	 * @param totalAmountReset the total amount that was reset
	 */
	private void executeResetOperation(
		final @NotNull List<UserCurrency> userCurrenciesToReset,
		final @NotNull Currency targetCurrency,
		final @NotNull JExEconomy pluginInstance,
		final @NotNull Player requestingPlayer,
		final double totalAmountReset
	) {
		this.i18n(
			    "reset_all.processing",
			    requestingPlayer
		    )
		    .withPrefix()
		    .withAll(Map.of(
			    "currency_identifier",
			    targetCurrency.getIdentifier(),
			    "player_count",
			    userCurrenciesToReset.size()
		    ))
		    .send();
		
		try {
			userCurrenciesToReset.forEach(userCurrencyToReset -> {
				userCurrencyToReset.setBalance(0.0);
				pluginInstance.getUserCurrencyRepository().updateAsync(userCurrencyToReset);
			});
			
			this.sendResetSuccessMessage(
				requestingPlayer,
				targetCurrency,
				userCurrenciesToReset.size(),
				totalAmountReset
			);
		} catch (
			  final Exception resetExecutionException
		) {
			this.sendResetErrorMessage(
				requestingPlayer,
				targetCurrency,
				resetExecutionException
			);
		}
	}
	
	/**
	 * Sends a success message after completing the reset operation.
	 * <p>
	 * This method sends a detailed success message to the requesting player
	 * with statistics about the completed reset operation.
	 * </p>
	 *
	 * @param requestingPlayer the player who requested the reset, must not be null
	 * @param targetCurrency the currency that was reset, must not be null
	 * @param affectedPlayerCount the number of players whose balances were reset
	 * @param totalAmountReset the total amount that was reset
	 */
	private void sendResetSuccessMessage(
		final @NotNull Player requestingPlayer,
		final @NotNull Currency targetCurrency,
		final int affectedPlayerCount,
		final double totalAmountReset
	) {
		this.i18n(
			    "reset_all.success",
			    requestingPlayer
		    )
		    .withPrefix()
		    .withAll(Map.of(
			    "currency_identifier",
			    targetCurrency.getIdentifier(),
			    "player_count",
			    affectedPlayerCount,
			    "total_amount_reset",
			    BALANCE_DECIMAL_FORMAT.format(totalAmountReset),
			    "currency_symbol",
			    targetCurrency.getSymbol()
		    ))
		    .send();
	}
	
	/**
	 * Sends an error message when the reset operation fails.
	 * <p>
	 * This method sends an error message to the requesting player when
	 * the reset operation encounters an exception during execution.
	 * </p>
	 *
	 * @param requestingPlayer the player who requested the reset, must not be null
	 * @param targetCurrency the currency that failed to reset, must not be null
	 * @param resetException the exception that occurred during reset, must not be null
	 */
	private void sendResetErrorMessage(
		final @NotNull Player requestingPlayer,
		final @NotNull Currency targetCurrency,
		final @NotNull Exception resetException
	) {
		this.i18n(
			    "reset_all.execution_error",
			    requestingPlayer
		    )
		    .withPrefix()
		    .withAll(Map.of(
			    "currency_identifier",
			    targetCurrency.getIdentifier(),
			    "error",
			    resetException.getMessage()
		    ))
		    .send();
	}
}