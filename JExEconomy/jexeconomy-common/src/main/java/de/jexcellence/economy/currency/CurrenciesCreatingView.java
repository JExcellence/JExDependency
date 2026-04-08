package de.jexcellence.economy.currency;

import com.raindropcentral.rplatform.utility.heads.view.Proceed;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.currency.anvil.CurrencyIconAnvilView;
import de.jexcellence.economy.currency.anvil.CurrencyIdentifierAnvilView;
import de.jexcellence.economy.currency.anvil.CurrencyPrefixAnvilView;
import de.jexcellence.economy.currency.anvil.CurrencySuffixAnvilView;
import de.jexcellence.economy.currency.anvil.CurrencySymbolAnvilView;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.User;
import de.jexcellence.economy.database.entity.UserCurrency;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * Interactive view for creating and configuring new currencies in the system.
 *
 * <p>This view provides a comprehensive interface for currency creation, allowing users to
 * configure all aspects of a currency including identifier, symbol, icon, prefix, and suffix.
 * The view integrates with multiple anvil input views for detailed property configuration
 * and handles the complete currency creation workflow including database persistence
 * and player account initialization.
 *
 * <p><strong>Key Features:</strong>
 * <ul>
 *   <li><strong>Interactive Configuration:</strong> Click-to-edit interface for all currency properties</li>
 *   <li><strong>Real-time Validation:</strong> Dynamic validation and state management</li>
 *   <li><strong>Complete Workflow:</strong> Handles creation, validation, and account initialization</li>
 *   <li><strong>Error Handling:</strong> Comprehensive error handling with user feedback</li>
 *   <li><strong>State Management:</strong> Maintains currency state across view transitions</li>
 * </ul>
 *
 * <p><strong>Configuration Elements:</strong>
 * <ul>
 *   <li><strong>Identifier (n):</strong> Unique currency identifier configuration</li>
 *   <li><strong>Symbol (s):</strong> Currency symbol and visual representation</li>
 *   <li><strong>Icon (i):</strong> Material icon for graphical interfaces</li>
 *   <li><strong>Prefix (p):</strong> Text displayed before currency amounts</li>
 *   <li><strong>Suffix (f):</strong> Text displayed after currency amounts</li>
 *   <li><strong>Create Button (c):</strong> Finalizes currency creation process</li>
 * </ul>
 *
 * <p><strong>Creation Workflow:</strong>
 * <ol>
 *   <li>Configure currency properties through anvil input views</li>
 *   <li>Validate required fields (identifier and symbol)</li>
 *   <li>Check for existing currencies with the same identifier</li>
 *   <li>Create currency entity in the database</li>
 *   <li>Initialize player accounts for the new currency</li>
 *   <li>Provide user feedback and reset state for next creation</li>
 * </ol>
 *
 * @author JExcellence
 * @see BaseView
 * @see Currency
 * @see UserCurrency
 * @see CurrencyIdentifierAnvilView
 * @see CurrencySymbolAnvilView
 * @see CurrencyIconAnvilView
 * @see CurrencyPrefixAnvilView
 * @see CurrencySuffixAnvilView
 */
public class CurrenciesCreatingView extends BaseView {
	
	/**
	 * State holder for the main JExEconomy plugin instance.
	 */
	private final State<JExEconomy> jexEconomy = initialState("plugin");
	
	/**
	 * Mutable state holder for the currency being created or edited.
	 */
	private final MutableState<Currency> targetCurrency = mutableState(new Currency(""));
	
	/**
	 * Constructs a new {@code CurrenciesCreatingView} with the currencies action overview as parent.
 *
 * <p>The view will provide currency creation functionality and navigate back to the
	 * currencies action overview when closed or cancelled.
	 */
	public CurrenciesCreatingView() {
		super(CurrenciesActionOverviewView.class);
	}
	
	/**
	 * Returns the internationalization key for this view.
 *
 * <p>This key is used to load localized strings for the currency creation interface,
	 * including titles, labels, and formatting templates.
	 *
	 * @return the i18n key for the currencies creating UI
	 */
	@Override
	protected @NotNull String getKey() {
		return "currencies_creating_ui";
	}
	
