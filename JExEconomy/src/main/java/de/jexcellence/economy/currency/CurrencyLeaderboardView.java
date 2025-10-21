package de.jexcellence.economy.currency;

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import de.jexcellence.economy.JExEconomyImpl;
import de.jexcellence.economy.database.entity.Currency;
import de.jexcellence.economy.database.entity.UserCurrency;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated view for displaying currency leaderboard rankings.
 * <p>
 * This view shows the top players with the highest balances for a specific currency,
 * sorted in descending order by balance amount. It provides detailed information about
 * each player's balance, their ranking position, and visual indicators for top performers.
 * </p>
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Real-time Rankings:</strong> Displays current player balances sorted by amount</li>
 *   <li><strong>Visual Hierarchy:</strong> Special materials and colors for top 3 positions</li>
 *   <li><strong>Pagination Support:</strong> Handles large numbers of players efficiently</li>
 *   <li><strong>Detailed Information:</strong> Shows player names, UUIDs, balances, and ranks</li>
 *   <li><strong>Currency Integration:</strong> Displays currency-specific formatting and symbols</li>
 * </ul>
 *
 * <h3>Ranking System:</h3>
 * <ul>
 *   <li><strong>1st Place:</strong> Diamond block with gold gradient color</li>
 *   <li><strong>2nd Place:</strong> Gold block with silver gradient color</li>
 *   <li><strong>3rd Place:</strong> Iron block with bronze gradient color</li>
 *   <li><strong>Other Ranks:</strong> Player head with white color</li>
 * </ul>
 *
 * <h3>Layout Structure:</h3>
 * <p>
 * The view uses a custom layout with currency information at the top and paginated
 * player entries in the center. Navigation controls are provided at the bottom
 * for moving between pages of results.
 * </p>
 *
 * @author JExcellence
 * @see APaginatedView
 * @see UserCurrency
 * @see Currency
 */
public class CurrencyLeaderboardView extends APaginatedView<UserCurrency> {
	
	/**
	 * Decimal formatter for displaying currency amounts with thousands separators.
	 */
	private static final DecimalFormat BALANCE_DECIMAL_FORMAT = new DecimalFormat("#,###.##");
	
	/**
	 * State holder for the main JExEconomyImpl plugin instance.
	 */
	private final State<JExEconomyImpl> jexEconomy = initialState("plugin");
	
	/**
	 * State holder for the currency being displayed in the leaderboard.
	 */
	private final State<Currency> targetCurrency = initialState("currency");
	
	/**
	 * Constructs a new {@code CurrencyLeaderboardView} with the currency detail view as parent.
	 * <p>
	 * The view will display leaderboard rankings for a specific currency and provide
	 * navigation back to the currency detail view when closed.
	 * </p>
	 */
	public CurrencyLeaderboardView() {
		super(CurrencyDetailView.class);
	}
	
	/**
	 * Returns the internationalization key for this view.
	 * <p>
	 * This key is used to load localized strings for the leaderboard interface,
	 * including titles, labels, and formatting templates.
	 * </p>
	 *
	 * @return the i18n key for the currency leaderboard UI
	 */
	@Override
	protected @NotNull String getKey() {
		return "currency_leaderboard_ui";
	}
	
	/**
	 * Provides the asynchronous data source for the leaderboard pagination.
	 * <p>
	 * This method retrieves the top players for the specified currency, filters out
	 * players with zero balances, and sorts them in descending order by balance amount.
	 * The results are limited to the top 25 players to optimize performance.
	 * </p>
	 *
	 * <h3>Data Processing:</h3>
	 * <ol>
	 *   <li>Fetch top 25 players for the currency from the repository</li>
	 *   <li>Filter out players with zero or negative balances</li>
	 *   <li>Sort remaining players by balance in descending order</li>
	 *   <li>Return the processed list for pagination</li>
	 * </ol>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @return a future containing the sorted list of user-currency associations
	 */
	@Override
	protected @NotNull CompletableFuture<List<UserCurrency>> getAsyncPaginationSource(
		final @NotNull Context renderContext
	) {
		final Currency       selectedCurrency = this.targetCurrency.get(renderContext);
		final JExEconomyImpl pluginInstance   = this.jexEconomy.get(renderContext);
		
		return pluginInstance.getUserCurrencyRepository()
		                     .findTopByCurrency(
			                     selectedCurrency,
			                     25
		                     )
		                     .thenApply(retrievedUserCurrencies ->
			                                retrievedUserCurrencies
				                                .stream()
				                                .filter(userCurrencyAssociation -> userCurrencyAssociation.getBalance() > 0)
				                                .sorted(Comparator.comparingDouble(UserCurrency::getBalance).reversed())
				                                .toList()
		                     );
	}
	
