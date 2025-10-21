package de.jexcellence.economy.currency;

import com.raindropcentral.rplatform.misc.heads.view.Proceed;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.common.BaseView;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.view.currency.CurrencyEditingView;
import de.jexcellence.economy.view.currency.anvil.CurrencyIconAnvilView;
import de.jexcellence.economy.view.currency.anvil.CurrencyPrefixAnvilView;
import de.jexcellence.economy.view.currency.anvil.CurrencySuffixAnvilView;
import de.jexcellence.economy.view.currency.anvil.CurrencySymbolAnvilView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Interactive view for editing existing currency properties.
 * <p>
 * This view provides a comprehensive interface for modifying all editable aspects
 * of an existing currency including symbol, icon, prefix, and suffix. The identifier
 * cannot be changed to maintain database integrity and prevent conflicts.
 * </p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Property Editing:</strong> Click-to-edit interface for currency properties</li>
 *   <li><strong>Real-time Updates:</strong> Changes are reflected immediately in the interface</li>
 *   <li><strong>Database Persistence:</strong> All changes are saved to the database</li>
 *   <li><strong>Validation:</strong> Input validation through specialized anvil views</li>
 *   <li><strong>State Management:</strong> Maintains currency state across view transitions</li>
 * </ul>
 *
 * <h3>Editable Properties:</h3>
 * <ul>
 *   <li><strong>Symbol (s):</strong> Currency symbol and visual representation</li>
 *   <li><strong>Icon (i):</strong> Material icon for graphical interfaces</li>
 *   <li><strong>Prefix (p):</strong> Text displayed before currency amounts</li>
 *   <li><strong>Suffix (f):</strong> Text displayed after currency amounts</li>
 *   <li><strong>Save Button (v):</strong> Saves all changes to the database</li>
 * </ul>
 *
 * <h3>Non-Editable Properties:</h3>
 * <ul>
 *   <li><strong>Identifier:</strong> Cannot be changed to maintain database integrity</li>
 * </ul>
 *
 * @author JExcellence
 * @see BaseView
 * @see Currency
 * @see CurrencyEditingView
 */
public class CurrencyPropertiesEditingView extends BaseView {
	
	/**
	 * State holder for the main JExEconomyImpl plugin instance.
	 */
	private final State<JExEconomyImpl> jexEconomy = initialState("plugin");
	
	/**
	 * Mutable state holder for the currency being edited.
	 */
	private final MutableState<Currency> targetCurrency = mutableState(null);
	
	/**
	 * Constructs a new {@code CurrencyPropertiesEditingView} with the currency editing view as parent.
	 * <p>
	 * The view will provide currency property editing functionality and navigate back to the
	 * currency editing selection view when closed or cancelled.
	 * </p>
	 */
	public CurrencyPropertiesEditingView() {
		super(CurrencyEditingView.class);
	}
	
	/**
	 * Returns the internationalization key for this view.
	 * <p>
	 * This key is used to load localized strings for the currency properties editing
	 * interface, including titles, labels, and formatting templates.
	 * </p>
	 *
	 * @return the i18n key for the currency properties editing UI
	 */
	@Override
	protected @NotNull String getKey() {
		return "currency_properties_editing_ui";
	}
	
	/**
	 * Defines the layout structure for the currency properties editing view.
	 * <p>
	 * The layout organizes currency property editing elements in a user-friendly
	 * arrangement with the save button positioned for easy access.
	 * </p>
	 *
	 * <h3>Layout Mapping:</h3>
	 * <ul>
	 *   <li><strong>n:</strong> Currency identifier display (read-only)</li>
	 *   <li><strong>s:</strong> Currency symbol editing</li>
	 *   <li><strong>i:</strong> Currency icon editing</li>
	 *   <li><strong>p:</strong> Currency prefix editing</li>
	 *   <li><strong>f:</strong> Currency suffix editing</li>
	 *   <li><strong>v:</strong> Save changes button</li>
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
			"        v"
		};
	}
	
	/**
	 * Handles view resumption when returning from anvil input views.
	 * <p>
	 * This method is called when the user returns from an anvil input view after
	 * editing a currency property. It updates the currency state with any
	 * changes made in the anvil view and refreshes the interface.
	 * </p>
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
	 * Renders all currency property editing elements and the save button.
	 * <p>
	 * This method populates the view with interactive elements for editing
	 * each modifiable aspect of the currency. The identifier is displayed
	 * as read-only to maintain database integrity.
	 * </p>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 */
	@Override
	public void onFirstRender(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer
	) {
		this.targetCurrency.set(
			(Currency) ((Map<String, Object>) renderContext.getInitialData()).get("currency"),
			renderContext
		);
		
		this.renderIdentifierDisplay(renderContext, contextPlayer);
		this.renderSymbolEditing(renderContext, contextPlayer);
		this.renderIconEditing(renderContext, contextPlayer);
		this.renderPrefixEditing(renderContext, contextPlayer);
		this.renderSuffixEditing(renderContext, contextPlayer);
		this.renderSaveButton(renderContext, contextPlayer);
	}
	