	/**
	 * Defines the layout structure for the currency creation view.
 *
 * <p>The layout organizes currency configuration elements in a user-friendly
	 * arrangement with the creation button positioned for easy access.
	 *
	 * <p><strong>Layout Mapping:</strong>
	 * <ul>
	 *   <li><strong>n:</strong> Currency identifier configuration</li>
	 *   <li><strong>s:</strong> Currency symbol configuration</li>
	 *   <li><strong>i:</strong> Currency icon configuration</li>
	 *   <li><strong>p:</strong> Currency prefix configuration</li>
	 *   <li><strong>f:</strong> Currency suffix configuration</li>
	 *   <li><strong>c:</strong> Create currency button</li>
	 * </ul>
	 *
	 * @return the layout pattern as a string array, never null
	 */
	@Override
	protected @NotNull String[] getLayout() {
		return new String[]{
			"         ",
			" ns i pf ",
			"         ",
			"        c"
		};
	}
	
	/**
	 * Handles view resumption when returning from anvil input views.
 *
 * <p>This method is called when the user returns from an anvil input view after
	 * configuring a currency property. It updates the currency state with any
	 * changes made in the anvil view and refreshes the interface.
	 *
	 * <p><strong>State Management:</strong>
	 * <ul>
	 *   <li>Extracts updated currency data from the origin context</li>
	 *   <li>Updates the mutable currency state</li>
	 *   <li>Refreshes the view to reflect changes</li>
	 * </ul>
	 *
	 * @param originContext the context from the anvil input view, must not be null
	 * @param targetContext the context of this view being resumed, must not be null
	 */
	@Override
	public void onResume(
		final @NotNull Context originContext,
		final @NotNull Context targetContext
	) {
		final Map<String, Object> originData = (Map<String, Object>) originContext.getInitialData();
		
		if (
			originData.containsKey("currency")
		) {
			final Currency updatedCurrency = (Currency) originData.get("currency");
			this.targetCurrency.set(
				updatedCurrency,
				targetContext
			);
		}
		
		targetContext.update();
	}
	
	/**
	 * Renders all currency configuration elements and the creation button.
 *
 * <p>This method populates the view with interactive elements for configuring
	 * each aspect of the currency. Each element opens a specialized anvil input
	 * view for detailed configuration with validation.
	 *
	 * <p><strong>Rendered Elements:</strong>
	 * <ul>
	 *   <li>Currency identifier configuration button</li>
	 *   <li>Currency symbol configuration button</li>
	 *   <li>Currency icon configuration button</li>
	 *   <li>Currency prefix configuration button</li>
	 *   <li>Currency suffix configuration button</li>
	 *   <li>Create currency button (conditionally displayed)</li>
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
		this.renderIdentifierConfiguration(renderContext, contextPlayer);
		this.renderSymbolConfiguration(renderContext, contextPlayer);
		this.renderIconConfiguration(renderContext, contextPlayer);
		this.renderPrefixConfiguration(renderContext, contextPlayer);
		this.renderSuffixConfiguration(renderContext, contextPlayer);
		this.renderCreationButton(renderContext, contextPlayer);
	}
	
	/**
	 * Renders the currency identifier configuration button.
 *
 * <p>This button opens the {@link CurrencyIdentifierAnvilView} for configuring
	 * the currency's unique identifier with validation and uniqueness checking.
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 */
	private void renderIdentifierConfiguration(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer
	) {
		renderContext
			.layoutSlot(
				'n',
				UnifiedBuilderFactory.item(Material.NAME_TAG)
				                     .setName(
					                     this.i18n(
						                     "identifier.name",
						                     contextPlayer
					                     ).build().component()
				                     )
				                     .setLore(
					                     this.i18n(
						                     "identifier.lore",
						                     contextPlayer
					                     ).build().children()
				                     )
				                     .build()
			)
			.onClick(clickContext -> {
				clickContext.openForPlayer(
					CurrencyIdentifierAnvilView.class,
					Map.of(
						"plugin",
						this.jexEconomy.get(clickContext),
						"currency",
						this.targetCurrency.get(clickContext)
					)
				);
			});
	}
	
