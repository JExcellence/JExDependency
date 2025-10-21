package de.jexcellence.economy.currency;

import com.raindropcentral.rplatform.utility.heads.view.Cancel;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import com.raindropcentral.rplatform.view.ConfirmationView;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.CurrencyLog;
import de.jexcellence.economy.database.entity.UserCurrency;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Paginated view for selecting a currency to delete with comprehensive safety measures.
 * <p>
 * This view displays all available currencies in a paginated format, allowing
 * administrators to select which currency they want to delete. Each currency entry
 * shows detailed information including the number of affected players and total
 * balances that will be lost. The deletion process includes multiple confirmation
 * steps to prevent accidental data loss.
 * </p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Currency Selection:</strong> Click-to-select interface for currency deletion</li>
 *   <li><strong>Impact Assessment:</strong> Shows affected players and total balances</li>
 *   <li><strong>Safety Measures:</strong> Multiple confirmation dialogs with detailed warnings</li>
 *   <li><strong>Comprehensive Cleanup:</strong> Removes currency and all associated player balances</li>
 *   <li><strong>Permission Checks:</strong> Restricted to administrators with delete permissions</li>
 * </ul>
 *
 * <h3>Safety Features:</h3>
 * <ul>
 *   <li><strong>Impact Display:</strong> Shows number of players and total currency in circulation</li>
 *   <li><strong>Confirmation Dialog:</strong> Requires explicit confirmation before deletion</li>
 *   <li><strong>Irreversible Warning:</strong> Clear warnings about permanent data loss</li>
 *   <li><strong>Permission Validation:</strong> Only accessible to authorized administrators</li>
 * </ul>
 *
 * <h3>Deletion Process:</h3>
 * <ol>
 *   <li>Display currency with impact information</li>
 *   <li>Show confirmation dialog with detailed warnings</li>
 *   <li>Delete all associated UserCurrency records</li>
 *   <li>Update CurrencyLog records to remove foreign key references</li>
 *   <li>Remove currency from database and cache</li>
 *   <li>Provide completion feedback</li>
 * </ol>
 *
 * @author JExcellence
 * @see APaginatedView
 * @see Currency
 * @see UserCurrency
 * @see CurrenciesActionOverviewView
 */
public class CurrencyDeletionView extends APaginatedView<Currency> {
	
	/**
	 * Decimal formatter for displaying currency amounts with thousands separators.
	 */
	private static final DecimalFormat BALANCE_DECIMAL_FORMAT = new DecimalFormat("#,###.##");
	
	/**
	 * State holder for the main JExEconomyImpl plugin instance.
	 */
	private final State<JExEconomyImpl> jexEconomy = initialState("plugin");
	
	/**
	 * Constructs a new {@code CurrencyDeletionView} with the currencies action overview as parent.
	 * <p>
	 * The view will display all available currencies for deletion selection and provide
	 * navigation back to the currencies action overview when closed.
	 * </p>
	 */
	public CurrencyDeletionView() {
		super(CurrenciesActionOverviewView.class);
	}
	
	/**
	 * Returns the internationalization key for this view.
	 * <p>
	 * This key is used to load localized strings for the currency deletion selection
	 * interface, including titles, labels, warnings, and formatting templates.
	 * </p>
	 *
	 * @return the i18n key for the currency deletion selection UI
	 */
	@Override
	protected @NotNull String getKey() {
		return "currency_deletion_ui";
	}
	
	/**
	 * Provides the asynchronous data source for the currencies pagination.
	 * <p>
	 * This method retrieves all available currencies from the repository with pagination
	 * support. The results are limited to 128 currencies to optimize performance.
	 * </p>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @return a future containing the list of currencies for pagination
	 */
	@Override
	protected @NotNull CompletableFuture<List<Currency>> getAsyncPaginationSource(
		final @NotNull Context renderContext
	) {
		return this.jexEconomy.get(renderContext).getCurrencyRepository().findAllAsync(
			1,
			128
		);
	}
	
