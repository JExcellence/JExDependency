package de.jexcellence.economy.placeholder;

import com.raindropcentral.rplatform.placeholder.AbstractPlaceholderExpansion;
import de.jexcellence.economy.JExEconomy;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Placeholder expansion for currency-related placeholders in the JExEconomy system.
 *
 * <p>This class integrates with the platform's placeholder system to provide dynamic
 * currency and player balance information for use in chat, scoreboards, and other
 * placeholder-aware components. It delegates placeholder resolution to
 * {@link CurrencyPlaceholderUtil} and defines the supported placeholder formats.
 *
 * <p><strong>Supported Placeholder Categories:</strong>
 * <ul>
 *   <li><strong>Currency Information:</strong> Basic currency metadata and formatting</li>
 *   <li><strong>Player Balances:</strong> Raw balance amounts with various formatting options</li>
 *   <li><strong>Formatted Balances:</strong> Complete balance strings with currency symbols and formatting</li>
 * </ul>
 *
 * <p><strong>Supported Placeholder Formats:</strong>
 * <ul>
 *   <li><code>currency_&lt;currency&gt;_name</code> - The name/identifier of a currency</li>
 *   <li><code>currency_&lt;currency&gt;_symbol</code> - The symbol of a currency</li>
 *   <li><code>currency_&lt;currency&gt;_prefix</code> - The prefix for a currency</li>
 *   <li><code>currency_&lt;currency&gt;_suffix</code> - The suffix for a currency</li>
 *   <li><code>currency_&lt;currency&gt;_currencies</code> - The formatted currency string (prefix + name + suffix)</li>
 *   <li><code>player_currency_&lt;currency&gt;_amount</code> - The player's balance in a currency (2 decimals)</li>
 *   <li><code>player_currency_&lt;currency&gt;_amount-rounded</code> - The player's balance rounded to integer</li>
 *   <li><code>player_currency_&lt;currency&gt;_amount-rounded-dots</code> - The player's balance rounded with locale dots</li>
 *   <li><code>player_formatted_currency_&lt;currency&gt;_amount</code> - The player's formatted balance (prefix + amount + symbol + suffix)</li>
 *   <li><code>player_formatted_currency_&lt;currency&gt;_amount-rounded</code> - The player's formatted rounded balance</li>
 *   <li><code>player_formatted_currency_&lt;currency&gt;_amount-rounded-dots</code> - The player's formatted rounded balance with locale dots</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>{@code
 * // In chat or scoreboard configuration:
 * "Your gold balance: %jexeconomy_player_formatted_currency_gold_amount%"
 * "Server economy: %jexeconomy_currency_gold_symbol%"
 * }</pre>
 *
 * @author JExcellence
 * @see CurrencyPlaceholderUtil
 */
public class Placeholder extends AbstractPlaceholderExpansion {

    /**
     * Utility for resolving and formatting currency placeholders.
     */
    private final CurrencyPlaceholderUtil currencyPlaceholderUtil;

    /**
     * Constructs a new {@code Placeholder} expansion for the given currency plugin.
 *
 * <p>Initializes the placeholder expansion with the platform abstraction layer and
     * creates a {@link CurrencyPlaceholderUtil} instance for handling placeholder resolution.
     * The expansion will be automatically registered with the platform's placeholder system.
     *
     * @param jexEconomyImpl the main JExEconomy plugin instance, must not be null
     * @throws IllegalArgumentException if the plugin instance is null
     */
    public Placeholder(
            final @NotNull JExEconomy jexEconomyImpl
    ) {
        super(jexEconomyImpl.getPlugin());
        this.currencyPlaceholderUtil = new CurrencyPlaceholderUtil(jexEconomyImpl);
    }

    /**
     * Defines the list of supported placeholder formats for this expansion.
 *
 * <p>This method returns a comprehensive list of all placeholder patterns supported
     * by the currency system. The placeholders are organized into three main categories:
     * currency information, player balances, and formatted balances.
     *
     * <p><strong>Placeholder Categories:</strong>
     * <ul>
     *   <li><strong>Currency Info:</strong> Basic currency metadata (name, symbol, prefix, suffix)</li>
     *   <li><strong>Player Currency:</strong> Raw balance amounts with formatting options</li>
     *   <li><strong>Formatted Currency:</strong> Complete balance strings with all currency elements</li>
     * </ul>
     *
     * @return a list of placeholder format strings, never null or empty
     */
    @Override
    protected @NotNull List<String> definePlaceholders() {
        return List.of(
                "currency_<currency>_name",
                "currency_<currency>_symbol",
                "currency_<currency>_prefix",
                "currency_<currency>_suffix",
                "currency_<currency>_currencies",
                "player_currency_<currency>_amount",
                "player_currency_<currency>_amount-rounded",
                "player_currency_<currency>_amount-rounded-dots",
                "player_formatted_currency_<currency>_amount",
                "player_formatted_currency_<currency>_amount-rounded",
                "player_formatted_currency_<currency>_amount-rounded-dots"
        );
    }

    /**
     * Resolves a placeholder for a given player and parameter string.
 *
 * <p>This method serves as the main entry point for placeholder resolution within the
     * platform's placeholder system. It validates the input parameters and delegates
     * the actual resolution to {@link CurrencyPlaceholderUtil} for supported placeholder formats.
     *
     * <p><strong>Supported Placeholder Prefixes:</strong>
     * <ul>
     *   <li><code>currency_</code> - Currency information placeholders</li>
     *   <li><code>player_currency_</code> - Raw player balance placeholders</li>
     *   <li><code>player_formatted_currency_</code> - Formatted player balance placeholders</li>
     * </ul>
     *
     * <p><strong>Error Handling:</strong>
 *
 * <p>If the player is null or the placeholder parameters don't match any supported
     * format, an empty string is returned to indicate that the placeholder is not
     * handled by this expansion.
     *
     * @param player the player for whom the placeholder is being resolved, can be null
     * @param params the placeholder parameter string, must not be null
     * @return the resolved placeholder value, or an empty string if not applicable
     * @throws IllegalArgumentException if params is null
     */
    @Override
    protected @Nullable String resolvePlaceholder(@Nullable Player player, @NotNull String params) {
        {

            if (
                    player == null
            ) {
                return "";
            }

            if (
                    !params.startsWith("currency_") &&
                            !params.startsWith("player_currency_") &&
                            !params.startsWith("player_formatted_currency_")
            ) {
                return "";
            }

            final String resolvedPlaceholder = this.currencyPlaceholderUtil.processPlaceholder(
                    player,
                    params
            );

            return resolvedPlaceholder != null ? resolvedPlaceholder : "";
        }
    }
}