	/**
	 * Renders the currency symbol configuration button.
 *
 * <p>This button opens the {@link CurrencySymbolAnvilView} for configuring
	 * the currency's visual symbol with length and format validation.
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 */
	private void renderSymbolConfiguration(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer
	) {
		renderContext
			.layoutSlot(
				's',
				UnifiedBuilderFactory.item(Material.GOLD_NUGGET)
				                     .setName(
					                     this.i18n(
						                     "symbol.name",
						                     contextPlayer
					                     ).build().component()
				                     )
				                     .setLore(
					                     this.i18n(
						                     "symbol.lore",
						                     contextPlayer
					                     ).build().children()
				                     )
				                     .build()
			)
			.onClick(clickContext -> {
				clickContext.openForPlayer(
					CurrencySymbolAnvilView.class,
					Map.of(
						"plugin",
						this.jexEconomy.get(clickContext),
						"currency",
						this.targetCurrency.get(clickContext)
					)
				);
			});
	}
	
	/**
	 * Renders the currency icon configuration button.
 *
 * <p>This button opens the {@link CurrencyIconAnvilView} for configuring
	 * the currency's material icon with material validation.
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 */
	private void renderIconConfiguration(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer
	) {
		renderContext
			.layoutSlot(
				'i',
				UnifiedBuilderFactory.item(Material.ITEM_FRAME)
				                     .setName(
					                     this.i18n(
						                     "icon.name",
						                     contextPlayer
					                     ).build().component()
				                     )
				                     .setLore(
					                     this.i18n(
						                     "icon.lore",
						                     contextPlayer
					                     ).build().children()
				                     )
				                     .build()
			)
			.onClick(clickContext -> {
				clickContext.openForPlayer(
					CurrencyIconAnvilView.class,
					Map.of(
						"plugin",
						this.jexEconomy.get(clickContext),
						"currency",
						this.targetCurrency.get(clickContext)
					)
				);
			});
	}
	
	/**
	 * Renders the currency prefix configuration button.
 *
 * <p>This button opens the {@link CurrencyPrefixAnvilView} for configuring
	 * the currency's prefix text with length validation.
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 */
	private void renderPrefixConfiguration(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer
	) {
		renderContext
			.layoutSlot(
				'p',
				UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
				                     .setName(
					                     this.i18n(
						                     "prefix.name",
						                     contextPlayer
					                     ).build().component()
				                     )
				                     .setLore(
					                     this.i18n(
						                     "prefix.lore",
						                     contextPlayer
					                     ).build().children()
				                     )
				                     .build()
			)
			.onClick(clickContext -> {
				clickContext.openForPlayer(
					CurrencyPrefixAnvilView.class,
					Map.of(
						"plugin",
						this.jexEconomy.get(clickContext),
						"currency",
						this.targetCurrency.get(clickContext)
					)
				);
			});
	}
	
	/**
	 * Renders the currency suffix configuration button.
 *
 * <p>This button opens the {@link CurrencySuffixAnvilView} for configuring
	 * the currency's suffix text with length validation.
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 */
	private void renderSuffixConfiguration(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer
	) {
		renderContext
			.layoutSlot(
				'f',
				UnifiedBuilderFactory.item(Material.PAPER)
				                     .setName(
					                     this.i18n(
						                     "suffix.name",
						                     contextPlayer
					                     ).build().component()
				                     )
				                     .setLore(
					                     this.i18n(
						                     "suffix.lore",
						                     contextPlayer
					                     ).build().children()
				                     )
				                     .build()
			)
			.onClick(clickContext -> {
				clickContext.openForPlayer(
					CurrencySuffixAnvilView.class,
					Map.of(
						"plugin",
						this.jexEconomy.get(clickContext),
						"currency",
						this.targetCurrency.get(clickContext)
					)
				);
			});
	}
	
	/**
	 * Renders the currency creation button with conditional display logic.
 *
 * <p>This button is only displayed when the currency has the required fields
	 * (identifier and symbol) configured. It initiates the currency creation
	 * process when clicked.
	 *
	 * <p><strong>Display Conditions:</strong>
	 * <ul>
	 *   <li>Currency identifier must not be empty</li>
	 *   <li>Currency symbol must not be empty</li>
	 * </ul>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 */
	private void renderCreationButton(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer
	) {
		renderContext
			.layoutSlot(
				'c',
				UnifiedBuilderFactory.item(new Proceed().getHead(contextPlayer))
				                     .setName(
					                     this.i18n(
						                     "create_currency.name",
						                     contextPlayer
					                     ).build().component()
				                     )
				                     .setLore(
					                     this.i18n(
						                     "create_currency.lore",
						                     contextPlayer
					                     ).build().children()
				                     )
				                     .build()
			)
			.displayIf(displayContext -> {
				final Currency currentCurrency = this.targetCurrency.get(displayContext);
				return this.isValidForCreation(currentCurrency);
			})
			.watch(this.targetCurrency)
			.updateOnStateChange(this.targetCurrency)
			.onClick(clickContext -> {
				this.handleCurrencyCreation(
					clickContext,
					contextPlayer
				);
			});
	}
	