	/**
	 * Renders a single currency entry in the pagination view with deletion warnings.
	 * <p>
	 * This method creates a visual representation of a currency with a warning-style
	 * appearance using barrier blocks and red coloring to emphasize the destructive
	 * nature of the deletion operation. Each entry shows impact information including
	 * affected players and total balances.
	 * </p>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param itemBuilder the item component builder for creating the display item, must not be null
	 * @param entryIndex the zero-based index of this entry in the pagination
	 * @param displayedCurrency the currency entity to render, must not be null
	 */
	@Override
	protected void renderEntry(
		final @NotNull Context renderContext,
		final @NotNull BukkitItemComponentBuilder itemBuilder,
		final int entryIndex,
		final @NotNull Currency displayedCurrency
	) {
		final Player         contextPlayer  = renderContext.getPlayer();
		final JExEconomyImpl jexEconomyImpl = this.jexEconomy.get(renderContext);
		
		final List<UserCurrency> userCurrencies = jexEconomyImpl.getUserCurrencyRepository().findListByAttributes(
			Map.of("currency.id", displayedCurrency.getId())
		);
		
		final int affectedPlayers = userCurrencies != null ? userCurrencies.size() : 0;
		final double totalBalance = userCurrencies != null ?
		                            userCurrencies.stream()
		                                          .mapToDouble(UserCurrency::getBalance)
		                                          .sum() : 0.0;
		itemBuilder
			.withItem(
				UnifiedBuilderFactory
					.item(new Cancel().getHead(contextPlayer))
					.setName(
						this.i18n(
							    "currency.name",
							    contextPlayer
						    )
						    .withAll(Map.of(
							    "currency_identifier",
							    displayedCurrency.getIdentifier(),
							    "currency_symbol",
							    displayedCurrency.getSymbol()
						    ))
						    .build()
						    .component()
					)
					.setLore(
						this.i18n(
							    "currency.lore",
							    contextPlayer
						    )
						    .withAll(Map.of(
							    "currency_identifier",
							    displayedCurrency.getIdentifier(),
							    "currency_symbol",
							    displayedCurrency.getSymbol(),
							    "affected_players",
							    affectedPlayers,
							    "total_balance",
							    BALANCE_DECIMAL_FORMAT.format(totalBalance),
							    "index",
							    entryIndex + 1
						    ))
						    .build()
						    .splitLines()
					)
					.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
					.build()
			)
			.onClick(clickContext -> {
				this.handleCurrencyDeletion(
					clickContext,
					displayedCurrency,
					affectedPlayers,
					totalBalance
				);
			});
	}
	
	/**
	 * Defines the layout structure for the currency deletion selection view.
	 * <p>
	 * The layout provides a clean, organized display with currency entries in the center
	 * and navigation controls below. The layout emphasizes the serious nature of the
	 * deletion operation.
	 * </p>
	 *
	 * @return the layout pattern as a string array, never null
	 */
	@Override
	protected @NotNull String[] getLayout() {
		return new String[]{
			"         ",
			" OOOOOOO ",
			"   <p>   ",
			"         "
		};
	}
	
	/**
	 * Handles additional rendering logic specific to the currency deletion selection view.
	 * <p>
	 * Currently, no additional rendering is required for the currency deletion selection,
	 * as all necessary elements are handled by the pagination system.
	 * </p>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 */
	@Override
	protected void onPaginatedRender(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer
	) {
		// No additional rendering required for the currency deletion selection
		// All necessary elements are handled by the pagination system
	}
	
	/**
	 * Handles the currency deletion process with comprehensive safety measures.
	 * <p>
	 * This method initiates the currency deletion workflow, including impact assessment,
	 * confirmation dialogs, and the actual deletion process. It provides detailed
	 * warnings about the irreversible nature of the operation.
	 * </p>
	 *
	 * @param clickContext the context from the currency selection click, must not be null
	 * @param currencyToDelete the currency selected for deletion, must not be null
	 * @param affectedPlayers the number of players who will lose balances, must be non-negative
	 * @param totalBalance the total amount of currency that will be lost
	 */
	private void handleCurrencyDeletion(
		final @NotNull Context clickContext,
		final @NotNull Currency currencyToDelete,
		final int affectedPlayers,
		final double totalBalance
	) {
		final Player         requestingPlayer = clickContext.getPlayer();
		final JExEconomyImpl jexEconomyImpl   = this.jexEconomy.get(clickContext);
		
		new ConfirmationView.Builder()
			.withKey("currency_deletion_ui")
			.withMessageKey("currency_deletion_ui.confirm.message")
			.withInitialData(Map.of(
				"plugin",
				jexEconomyImpl,
				"currency",
				currencyToDelete,
				"affected_players",
				affectedPlayers,
				"total_balance",
				totalBalance,
				"currency_identifier",
				currencyToDelete.getIdentifier(),
				"currency_symbol",
				currencyToDelete.getSymbol(),
				"total_balance_formatted",
				BALANCE_DECIMAL_FORMAT.format(totalBalance)
			))
			.withCallback(confirmationResult -> {
				if (confirmationResult) {
					this.executeCurrencyDeletion(
						currencyToDelete,
						jexEconomyImpl,
						requestingPlayer,
						affectedPlayers,
						totalBalance
					);
					requestingPlayer.closeInventory();
				} else {
					this.handleDeletionCancellation(requestingPlayer, currencyToDelete);
				}
			})
			.withParentView(CurrencyDeletionView.class)
			.openFor(clickContext, requestingPlayer);
	}
	
