package com.raindropcentral.rds.configs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Represents how recent sales are counted for dynamic-pricing demand signals.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public enum DynamicPricingRecentSalesMode {

    /**
     * Counts each completed purchase transaction as one sale.
     */
    TRANSACTIONS,

    /**
     * Counts the number of purchased item units across completed transactions.
     */
    ITEM_UNITS;

    /**
     * Parses a recent-sales mode value.
     *
     * @param rawValue raw configuration value
     * @return parsed mode, defaulting to {@link #ITEM_UNITS} when the value is blank or unknown
     */
    public static @NotNull DynamicPricingRecentSalesMode fromString(
            final @Nullable String rawValue
    ) {
        if (rawValue == null || rawValue.isBlank()) {
            return ITEM_UNITS;
        }

        final String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "TRANSACTION", "TRANSACTIONS", "PURCHASES" -> TRANSACTIONS;
            default -> ITEM_UNITS;
        };
    }
}