	/**
	 * Validates if a currency is ready for creation.
 *
 * <p>This method checks that all required fields are properly configured
	 * before allowing the currency creation process to proceed.
	 *
	 * @param currencyToValidate the currency to validate, can be null
	 * @return true if the currency is valid for creation, false otherwise
	 */
	private boolean isValidForCreation(
		final @Nullable Currency currencyToValidate
	) {
		if (
			currencyToValidate == null
		) {
			return false;
		}
		
		final String currencyIdentifier = currencyToValidate.getIdentifier();
		final String currencySymbol = currencyToValidate.getSymbol();
		
		return ! currencyIdentifier.isEmpty() && ! currencySymbol.isEmpty();
	}
	
	/**
	 * Handles the complete currency creation process with validation and error handling.
 *
 * <p>This method orchestrates the entire currency creation workflow including
	 * existence checking, database creation, and player account initialization.
	 * It provides comprehensive user feedback throughout the process.
	 *
	 * <p><strong>Creation Process:</strong>
	 * <ol>
	 *   <li>Extract currency data and plugin instance</li>
	 *   <li>Update view and send processing notification</li>
	 *   <li>Check if currency already exists</li>
	 *   <li>Create currency in database if unique</li>
	 *   <li>Handle success or failure scenarios</li>
	 *   <li>Close view after completion</li>
	 * </ol>
	 *
	 * @param clickContext the context from the creation button click, must not be null
	 * @param requestingPlayer the player requesting currency creation, must not be null
	 */
	private void handleCurrencyCreation(
		final @NotNull Context clickContext,
		final @NotNull Player requestingPlayer
	) {
		final Currency       currencyToCreate = this.targetCurrency.get(clickContext);
		final JExEconomy pluginInstance   = this.jexEconomy.get(clickContext);
		
		clickContext.update();
		
		this.sendProcessingNotification(requestingPlayer, currencyToCreate);
		
		pluginInstance.getCurrencyAdapter()
		              .hasGivenCurrency(currencyToCreate.getIdentifier())
		              .thenAcceptAsync(
			              currencyExists -> {
				              if (
					              currencyExists
				              ) {
					              this.handleCurrencyAlreadyExists(requestingPlayer, currencyToCreate);
					              return;
				              }
				              
				              this.createCurrencyInDatabase(
					              currencyToCreate,
					              pluginInstance,
					              requestingPlayer,
					              clickContext
				              );
			              },
			              pluginInstance.getExecutor()
		              )
		              .exceptionally(creationException -> {
			              this.handleCreationError(requestingPlayer, currencyToCreate, creationException);
			              return null;
		              });
		
		clickContext.closeForPlayer();
	}
	
	/**
	 * Sends a processing notification to the requesting player.
 *
 * <p>This method informs the player that the currency creation process has
	 * started and is being processed in the background.
	 *
	 * @param requestingPlayer the player requesting currency creation, must not be null
	 * @param currencyToCreate the currency being created, must not be null
	 */
	private void sendProcessingNotification(
		final @NotNull Player requestingPlayer,
		final @NotNull Currency currencyToCreate
	) {
		this.i18n(
			    "create.processing",
			    requestingPlayer
		    )
		    .includePrefix()
		    .withPlaceholder(
			    "identifier",
			    currencyToCreate.getIdentifier()
		    )
		    .build().sendMessage();
	}
	
	/**
	 * Handles the scenario where a currency with the same identifier already exists.
 *
 * <p>This method sends an appropriate error message to the requesting player
	 * when they attempt to create a currency with a duplicate identifier.
	 *
	 * @param requestingPlayer the player requesting currency creation, must not be null
	 * @param currencyToCreate the currency that conflicts with an existing one, must not be null
	 */
	private void handleCurrencyAlreadyExists(
		final @NotNull Player requestingPlayer,
		final @NotNull Currency currencyToCreate
	) {
		this.i18n(
			    "create.already_exists",
			    requestingPlayer
		    )
		    .includePrefix()
		    .withPlaceholder(
			    "identifier",
			    currencyToCreate.getIdentifier()
		    )
		    .build().sendMessage();
	}
	