	/**
	 * Handles the cancellation of the currency deletion operation.
	 * <p>
	 * This method sends a cancellation message to the requesting player
	 * when they choose not to proceed with the deletion operation.
	 * </p>
	 *
	 * @param requestingPlayer the player who cancelled the deletion, must not be null
	 * @param currencyToDelete the currency that was going to be deleted, must not be null
	 */
	private void handleDeletionCancellation(
		final @NotNull Player requestingPlayer,
		final @NotNull Currency currencyToDelete
	) {
		this.i18n(
			    "delete.cancelled",
			    requestingPlayer
		    )
		    .withPrefix()
		    .with(
			    "currency_identifier",
			    currencyToDelete.getIdentifier()
		    )
		    .send();
	}
	
	/**
	 * Executes the actual currency deletion after confirmation.
	 * <p>
	 * This method performs the complete deletion process including removing
	 * all associated UserCurrency records, updating CurrencyLog records to
	 * remove foreign key references, deleting the currency from the database,
	 * updating the cache, and providing user feedback.
	 * </p>
	 *
	 * <h3>Deletion Steps:</h3>
	 * <ol>
	 *   <li>Send processing notification</li>
	 *   <li>Delete all associated UserCurrency records</li>
	 *   <li>Update CurrencyLog records to remove currency reference (preserve audit trail)</li>
	 *   <li>Delete the currency from the database</li>
	 *   <li>Remove currency from in-memory cache</li>
	 *   <li>Send completion confirmation</li>
	 * </ol>
	 *
	 * @param currencyToDelete the currency to delete, must not be null
	 * @param jexEconomyImpl the main plugin instance, must not be null
	 * @param requestingPlayer the player who requested the deletion, must not be null
	 * @param affectedPlayers the number of players who will lose balances
	 * @param totalBalance the total amount of currency being deleted
	 */
	private void executeCurrencyDeletion(
		final @NotNull Currency currencyToDelete,
		final @NotNull JExEconomyImpl jexEconomyImpl,
		final @NotNull Player requestingPlayer,
		final int affectedPlayers,
		final double totalBalance
	) {
		this.i18n(
			    "delete.processing",
			    requestingPlayer
		    )
		    .withPrefix()
		    .withAll(Map.of(
			    "currency_identifier",
			    currencyToDelete.getIdentifier(),
			    "affected_players",
			    affectedPlayers
		    ))
		    .send();
		
		// Execute the deletion process asynchronously
		CompletableFuture.supplyAsync(() -> {
			try {
				// Step 1: Delete all UserCurrency records that reference this currency
				final List<UserCurrency> userCurrencies = jexEconomyImpl.getUserCurrencyRepository()
				                                                        .findListByAttributes(
					                                                     Map.of("currency.id", currencyToDelete.getId())
				                                                     );
				
				if (userCurrencies != null && !userCurrencies.isEmpty()) {
					for (UserCurrency userCurrency : userCurrencies) {
						jexEconomyImpl.getUserCurrencyRepository().delete(userCurrency.getId());
					}
					jexEconomyImpl.getPlugin().getLogger().info(
						"Deleted " + userCurrencies.size() + " UserCurrency records for currency: " + currencyToDelete.getIdentifier()
					);
				}
				
				// Step 2: Update CurrencyLog records to remove currency reference (preserve audit trail)
				final List<CurrencyLog> currencyLogs = jexEconomyImpl.getCurrencyLogRepository()
				                                                     .findByCriteria(
					                                                  null, // any log type
					                                                  null, // any log level
					                                                  null, // any player
					                                                  currencyToDelete.getId(), // specific currency
					                                                  null, // any start time
					                                                  null, // any end time
					                                                  Integer.MAX_VALUE // get all records
				                                                  );
				
				int updatedLogCount = 0;
				if (!currencyLogs.isEmpty()) {
					for (CurrencyLog log : currencyLogs) {
						log.setCurrency(null); // Remove currency reference but keep all other data
						jexEconomyImpl.getCurrencyLogRepository().update(log);
						updatedLogCount++;
					}
					jexEconomyImpl.getPlugin().getLogger().info(
						"Updated " + updatedLogCount + " CurrencyLog records to remove currency reference for currency: " + currencyToDelete.getIdentifier()
					);
				}
				
				// Step 3: Delete the currency itself
				boolean deletionSuccessful = jexEconomyImpl.getCurrencyRepository().delete(currencyToDelete.getId());
				
				if (deletionSuccessful) {
					// Step 4: Remove from in-memory cache
					jexEconomyImpl.getCurrencies().remove(currencyToDelete.getId());
					return true;
				} else {
					throw new RuntimeException("Failed to delete currency from database");
				}
				
			} catch (Exception e) {
				jexEconomyImpl.getPlugin().getLogger().log(
					Level.SEVERE,
					"Failed to delete currency: " + currencyToDelete.getIdentifier(),
					e
				);
				throw new RuntimeException("Currency deletion failed: " + e.getMessage(), e);
			}
		}, jexEconomyImpl.getExecutor())
		.thenAcceptAsync(deletionSuccessful -> {
			// Step 5: Send success message
			this.sendDeletionSuccessMessage(
				requestingPlayer,
				currencyToDelete,
				affectedPlayers,
				totalBalance
			);
		}, jexEconomyImpl.getExecutor())
		.exceptionally(deletionException -> {
			// Handle any errors that occurred during the deletion process
			this.sendDeletionErrorMessage(
				requestingPlayer,
				currencyToDelete,
				deletionException
			);
			return null;
		});
	}
	
