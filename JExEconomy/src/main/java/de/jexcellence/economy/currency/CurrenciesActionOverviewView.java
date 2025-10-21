package de.jexcellence.economy.currency;

import com.raindropcentral.rplatform.utility.heads.view.Proceed;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.economy.JExEconomyImpl;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Action overview view for currency management operations.
 * <p>
 * This view provides the main menu for currency-related actions including
 * creating, editing, viewing, and deleting currencies. Each action is
 * represented by an appropriate material and leads to the corresponding
 * functionality within the currency management system.
 * </p>
 *
 * <h3>Available Actions:</h3>
 * <ul>
 *   <li><strong>Create Currency (c):</strong> Opens the currency creation interface</li>
 *   <li><strong>Edit Currency (e):</strong> Provides access to currency editing functionality</li>
 *   <li><strong>View Currencies (v):</strong> Displays the currencies overview with pagination</li>
 *   <li><strong>Delete Currency (d):</strong> Administrative function for currency deletion (permission-restricted)</li>
 * </ul>
 *
 * <h3>Permission Requirements:</h3>
 * <p>
 * Most actions are available to all users, but the delete functionality requires one of:
 * </p>
 * <ul>
 *   <li><code>jexeconomy.admin.delete</code> - Specific delete permission</li>
 *   <li><code>jexeconomy.admin.*</code> - All administrative permissions</li>
 *   <li>Server operator status</li>
 * </ul>
 *
 * <h3>Layout Structure:</h3>
 * <p>
 * The view uses a simple horizontal layout with four action buttons centered
 * in the middle row, providing easy access to all currency management functions.
 * </p>
 *
 * @author JExcellence
 * @see BaseView
 * @see CurrenciesCreatingView
 * @see CurrenciesOverviewView
 * @see CurrencyEditingView
 * @see CurrencyDeletionView
 */
public class CurrenciesActionOverviewView extends BaseView {
	
	/**
	 * State holder for the main JExEconomyImpl plugin instance.
	 */
	private final State<JExEconomyImpl> jexEconomy = initialState("plugin");
	
	/**
	 * Returns the internationalization key for this view.
	 * <p>
	 * This key is used to load localized strings for the action overview interface,
	 * including titles, labels, and action descriptions.
	 * </p>
	 *
	 * @return the i18n key for the currencies action overview UI
	 */
	@Override
	protected @NotNull String getKey() {
		return "currencies_action_overview_ui";
	}
	
	/**
	 * Defines the layout structure for the action overview view.
	 * <p>
	 * The layout provides a clean, centered arrangement of action buttons
	 * with visual spacing for optimal user experience.
	 * </p>
	 *
	 * <h3>Layout Mapping:</h3>
	 * <ul>
	 *   <li><strong>c:</strong> Create currency action button</li>
	 *   <li><strong>e:</strong> Edit currency action button</li>
	 *   <li><strong>v:</strong> View currencies action button</li>
	 *   <li><strong>d:</strong> Delete currency action button (permission-restricted)</li>
	 * </ul>
	 *
	 * @return the layout pattern as a string array, never null
	 */
	@Override
	protected @NotNull String[] getLayout() {
		return new String[]{
			"         ",
			" c e v d ",
			"         "
		};
	}
	