	/**
	 * Handles general creation errors during the currency creation process.
 *
 * <p>This method sends detailed error information to the requesting player
	 * when the currency creation process encounters an unexpected exception.
	 *
	 * @param requestingPlayer the player requesting currency creation, must not be null
	 * @param currencyToCreate the currency that failed to create, must not be null
	 * @param creationException the exception that occurred during creation, must not be null
	 */
	private void handleCreationError(
		final @NotNull Player requestingPlayer,
		final @NotNull Currency currencyToCreate,
		final @NotNull Throwable creationException
	) {
		this.i18n(
			    "create.error",
			    requestingPlayer
		    )
		    .includePrefix()
		    .withPlaceholders(Map.of(
			    "identifier",
			    currencyToCreate.getIdentifier(),
			    "error",
			    creationException.getMessage()
		    ))
		    .build().sendMessage();
	}
	
	/**
	 * Creates the currency in the database and handles the complete workflow.
 *
 * <p>This method performs the actual database creation operation and manages
	 * the subsequent steps including cache updates and player account creation.
	 * It provides comprehensive error handling and user feedback.
	 *
	 * <p><strong>Database Creation Process:</strong>
	 * <ol>
	 *   <li>Create new currency entity with configured properties</li>
	 *   <li>Persist currency to database through adapter</li>
	 *   <li>Retrieve saved currency from cache</li>
	 *   <li>Initialize player accounts for the new currency</li>
	 *   <li>Reset view state for next creation</li>
	 *   <li>Send success confirmation to user</li>
	 * </ol>
	 *
	 * @param currencyToCreate the currency configuration to persist, must not be null
	 * @param pluginInstance the main plugin instance, must not be null
	 * @param requestingPlayer the player requesting currency creation, must not be null
	 * @param viewContext the current view context, must not be null
	 */
	private void createCurrencyInDatabase(
		final @NotNull Currency currencyToCreate,
		final @NotNull JExEconomy pluginInstance,
		final @NotNull Player requestingPlayer,
		final @NotNull Context viewContext
	) {
		final Currency newCurrencyEntity = new Currency(
			currencyToCreate.getPrefix(),
			currencyToCreate.getSuffix(),
			currencyToCreate.getIdentifier(),
			currencyToCreate.getSymbol(),
			currencyToCreate.getIcon()
		);
		
		pluginInstance.getCurrencyAdapter()
		              .createCurrency(newCurrencyEntity)
		              .thenAcceptAsync(
			              creationSuccessful -> {
				              if (
					              creationSuccessful
				              ) {
					              this.handleSuccessfulCreation(
						              newCurrencyEntity,
						              pluginInstance,
						              requestingPlayer,
						              viewContext
					              );
				              } else {
					              this.handleCreationFailure(requestingPlayer, newCurrencyEntity);
				              }
			              },
			              pluginInstance.getExecutor()
		              )
		              .exceptionally(databaseException -> {
			              this.handleDatabaseError(requestingPlayer, newCurrencyEntity, databaseException);
			              return null;
		              });
	}
	
	/**
	 * Handles successful currency creation including cache retrieval and account initialization.
 *
 * <p>This method manages the post-creation workflow including retrieving the saved
	 * currency from cache, initializing player accounts, and providing user feedback.
	 *
	 * @param newCurrencyEntity the currency that was created, must not be null
	 * @param pluginInstance the main plugin instance, must not be null
	 * @param requestingPlayer the player who requested creation, must not be null
	 * @param viewContext the current view context, must not be null
	 */
	private void handleSuccessfulCreation(
		final @NotNull Currency newCurrencyEntity,
		final @NotNull JExEconomy pluginInstance,
		final @NotNull Player requestingPlayer,
		final @NotNull Context viewContext
	) {
		final Currency savedCurrency = this.findCurrencyInCache(
			newCurrencyEntity.getIdentifier(),
			pluginInstance
		);
		
		if (
			savedCurrency != null
		) {
			this.createPlayerAccountsForCurrency(
				savedCurrency,
				pluginInstance,
				requestingPlayer
			);
			this.resetCurrencyState(viewContext);
			this.sendCreationSuccessMessage(requestingPlayer, savedCurrency);
		} else {
			this.handleCurrencyNotFoundInCache(requestingPlayer, newCurrencyEntity);
		}
	}
	