	/**
	 * Renders a single leaderboard entry for a player's currency balance.
	 * <p>
	 * This method creates a visual representation of a player's ranking, including
	 * their position, name, balance, and appropriate visual styling based on their rank.
	 * Top 3 players receive special positioning and materials for visual emphasis.
	 * </p>
	 *
	 * <h3>Rendering Features:</h3>
	 * <ul>
	 *   <li>Rank-based material selection (diamond, gold, iron, or player head)</li>
	 *   <li>Color-coded rank indicators with gradient effects</li>
	 *   <li>Formatted balance display with currency symbols</li>
	 *   <li>Player information including name and UUID</li>
	 *   <li>Special slot positioning for top 3 players</li>
	 * </ul>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param itemBuilder the item component builder for creating the display item, must not be null
	 * @param entryIndex the zero-based index of this entry in the pagination
	 * @param userCurrencyEntry the user-currency association to render, must not be null
	 */
	@Override
	protected void renderEntry(
		final @NotNull Context renderContext,
		final @NotNull BukkitItemComponentBuilder itemBuilder,
		final int entryIndex,
		final @NotNull UserCurrency userCurrencyEntry
	) {
		final Player contextPlayer = renderContext.getPlayer();
		final Currency displayedCurrency = this.targetCurrency.get(renderContext);
		final int playerRank = entryIndex + 1;
		
		final Material rankMaterial = determineRankMaterial(playerRank);
		final String rankColorCode = determineRankColor(playerRank);
		
		if (
			playerRank < 4
		) {
			itemBuilder.withSlot(
				playerRank == 1 ?
				13 :
				playerRank == 2 ?
				21 :
				23
			);
		}
		
		itemBuilder
			.withItem(
				UnifiedBuilderFactory
					.item(rankMaterial)
					.setName(
						this.i18n(
							    "player_entry.name",
							    contextPlayer
						    )
						    .withAll(Map.of(
							    "rank",
							    playerRank,
							    "rank_color",
							    rankColorCode,
							    "player_name",
							    userCurrencyEntry.getPlayer().getPlayerName(),
							    "currency_symbol",
							    displayedCurrency.getSymbol()
						    ))
						    .build()
						    .component()
					)
					.setLore(
						this.i18n(
							    "player_entry.lore",
							    contextPlayer
						    )
						    .withAll(Map.of(
							    "rank",
							    playerRank,
							    "rank_color",
							    rankColorCode,
							    "player_name",
							    userCurrencyEntry.getPlayer().getPlayerName(),
							    "player_uuid",
							    userCurrencyEntry.getPlayer().getUniqueId(),
							    "balance",
							    BALANCE_DECIMAL_FORMAT.format(userCurrencyEntry.getBalance()),
							    "currency_symbol",
							    displayedCurrency.getSymbol(),
							    "currency_identifier",
							    displayedCurrency.getIdentifier(),
							    "formatted_balance",
							    this.formatBalanceWithCurrency(
								    userCurrencyEntry.getBalance(),
								    displayedCurrency
							    )
						    ))
						    .build()
						    .splitLines()
					)
					.addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
					.build()
			);
	}
	
	/**
	 * Defines the layout structure for the leaderboard view.
	 * <p>
	 * The layout provides a centered area for displaying player entries with
	 * navigation controls at the bottom. The 'O' characters represent pagination
	 * slots where player entries will be displayed.
	 * </p>
	 *
	 * <h3>Layout Structure:</h3>
	 * <ul>
	 *   <li><strong>Rows 1-3:</strong> Empty space for visual separation</li>
	 *   <li><strong>Rows 4-5:</strong> Pagination content area (marked with 'O')</li>
	 *   <li><strong>Row 6:</strong> Navigation controls (previous, page indicator, next)</li>
	 * </ul>
	 *
	 * @return the layout pattern as a string array, never null
	 */
	@Override
	protected @NotNull String[] getLayout() {
		return new String[]{
			"         ",
			"         ",
			"         ",
			"  OOOOO  ",
			"  OOOOO  ",
			"   <p>   "
		};
	}
	
