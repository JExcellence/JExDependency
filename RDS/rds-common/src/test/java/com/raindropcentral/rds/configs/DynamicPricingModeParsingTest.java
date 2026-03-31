/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

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