	/**
	 * Renders the currency identifier display (read-only).
	 * <p>
	 * This element shows the currency identifier but does not allow editing
	 * to maintain database integrity and prevent conflicts.
	 * </p>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 */
	private void renderIdentifierDisplay(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer
	) {
		final Currency currentCurrency = this.targetCurrency.get(renderContext);
		
		renderContext
			.layoutSlot(
				'n',
				UnifiedBuilderFactory.item(Material.NAME_TAG)
				                     .setName(
					                     this.i18n(
						                     "identifier.name",
						                     contextPlayer
					                     ).withPlaceholder(
						                     "currency_identifier",
						                     currentCurrency.getIdentifier()
					                     ).build().component()
				                     )
				                     .setLore(
					                     this.i18n(
						                     "identifier.lore",
						                     contextPlayer
					                     ).withPlaceholder(
						                     "currency_identifier",
						                     currentCurrency.getIdentifier()
					                     ).build().children()
				                     )
				                     .build()
			);
	}
	
	/**
	 * Renders the currency symbol editing button.
	 * <p>
	 * This button opens the {@link CurrencySymbolAnvilView} for editing
	 * the currency's visual symbol with validation.
	 * </p>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 */
	private void renderSymbolEditing(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer
	) {
		final Currency currentCurrency = this.targetCurrency.get(renderContext);
		
		renderContext
			.layoutSlot(
				's',
				UnifiedBuilderFactory.item(Material.GOLD_NUGGET)
				                     .setName(
					                     this.i18n(
						                     "symbol.name",
						                     contextPlayer
					                     ).withPlaceholder(
						                     "currency_symbol",
						                     currentCurrency.getSymbol()
					                     ).build().component()
				                     )
				                     .setLore(
					                     this.i18n(
						                     "symbol.lore",
						                     contextPlayer
					                     ).withPlaceholder(
						                     "currency_symbol",
						                     currentCurrency.getSymbol()
					                     ).build().children()
				                     )
				                     .build()
			)
			.updateOnStateChange(this.targetCurrency)
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
	 * Renders the currency icon editing button.
	 * <p>
	 * This button opens the {@link CurrencyIconAnvilView} for editing
	 * the currency's material icon with validation.
	 * </p>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 */
	private void renderIconEditing(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer
	) {
		final Currency currentCurrency = this.targetCurrency.get(renderContext);
		
		renderContext
			.layoutSlot(
				'i',
				UnifiedBuilderFactory.item(currentCurrency.getIcon())
				                     .setName(
					                     this.i18n(
						                     "icon.name",
						                     contextPlayer
					                     ).withPlaceholder(
						                     "material_name",
						                     currentCurrency.getIcon().name()
					                     ).build().component()
				                     )
				                     .setLore(
					                     this.i18n(
						                     "icon.lore",
						                     contextPlayer
					                     ).withPlaceholder(
						                     "material_name",
						                     currentCurrency.getIcon().name()
					                     ).build().children()
				                     )
				                     .build()
			)
			.updateOnStateChange(this.targetCurrency)
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
	 * Renders the currency prefix editing button.
	 * <p>
	 * This button opens the {@link CurrencyPrefixAnvilView} for editing
	 * the currency's prefix text with validation.
	 * </p>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 */
	private void renderPrefixEditing(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer
	) {
		final Currency currentCurrency = this.targetCurrency.get(renderContext);
		final String currencyPrefix = currentCurrency.getPrefix();
		final boolean hasPrefixConfigured = ! currencyPrefix.isEmpty();
		
		renderContext
			.layoutSlot(
				'p',
				UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
				                     .setName(
					                     this.i18n(
						                     "prefix.name",
						                     contextPlayer
					                     ).withPlaceholder(
						                     "currency_prefix",
						                     hasPrefixConfigured ? currencyPrefix : "None"
					                     ).build().component()
				                     )
				                     .setLore(
					                     this.i18n(
						                     "prefix.lore",
						                     contextPlayer
					                     ).withPlaceholders(Map.of(
						                     "currency_prefix",
						                     hasPrefixConfigured ? currencyPrefix : "None",
						                     "has_prefix",
						                     hasPrefixConfigured ? "true" : "false"
					                     )).build().children()
				                     )
				                     .build()
			)
			.updateOnStateChange(this.targetCurrency)
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
	 * Renders the currency suffix editing button.
	 * <p>
	 * This button opens the {@link CurrencySuffixAnvilView} for editing
	 * the currency's suffix text with validation.
	 * </p>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 */
	private void renderSuffixEditing(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer
	) {
		final Currency currentCurrency = this.targetCurrency.get(renderContext);
		final String currencySuffix = currentCurrency.getSuffix();
		final boolean hasSuffixConfigured = ! currencySuffix.isEmpty();
		
		renderContext
			.layoutSlot(
				'f',
				UnifiedBuilderFactory.item(Material.PAPER)
				                     .setName(
					                     this.i18n(
						                     "suffix.name",
						                     contextPlayer
					                     ).withPlaceholder(
						                     "currency_suffix",
						                     hasSuffixConfigured ? currencySuffix : "None"
					                     ).build().component()
				                     )
				                     .setLore(
					                     this.i18n(
						                     "suffix.lore",
						                     contextPlayer
					                     ).withPlaceholders(Map.of(
						                     "currency_suffix",
						                     hasSuffixConfigured ? currencySuffix : "None",
						                     "has_suffix",
						                     hasSuffixConfigured ? "true" : "false"
					                     )).build().children()
				                     )
				                     .build()
			)
			.updateOnStateChange(this.targetCurrency)
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
	 * Renders the save changes button.
	 * <p>
	 * This button saves all changes made to the currency properties to the database
	 * and updates the in-memory cache.
	 * </p>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 */
	private void renderSaveButton(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer
	) {
		renderContext
			.layoutSlot(
				'v',
				UnifiedBuilderFactory.item(new Proceed().getHead(contextPlayer))
				                     .setName(
					                     this.i18n(
						                     "save_changes.name",
						                     contextPlayer
					                     ).build().component()
				                     )
				                     .setLore(
					                     this.i18n(
						                     "save_changes.lore",
						                     contextPlayer
					                     ).build().children()
				                     )
				                     .build()
			)
			.onClick(clickContext -> {
				this.handleSaveChanges(
					clickContext,
					contextPlayer
				);
			});
	}
	
	/**
	 * Handles saving currency changes to the database.
	 * <p>
	 * This method saves the modified currency properties to the database,
	 * updates the in-memory cache, and provides user feedback.
	 * </p>
	 *
	 * @param clickContext the context from the save button click, must not be null
	 * @param requestingPlayer the player requesting the save operation, must not be null
	 */
	private void handleSaveChanges(
		final @NotNull Context clickContext,
		final @NotNull Player requestingPlayer
	) {
		final Currency       currencyToSave = this.targetCurrency.get(clickContext);
		final JExEconomyImpl jexEconomyImpl = this.jexEconomy.get(clickContext);
		
		this.i18n(
			    "save.processing",
			    requestingPlayer
		    )
		    .includePrefix()
		    .withPlaceholder(
			    "currency_identifier",
			    currencyToSave.getIdentifier()
		    )
		    .sendMessage();
		
		jexEconomyImpl.getCurrencyRepository()
		              .updateAsync(currencyToSave)
		              .thenAcceptAsync(
			              updateSuccessful -> {
				              if (updateSuccessful != null) {
					              jexEconomyImpl.getCurrencies().put(
						              currencyToSave.getId(),
						              currencyToSave
					              );
								  
					              this.i18n(
						                  "save.success",
						                  requestingPlayer
					                  )
					                  .includePrefix()
					                  .withPlaceholder(
						                  "currency_identifier",
						                  currencyToSave.getIdentifier()
					                  )
					                  .sendMessage();
				              } else {
					              this.i18n(
						                  "save.failed",
						                  requestingPlayer
					                  )
					                  .includePrefix()
					                  .withPlaceholder(
						                  "currency_identifier",
						                  currencyToSave.getIdentifier()
					                  )
					                  .sendMessage();
				              }
			              },
			              jexEconomyImpl.getExecutor()
		              )
		              .exceptionally(saveException -> {
			              this.i18n(
				                  "save.error",
				                  requestingPlayer
			                  )
			                  .includePrefix()
			                  .withPlaceholders(Map.of(
				                  "currency_identifier",
				                  currencyToSave.getIdentifier(),
				                  "error",
				                  saveException.getMessage()
			                  ))
			                  .sendMessage();
			              return null;
		              });
		
		clickContext.closeForPlayer();
	}
}