	/**
	 * Sends a success message after completing the currency deletion.
	 * <p>
	 * This method sends a detailed success message to the requesting player
	 * with statistics about the completed deletion operation.
	 * </p>
	 *
	 * @param requestingPlayer the player who requested the deletion, must not be null
	 * @param deletedCurrency the currency that was deleted, must not be null
	 * @param affectedPlayers the number of players whose balances were removed
	 * @param totalBalance the total amount of currency that was removed
	 */
	private void sendDeletionSuccessMessage(
		final @NotNull Player requestingPlayer,
		final @NotNull Currency deletedCurrency,
		final int affectedPlayers,
		final double totalBalance
	) {
		this.i18n(
			    "delete.success",
			    requestingPlayer
		    )
		    .withPrefix()
		    .withAll(Map.of(
			    "currency_identifier",
			    deletedCurrency.getIdentifier(),
			    "affected_players",
			    affectedPlayers,
			    "total_balance_removed",
			    BALANCE_DECIMAL_FORMAT.format(totalBalance),
			    "currency_symbol",
			    deletedCurrency.getSymbol()
		    ))
		    .send();
	}
	
	/**
	 * Sends a failure message when the currency deletion fails.
	 * <p>
	 * This method sends a failure message to the requesting player when
	 * the deletion operation fails at the database level.
	 * </p>
	 *
	 * @param requestingPlayer the player who requested the deletion, must not be null
	 * @param currencyToDelete the currency that failed to delete, must not be null
	 */
	private void sendDeletionFailureMessage(
		final @NotNull Player requestingPlayer,
		final @NotNull Currency currencyToDelete
	) {
		this.i18n(
			    "delete.failed",
			    requestingPlayer
		    )
		    .withPrefix()
		    .with(
			    "currency_identifier",
			    currencyToDelete.getIdentifier()
		    )
		    .send();
	}
	
	/**
	 * Sends an error message when the currency deletion encounters an exception.
	 * <p>
	 * This method sends detailed error information to the requesting player
	 * when the deletion operation encounters an unexpected exception.
	 * </p>
	 *
	 * @param requestingPlayer the player who requested the deletion, must not be null
	 * @param currencyToDelete the currency that failed to delete, must not be null
	 * @param deletionException the exception that occurred during deletion, must not be null
	 */
	private void sendDeletionErrorMessage(
		final @NotNull Player requestingPlayer,
		final @NotNull Currency currencyToDelete,
		final @NotNull Throwable deletionException
	) {
		this.i18n(
			    "delete.error",
			    requestingPlayer
		    )
		    .withPrefix()
		    .withAll(Map.of(
			    "currency_identifier",
			    currencyToDelete.getIdentifier(),
			    "error",
			    deletionException.getMessage()
		    ))
		    .send();
	}
}