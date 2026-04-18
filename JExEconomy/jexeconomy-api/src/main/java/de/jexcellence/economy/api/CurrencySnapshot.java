package de.jexcellence.economy.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable snapshot of a currency's properties.
 *
 * <p>This is a lightweight, API-safe representation of a currency
 * without any persistence annotations. Third-party plugins receive
 * these from {@link EconomyProvider} and events.
 *
 * @param id         the internal database identifier
 * @param identifier the unique programmatic name (e.g. "coins")
 * @param symbol     the display symbol (e.g. "$")
 * @param prefix     text displayed before amounts
 * @param suffix     text displayed after amounts
 * @param icon       the Material name used as GUI icon
 * @author JExcellence
 * @since 3.0.0
 */
public record CurrencySnapshot(long id,
                                @NotNull String identifier,
                                @NotNull String symbol,
                                @NotNull String prefix,
                                @NotNull String suffix,
                                @NotNull String icon) {

    /**
     * Formats an amount using this currency's prefix, symbol, and suffix.
     *
     * @param amount the amount to format
     * @return the formatted string (e.g. "$100.00")
     */
    public @NotNull String format(double amount) {
        var formatted = String.format("%.2f", amount);
        return prefix + formatted + symbol + suffix;
    }
}
