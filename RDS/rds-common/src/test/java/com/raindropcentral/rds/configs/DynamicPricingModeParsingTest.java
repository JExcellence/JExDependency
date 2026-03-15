package com.raindropcentral.rds.configs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests parsing behavior for dynamic-pricing mode enums.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class DynamicPricingModeParsingTest {

    @Test
    void parsesInfiniteStockModeAliasesAndDefaults() {
        assertEquals(
            DynamicPricingInfiniteStockMode.FIXED_CAP,
            DynamicPricingInfiniteStockMode.fromString(null)
        );
        assertEquals(
            DynamicPricingInfiniteStockMode.IGNORE,
            DynamicPricingInfiniteStockMode.fromString("exclude")
        );
        assertEquals(
            DynamicPricingInfiniteStockMode.USE_HIGHEST_FINITE,
            DynamicPricingInfiniteStockMode.fromString("match_highest_finite")
        );
        assertEquals(
            DynamicPricingInfiniteStockMode.FIXED_CAP,
            DynamicPricingInfiniteStockMode.fromString("unknown")
        );
    }

    @Test
    void parsesMissingBasePriceModeAliasesAndDefaults() {
        assertEquals(
            DynamicPricingMissingBasePriceMode.CONFIG_DEFAULT,
            DynamicPricingMissingBasePriceMode.fromString(null)
        );
        assertEquals(
            DynamicPricingMissingBasePriceMode.LISTING_VALUE,
            DynamicPricingMissingBasePriceMode.fromString("item_value")
        );
        assertEquals(
            DynamicPricingMissingBasePriceMode.FIXED_FALLBACK,
            DynamicPricingMissingBasePriceMode.fromString("fixed")
        );
        assertEquals(
            DynamicPricingMissingBasePriceMode.CONFIG_DEFAULT,
            DynamicPricingMissingBasePriceMode.fromString("unknown")
        );
    }

    @Test
    void parsesRecentSalesModeAliasesAndDefaults() {
        assertEquals(
            DynamicPricingRecentSalesMode.ITEM_UNITS,
            DynamicPricingRecentSalesMode.fromString(null)
        );
        assertEquals(
            DynamicPricingRecentSalesMode.TRANSACTIONS,
            DynamicPricingRecentSalesMode.fromString("purchases")
        );
        assertEquals(
            DynamicPricingRecentSalesMode.ITEM_UNITS,
            DynamicPricingRecentSalesMode.fromString("unknown")
        );
    }
}
