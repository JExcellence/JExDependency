package com.raindropcentral.rds.configs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Represents how dynamic pricing should treat infinite stock listings.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public enum DynamicPricingInfiniteStockMode {

    /**
     * Ignores infinite listings for the listed-item supply signal.
     */
    IGNORE,

    /**
     * Uses a fixed configured cap amount for each infinite listing.
     */
    FIXED_CAP,

    /**
     * Uses the highest finite listing amount seen for the same item/currency market.
     */
    USE_HIGHEST_FINITE;

    /**
     * Parses an infinite-stock mode value.
     *
     * @param rawValue raw configuration value
     * @return parsed mode, defaulting to {@link #FIXED_CAP} when the value is blank or unknown
     */
    public static @NotNull DynamicPricingInfiniteStockMode fromString(
            final @Nullable String rawValue
    ) {
        if (rawValue == null || rawValue.isBlank()) {
            return FIXED_CAP;
        }

        final String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "IGNORE", "EXCLUDE" -> IGNORE;
            case "USE_HIGHEST_FINITE", "HIGHEST_FINITE", "MATCH_HIGHEST_FINITE" -> USE_HIGHEST_FINITE;
            default -> FIXED_CAP;
        };
    }
}