	/**
	 * Finds a currency in the plugin's cache by identifier.
 *
 * <p>This method searches the in-memory currency cache for a currency with
	 * the specified identifier and returns it if found.
	 *
	 * @param currencyIdentifier the identifier to search for, must not be null
	 * @param pluginInstance the main plugin instance, must not be null
	 * @return the found currency, or null if not found
	 */
	private @Nullable Currency findCurrencyInCache(
		final @NotNull String currencyIdentifier,
		final @NotNull JExEconomy pluginInstance
	) {
		return pluginInstance.getCurrencies()
		                     .values()
		                     .stream()
		                     .filter(cachedCurrency -> cachedCurrency.getIdentifier().equals(currencyIdentifier))
		                     .findFirst()
		                     .orElse(null);
	}
	
	/**
	 * Handles the scenario where a created currency is not found in the cache.
 *
 * <p>This method sends an error message when the currency was successfully created
	 * in the database but cannot be found in the in-memory cache.
	 *
	 * @param requestingPlayer the player who requested creation, must not be null
	 * @param newCurrencyEntity the currency that was created but not found in cache, must not be null
	 */
	private void handleCurrencyNotFoundInCache(
		final @NotNull Player requestingPlayer,
		final @NotNull Currency newCurrencyEntity
	) {
		this.i18n(
			    "create.currency_not_found_in_cache",
			    requestingPlayer
		    )
		    .includePrefix()
		    .withPlaceholder(
			    "identifier",
			    newCurrencyEntity.getIdentifier()
		    )
		    .build().sendMessage();
	}
	
	/**
	 * Handles currency creation failure scenarios.
 *
 * <p>This method sends an appropriate error message when the currency creation
	 * operation fails at the database level.
	 *
	 * @param requestingPlayer the player who requested creation, must not be null
	 * @param newCurrencyEntity the currency that failed to create, must not be null
	 */
	private void handleCreationFailure(
		final @NotNull Player requestingPlayer,
		final @NotNull Currency newCurrencyEntity
	) {
		this.i18n(
			    "create.failed",
			    requestingPlayer
		    )
		    .includePrefix()
		    .withPlaceholder(
			    "identifier",
			    newCurrencyEntity.getIdentifier()
		    )
		    .build().sendMessage();
	}
	
	/**
	 * Handles database errors during currency creation.
 *
 * <p>This method sends detailed error information when the currency creation
	 * process encounters a database-related exception.
	 *
	 * @param requestingPlayer the player who requested creation, must not be null
	 * @param newCurrencyEntity the currency that failed to create, must not be null
	 * @param databaseException the database exception that occurred, must not be null
	 */
	private void handleDatabaseError(
		final @NotNull Player requestingPlayer,
		final @NotNull Currency newCurrencyEntity,
		final @NotNull Throwable databaseException
	) {
		this.i18n(
			    "create.database_error",
			    requestingPlayer
		    )
		    .includePrefix()
		    .withPlaceholders(Map.of(
			    "identifier",
			    newCurrencyEntity.getIdentifier(),
			    "error",
			    databaseException.getMessage()
		    ))
		    .build().sendMessage();
	}
	
	/**
	 * Sends a success message after successful currency creation.
 *
 * <p>This method informs the requesting player that the currency has been
	 * successfully created and is now available in the system.
	 *
	 * @param requestingPlayer the player who requested creation, must not be null
	 * @param savedCurrency the currency that was successfully created, must not be null
	 */
	private void sendCreationSuccessMessage(
		final @NotNull Player requestingPlayer,
		final @NotNull Currency savedCurrency
	) {
		this.i18n(
			    "create.successfully_created",
			    requestingPlayer
		    )
		    .includePrefix()
		    .withPlaceholder(
			    "identifier",
			    savedCurrency.getIdentifier()
		    )
		    .build().sendMessage();
	}
	
