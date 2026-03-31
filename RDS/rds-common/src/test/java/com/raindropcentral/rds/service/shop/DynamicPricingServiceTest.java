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

package com.raindropcentral.rds.service.shop;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.raindropcentral.rds.configs.DynamicPricingMissingBasePriceMode;
import com.raindropcentral.rds.configs.DynamicPricingSection;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests dynamic-pricing service computation behavior.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class DynamicPricingServiceTest {

    @Test
    void computeQuoteAppliesStepCapsAndSmoothing(final @TempDir Path tempDir) throws IOException {
        final DynamicPricingSection dynamicPricing = loadDynamicPricingSection(
                tempDir,
                """
                    dynamic_pricing:
                      enabled: true
                      formula:
                        base_multiplier: 2.0
                        minimum_multiplier: 0.1
                        maximum_multiplier: 4.0
                        shops_selling_weight: 0.0
                        shops_selling_baseline: 1.0
                        listed_items_weight: 0.0
                        listed_items_baseline: 1.0
                        recent_sales_weight: 0.0
                        recent_sales_baseline: 1.0
                      stability:
                        minimum_price: 0.0
                        maximum_price: -1
                        max_step_change_percent: 0.10
                        smoothing_alpha: 1.0
                        rounding_scale: 2
                    """
        );

        final DynamicPricingService.DynamicPriceQuote quote = DynamicPricingService.computeQuote(
                20.0D,
                DynamicPricingMarketStats.empty(),
                10.0D,
                dynamicPricing,
                new WeightedDynamicPricingFormulaStrategy(),
                false
        );

        assertEquals(11.0D, quote.unitPrice(), 1.0E-9D);
        assertEquals(20.0D, quote.basePrice(), 1.0E-9D);
        assertEquals(2.0D, quote.multiplier(), 1.0E-9D);
    }

    @Test
    void resolveMissingBasePriceRespectsConfiguredModes() {
        assertEquals(
                42.0D,
                DynamicPricingService.resolveMissingBasePrice(
                        DynamicPricingMissingBasePriceMode.CONFIG_DEFAULT,
                        5.0D,
                        42.0D,
                        7.0D
                )
        );
        assertEquals(
                5.0D,
                DynamicPricingService.resolveMissingBasePrice(
                        DynamicPricingMissingBasePriceMode.LISTING_VALUE,
                        5.0D,
                        42.0D,
                        7.0D
                )
        );
        assertEquals(
                7.0D,
                DynamicPricingService.resolveMissingBasePrice(
                        DynamicPricingMissingBasePriceMode.FIXED_FALLBACK,
                        5.0D,
                        42.0D,
                        7.0D
                )
        );
        assertEquals(
                0.0D,
                DynamicPricingService.resolveMissingBasePrice(
                        DynamicPricingMissingBasePriceMode.FIXED_FALLBACK,
                        5.0D,
                        42.0D,
                        -100.0D
                )
        );
    }

    @Test
    void resolveInfiniteListedAmountHandlesConfiguredModes(final @TempDir Path tempDir) throws IOException {
        final DynamicPricingService.MarketIdentifier marketIdentifier =
                new DynamicPricingService.MarketIdentifier("DIAMOND", "vault");
        final Map<DynamicPricingService.MarketIdentifier, Long> highestFiniteByMarket = Map.of(
                marketIdentifier,
                20L
        );

        final DynamicPricingSection fixedCapSection = loadDynamicPricingSection(
                tempDir.resolve("fixed"),
                """
                    dynamic_pricing:
                      infinite_stock:
                        mode: FIXED_CAP
                        fixed_cap_amount: 100
                        count_as_seller: true
                    """
        );
        assertEquals(
                100L,
                DynamicPricingService.resolveInfiniteListedAmount(
                        marketIdentifier,
                        highestFiniteByMarket,
                        fixedCapSection.getInfiniteStockSettings()
                )
        );

        final DynamicPricingSection ignoreSection = loadDynamicPricingSection(
                tempDir.resolve("ignore"),
                """
                    dynamic_pricing:
                      infinite_stock:
                        mode: IGNORE
                        count_as_seller: false
                    """
        );
        assertEquals(
                0L,
                DynamicPricingService.resolveInfiniteListedAmount(
                        marketIdentifier,
                        highestFiniteByMarket,
                        ignoreSection.getInfiniteStockSettings()
                )
        );

        final DynamicPricingSection highestFiniteSection = loadDynamicPricingSection(
                tempDir.resolve("highest"),
                """
                    dynamic_pricing:
                      infinite_stock:
                        mode: USE_HIGHEST_FINITE
                        fallback_amount_when_no_finite: 300
                        count_as_seller: true
                    """
        );
        assertEquals(
                20L,
                DynamicPricingService.resolveInfiniteListedAmount(
                        marketIdentifier,
                        highestFiniteByMarket,
                        highestFiniteSection.getInfiniteStockSettings()
                )
        );
        assertEquals(
                300L,
                DynamicPricingService.resolveInfiniteListedAmount(
                        marketIdentifier,
                        Map.of(),
                        highestFiniteSection.getInfiniteStockSettings()
                )
        );
    }

    private static @NotNull DynamicPricingSection loadDynamicPricingSection(
            final @NotNull Path directory,
            final @NotNull String yaml
    ) throws IOException {
        Files.createDirectories(directory);
        final Path configFile = directory.resolve("config.yml");
        Files.writeString(configFile, yaml);
        return DynamicPricingSection.fromFile(configFile.toFile());
    }
}
