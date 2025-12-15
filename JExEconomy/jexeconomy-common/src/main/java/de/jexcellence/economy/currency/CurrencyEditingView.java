package de.jexcellence.economy.currency;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import de.jexcellence.economy.JExEconomy;
import de.jexcellence.economy.database.entity.Currency;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated view for selecting a currency to edit.
 * <p>
 * This view displays all available currencies in a paginated format, allowing
 * administrators to select which currency they want to edit. Each currency entry
 * shows basic information and can be clicked to open the currency editing interface.
 * </p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Currency Selection:</strong> Click-to-select interface for currency editing</li>
 *   <li><strong>Paginated Display:</strong> Efficiently handles large numbers of currencies</li>
 *   <li><strong>Currency Information:</strong> Shows identifier, symbol, and icon</li>
 *   <li><strong>Visual Representation:</strong> Uses currency icons for immediate recognition</li>
 * </ul>
 *
 * <h3>Navigation Flow:</h3>
 * <p>
 * Users navigate from the currencies action overview to this view to select a currency
 * to edit, then proceed to the currency properties editing interface.
 * </p>
 *
 * @author JExcellence
 * @see APaginatedView
 * @see Currency
 * @see CurrencyPropertiesEditingView
 * @see CurrenciesActionOverviewView
 */
public class CurrencyEditingView extends APaginatedView<Currency> {
	
	/**
	 * State holder for the main JExEconomy plugin instance.
	 */
	private final State<JExEconomy> jexEconomy = initialState("plugin");
	
	/**
	 * Constructs a new {@code CurrencyEditingView} with the currencies action overview as parent.
	 * <p>
	 * The view will display all available currencies for selection and provide
	 * navigation back to the currencies action overview when closed.
	 * </p>
	 */
	public CurrencyEditingView() {
		super(CurrenciesActionOverviewView.class);
	}
	
	/**
	 * Returns the internationalization key for this view.
	 * <p>
	 * This key is used to load localized strings for the currency editing selection
	 * interface, including titles, labels, and formatting templates.
	 * </p>
	 *
	 * @return the i18n key for the currency editing selection UI
	 */
	@Override
	protected @NotNull String getKey() {
		return "currency_editing_ui";
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
	 * Renders a single currency entry in the pagination view.
	 * <p>
	 * This method creates a visual representation of a currency including its icon,
	 * name, symbol, and additional metadata. The entry is interactive and allows
	 * administrators to click through to the currency properties editing view.
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
		final Player contextPlayer = renderContext.getPlayer();
		
		itemBuilder
			.withItem(
				UnifiedBuilderFactory
					.item(displayedCurrency.getIcon())
					.setName(
						this.i18n(
							    "currency.name",
							    contextPlayer
						    )
						    .withPlaceholder(
							    "currency_identifier",
							    displayedCurrency.getIdentifier()
						    )
						    .withPlaceholder(
							    "currency_symbol",
							    displayedCurrency.getSymbol()
						    )
						    .build()
						    .component()
					)
					.setLore(
						this.i18n(
							    "currency.lore",
							    contextPlayer
						    )
						    .withPlaceholders(
							    Map.of(
								    "currency_identifier",
								    displayedCurrency.getIdentifier(),
								    "currency_symbol",
								    displayedCurrency.getSymbol(),
								    "currency_prefix",
								    displayedCurrency.getPrefix(),
								    "currency_suffix",
								    displayedCurrency.getSuffix(),
								    "index",
								    entryIndex + 1
							    )
						    )
						    .build()
						    .children()
					)
					.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
					.build()
			)
			.onClick(clickContext -> {
				clickContext.openForPlayer(
					CurrencyPropertiesEditingView.class,
					Map.of(
						"plugin",
						this.jexEconomy.get(clickContext),
						"currency",
						displayedCurrency,
						"initialData",
						clickContext.getInitialData()
					)
				);
			});
	}
	
	/**
	 * Defines the layout structure for the currency editing selection view.
	 * <p>
	 * The layout provides a clean, organized display with currency entries in the center
	 * and navigation controls below.
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
	 * Handles additional rendering logic specific to the currency editing selection view.
	 * <p>
	 * Currently, no additional rendering is required for the currency editing selection,
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
		// No additional rendering required for the currency editing selection
		// All necessary elements are handled by the pagination system
	}
}