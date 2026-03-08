package com.raindropcentral.rds.configs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Represents the fallback behavior when no material base price exists for an item currency.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public enum DynamicPricingMissingBasePriceMode {

    /**
     * Uses {@code default_item_price} from {@code config.yml}.
     */
    CONFIG_DEFAULT,

    /**
     * Uses the listing's manually configured shop-item value.
     */
    LISTING_VALUE,

    /**
     * Uses the fixed fallback amount from {@code dynamic_pricing.missing_base_price_fallback}.
     */
    FIXED_FALLBACK;

    /**
     * Parses a missing-base-price mode value.
     *
     * @param rawValue raw configuration value
     * @return parsed mode, defaulting to {@link #CONFIG_DEFAULT} when the value is blank or unknown
     */
    public static @NotNull DynamicPricingMissingBasePriceMode fromString(
            final @Nullable String rawValue
    ) {
        if (rawValue == null || rawValue.isBlank()) {
            return CONFIG_DEFAULT;
        }

        final String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "LISTING_VALUE", "SHOP_ITEM_VALUE", "ITEM_VALUE" -> LISTING_VALUE;
            case "FIXED_FALLBACK", "FIXED", "CUSTOM_FALLBACK" -> FIXED_FALLBACK;
            default -> CONFIG_DEFAULT;
        };
    }
}