	/**
	 * Renders all action buttons and their associated functionality.
	 * <p>
	 * This method populates the view with interactive action buttons for
	 * currency management operations. Each button is configured with appropriate
	 * materials, localized text, and click handlers.
	 * </p>
	 *
	 * <h3>Rendered Actions:</h3>
	 * <ul>
	 *   <li>Create currency button with proceed head icon</li>
	 *   <li>Edit currency button with anvil material</li>
	 *   <li>View currencies button with spyglass material</li>
	 *   <li>Delete currency button with barrier material (permission-based)</li>
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
		this.renderCreateCurrencyAction(renderContext, contextPlayer);
		this.renderEditCurrencyAction(renderContext, contextPlayer);
		this.renderViewCurrenciesAction(renderContext, contextPlayer);
		this.renderDeleteCurrencyAction(renderContext, contextPlayer);
	}
	
	/**
	 * Renders the create currency action button.
	 * <p>
	 * This button opens the {@link CurrenciesCreatingView} where users can
	 * configure and create new currencies in the system.
	 * </p>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 */
	private void renderCreateCurrencyAction(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer
	) {
		renderContext.layoutSlot(
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
				                     ).build().splitLines()
			                     )
			                     .build()
		).onClick(clickContext -> {
			clickContext.openForPlayer(
				CurrenciesCreatingView.class,
				Map.of(
					"plugin",
					this.jexEconomy.get(clickContext)
				)
			);
		});
	}
	
	/**
	 * Renders the edit currency action button.
	 * <p>
	 * This button opens the {@link CurrencyEditingView} where users can
	 * select and modify existing currencies in the system.
	 * </p>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 */
	private void renderEditCurrencyAction(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer
	) {
		renderContext.layoutSlot(
			'e',
			UnifiedBuilderFactory.item(Material.ANVIL)
			                     .setName(
				                     this.i18n(
					                     "edit_currency.name",
					                     contextPlayer
				                     ).build().component()
			                     )
			                     .setLore(
				                     this.i18n(
					                     "edit_currency.lore",
					                     contextPlayer
				                     ).build().splitLines()
			                     )
			                     .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			                     .build()
		).onClick(clickContext -> {
			clickContext.openForPlayer(
				CurrencyEditingView.class,
				Map.of(
					"plugin",
					this.jexEconomy.get(clickContext)
				)
			);
		});
	}
	
	/**
	 * Renders the view currencies action button.
	 * <p>
	 * This button opens the {@link CurrenciesOverviewView} where users can
	 * browse all existing currencies with pagination support.
	 * </p>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 */
	private void renderViewCurrenciesAction(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer
	) {
		renderContext.layoutSlot(
			'v',
			UnifiedBuilderFactory.item(Material.SPYGLASS)
			                     .setName(
				                     this.i18n(
					                     "view_currencies.name",
					                     contextPlayer
				                     ).build().component()
			                     )
			                     .setLore(
				                     this.i18n(
					                     "view_currencies.lore",
					                     contextPlayer
				                     ).build().splitLines()
			                     )
			                     .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			                     .build()
		).onClick(clickContext -> {
			clickContext.openForPlayer(
				CurrenciesOverviewView.class,
				Map.of(
					"plugin",
					this.jexEconomy.get(clickContext)
				)
			);
		});
	}
	
	/**
	 * Renders the delete currency action button with permission restrictions.
	 * <p>
	 * This button is only visible to users with appropriate administrative permissions.
	 * It opens the {@link CurrencyDeletionView} where administrators can select
	 * currencies to delete with comprehensive safety measures and impact assessment.
	 * </p>
	 *
	 * <h3>Permission Requirements:</h3>
	 * <p>
	 * The button is displayed only if the player has one of:
	 * </p>
	 * <ul>
	 *   <li><code>jexeconomy.admin.delete</code> - Specific delete permission</li>
	 *   <li><code>jexeconomy.admin.*</code> - All administrative permissions</li>
	 *   <li>Server operator status</li>
	 * </ul>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 */
	private void renderDeleteCurrencyAction(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer
	) {
		renderContext.layoutSlot(
			'd',
			UnifiedBuilderFactory.item(Material.BARRIER)
			                     .setName(
				                     this.i18n(
					                     "delete_currency.name",
					                     contextPlayer
				                     ).build().component()
			                     )
			                     .setLore(
				                     this.i18n(
					                     "delete_currency.lore",
					                     contextPlayer
				                     ).build().splitLines()
			                     )
			                     .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			                     .build()
		).displayIf(displayContext -> {
			final Player viewingPlayer = displayContext.getPlayer();
			return this.hasDeletePermission(viewingPlayer);
		}).onClick(clickContext -> {
			clickContext.openForPlayer(
				CurrencyDeletionView.class,
				Map.of(
					"plugin",
					this.jexEconomy.get(clickContext)
				)
			);
		});
	}
	
	/**
	 * Checks if a player has permission to access the delete currency functionality.
	 * <p>
	 * This method verifies that the player has one of the required permissions
	 * or is a server operator, allowing them to access administrative delete functions.
	 * </p>
	 *
	 * @param targetPlayer the player to check permissions for, must not be null
	 * @return true if the player has delete permissions, false otherwise
	 */
	private boolean hasDeletePermission(
		final @NotNull Player targetPlayer
	) {
		return targetPlayer.hasPermission("jexeconomy.admin.delete") ||
		       targetPlayer.hasPermission("jexeconomy.admin.*") ||
		       targetPlayer.isOp();
	}
}