	/**
	 * Handles additional rendering logic specific to the leaderboard view.
	 * <p>
	 * This method renders the currency information display at the top of the interface,
	 * showing the currency icon, name, and symbol. This provides context for which
	 * currency's leaderboard is being displayed.
	 * </p>
	 *
	 * <h3>Currency Information Display:</h3>
	 * <ul>
	 *   <li>Currency icon as the display material</li>
	 *   <li>Currency identifier and symbol in the name</li>
	 *   <li>Additional currency details in the lore</li>
	 *   <li>Hidden item attributes for clean appearance</li>
	 * </ul>
	 *
	 * @param renderContext the current rendering context, must not be null
	 * @param contextPlayer the player viewing the interface, must not be null
	 */
	@Override
	protected void onPaginatedRender(
		final @NotNull RenderContext renderContext,
		final @NotNull Player contextPlayer
	) {
		final Currency displayedCurrency = this.targetCurrency.get(renderContext);
		
		renderContext.slot(
			1,
			5,
			UnifiedBuilderFactory.item(displayedCurrency.getIcon())
			                     .setName(
				                     this.i18n(
					                         "currency_info.name",
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
					                         "currency_info.lore",
					                         contextPlayer
				                         )
				                         .withAll(Map.of(
					                         "currency_identifier",
					                         displayedCurrency.getIdentifier(),
					                         "currency_symbol",
					                         displayedCurrency.getSymbol()
				                         ))
				                         .build()
				                         .splitLines()
			                     )
			                     .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
			                     .build()
		);
	}
	
	/**
	 * Determines the appropriate material for displaying a player's rank.
	 * <p>
	 * This method provides visual hierarchy by assigning different materials
	 * based on the player's ranking position. The top 3 positions receive
	 * special block materials, while other positions use player heads.
	 * </p>
	 *
	 * <h3>Material Mapping:</h3>
	 * <ul>
	 *   <li><strong>1st Place:</strong> Diamond Block (most prestigious)</li>
	 *   <li><strong>2nd Place:</strong> Gold Block (second tier)</li>
	 *   <li><strong>3rd Place:</strong> Iron Block (third tier)</li>
	 *   <li><strong>Other Ranks:</strong> Player Head (standard representation)</li>
	 * </ul>
	 *
	 * @param playerRank the player's ranking position (1-based), must be positive
	 * @return the material to use for displaying this rank, never null
	 */
	private @NotNull Material determineRankMaterial(
		final int playerRank
	) {
		return switch (playerRank) {
			case 1 -> Material.DIAMOND_BLOCK;
			case 2 -> Material.GOLD_BLOCK;
			case 3 -> Material.IRON_BLOCK;
			default -> Material.PLAYER_HEAD;
		};
	}
	
	/**
	 * Determines the appropriate color code for displaying a player's rank.
	 * <p>
	 * This method provides visual distinction through color coding, using
	 * gradient effects for the top 3 positions and standard white for others.
	 * The colors complement the material choices for enhanced visual hierarchy.
	 * </p>
	 *
	 * <h3>Color Mapping:</h3>
	 * <ul>
	 *   <li><strong>1st Place:</strong> Gold gradient (prestigious and eye-catching)</li>
	 *   <li><strong>2nd Place:</strong> Silver gradient (elegant second place)</li>
	 *   <li><strong>3rd Place:</strong> Bronze gradient (warm third place)</li>
	 *   <li><strong>Other Ranks:</strong> White (neutral and readable)</li>
	 * </ul>
	 *
	 * @param playerRank the player's ranking position (1-based), must be positive
	 * @return the color code string for this rank, never null
	 */
	private @NotNull String determineRankColor(
		final int playerRank
	) {
		return switch (playerRank) {
			case 1 -> "<gradient:#FFD700:#FFA500>";
			case 2 -> "<gradient:#C0C0C0:#A8A8A8>";
			case 3 -> "<gradient:#CD7F32:#B87333>";
			default -> "<white>";
		};
	}
	
	/**
	 * Formats a balance amount with the currency's prefix, symbol, and suffix.
	 * <p>
	 * This method creates a complete currency representation by combining the
	 * formatted amount with all currency display elements. The result provides
	 * a consistent and professional appearance for balance displays.
	 * </p>
	 *
	 * <h3>Format Structure:</h3>
	 * <p>
	 * The returned string follows the pattern: <code>prefix + formatted_amount + " " + symbol + suffix</code>
	 * </p>
	 *
	 * <h3>Example Output:</h3>
	 * <ul>
	 *   <li>Input: 1234.56, Currency with prefix="$", symbol="USD", suffix=""</li>
	 *   <li>Output: "$1,234.56 USD"</li>
	 * </ul>
	 *
	 * @param balanceAmount the raw balance amount to format
	 * @param targetCurrency the currency containing formatting information, must not be null
	 * @return the fully formatted balance string with all currency elements, never null
	 */
	private @NotNull String formatBalanceWithCurrency(
		final double balanceAmount,
		final @NotNull Currency targetCurrency
	) {
		final String formattedAmount = BALANCE_DECIMAL_FORMAT.format(balanceAmount);
		final String currencyPrefix = targetCurrency.getPrefix();
		final String currencySuffix = targetCurrency.getSuffix();
		
		return currencyPrefix + formattedAmount + " " + targetCurrency.getSymbol() + currencySuffix;
	}
}