	/**
	 * Creates player accounts for the new currency for all existing users.
 *
 * <p>This method initializes zero-balance accounts for all existing players
	 * when a new currency is created, ensuring that all players have access
	 * to the new currency system.
	 *
	 * <p><strong>Account Creation Process:</strong>
	 * <ol>
	 *   <li>Retrieve all existing users from the database</li>
	 *   <li>Create UserCurrency entities with zero balance for each user</li>
	 *   <li>Persist the new accounts to the database</li>
	 *   <li>Send confirmation message with account statistics</li>
	 * </ol>
	 *
	 * @param savedCurrency the currency to create accounts for, must not be null
	 * @param pluginInstance the main plugin instance, must not be null
	 * @param requestingPlayer the player who requested currency creation, must not be null
	 */
	private void createPlayerAccountsForCurrency(
		final @NotNull Currency savedCurrency,
		final @NotNull JExEconomy pluginInstance,
		final @NotNull Player requestingPlayer
	) {
		pluginInstance.getUserRepository()
		              .findAllAsync(0, 128)
		              .thenAcceptAsync(
			              retrievedUsers -> {
				              if (
					              retrievedUsers != null &&
					              !retrievedUsers.isEmpty()
				              ) {
					              this.processUserAccountCreation(
						              retrievedUsers,
						              savedCurrency,
						              pluginInstance,
						              requestingPlayer
					              );
				              }
			              },
			              pluginInstance.getExecutor()
		              )
		              .exceptionally(accountCreationException -> {
			              this.handleAccountCreationError(
				              requestingPlayer,
				              savedCurrency,
				              accountCreationException
			              );
			              return null;
		              });
	}
	
	/**
	 * Processes the creation of user accounts for the new currency.
 *
 * <p>This method creates UserCurrency entities for each existing user and
	 * persists them to the database, then sends a confirmation message.
	 *
	 * @param retrievedUsers the list of users to create accounts for, must not be null
	 * @param savedCurrency the currency to create accounts for, must not be null
	 * @param pluginInstance the main plugin instance, must not be null
	 * @param requestingPlayer the player who requested currency creation, must not be null
	 */
	private void processUserAccountCreation(
		final @NotNull List<User> retrievedUsers,
		final @NotNull Currency savedCurrency,
		final @NotNull JExEconomy pluginInstance,
		final @NotNull Player requestingPlayer
	) {
		retrievedUsers.forEach(existingUser -> {
			final UserCurrency newUserCurrency = new UserCurrency(
				existingUser,
				savedCurrency
			);
			pluginInstance.getUserCurrencyRepository().create(newUserCurrency);
		});
		
		this.sendAccountCreationSuccessMessage(
			requestingPlayer,
			savedCurrency,
			retrievedUsers.size()
		);
	}
	
	/**
	 * Sends a success message after creating player accounts.
 *
 * <p>This method informs the requesting player about the successful creation
	 * of player accounts for the new currency.
	 *
	 * @param requestingPlayer the player who requested currency creation, must not be null
	 * @param savedCurrency the currency for which accounts were created, must not be null
	 * @param accountCount the number of accounts created
	 */
	private void sendAccountCreationSuccessMessage(
		final @NotNull Player requestingPlayer,
		final @NotNull Currency savedCurrency,
		final int accountCount
	) {
		this.i18n(
			    "create.success.player_accounts_created",
			    requestingPlayer
		    )
		    .includePrefix()
		    .withPlaceholders(Map.of(
			    "player_amount",
			    accountCount,
			    "identifier",
			    savedCurrency.getIdentifier()
		    ))
		    .build().sendMessage();
	}
	
	/**
	 * Handles errors during player account creation.
 *
 * <p>This method sends an error message when the player account creation
	 * process encounters an exception.
	 *
	 * @param requestingPlayer the player who requested currency creation, must not be null
	 * @param savedCurrency the currency for which account creation failed, must not be null
	 * @param accountCreationException the exception that occurred, must not be null
	 */
	private void handleAccountCreationError(
		final @NotNull Player requestingPlayer,
		final @NotNull Currency savedCurrency,
		final @NotNull Throwable accountCreationException
	) {
		this.i18n(
			    "create.player_accounts_error",
			    requestingPlayer
		    )
		    .includePrefix()
		    .withPlaceholders(Map.of(
			    "identifier",
			    savedCurrency.getIdentifier(),
			    "error",
			    accountCreationException.getMessage()
		    ))
		    .build().sendMessage();
	}
	
	/**
	 * Resets the currency state to allow creating another currency.
 *
 * <p>This method clears the current currency configuration and updates the view
	 * to prepare for the creation of a new currency.
	 *
	 * @param viewContext the current view context, must not be null
	 */
	private void resetCurrencyState(
		final @NotNull Context viewContext
	) {
		this.targetCurrency.set(
			new Currency(""),
			viewContext
		);
		viewContext.update();
	}
}
