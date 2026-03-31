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

import com.raindropcentral.rds.configs.DynamicPricingSection;
import org.jetbrains.annotations.NotNull;

/**
 * Applies a weighted demand/supply formula to produce a raw market multiplier.
 *
 * <p>Demand signal ({@code recentSales}) increases prices, while supply signals
 * ({@code shopsSelling} and {@code listedItems}) reduce prices.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class WeightedDynamicPricingFormulaStrategy implements DynamicPricingFormulaStrategy {

    /**
     * Computes a raw multiplier using configured baselines and weights.
     *
     * @param marketStats aggregated market signals for one material/currency market
     * @param formulaSettings configured formula settings
     * @return unbounded market multiplier
     */
    @Override
    public double computeRawMultiplier(
            final @NotNull DynamicPricingMarketStats marketStats,
            final @NotNull DynamicPricingSection.FormulaSettings formulaSettings
    ) {
        final double normalizedShopsSelling = marketStats.shopsSelling() / formulaSettings.shopsSellingBaseline();
        final double normalizedListedItems = marketStats.listedItems() / formulaSettings.listedItemsBaseline();
        final double normalizedRecentSales = marketStats.recentSales() / formulaSettings.recentSalesBaseline();

        return formulaSettings.baseMultiplier()
                + (normalizedRecentSales * formulaSettings.recentSalesWeight())
                - (normalizedShopsSelling * formulaSettings.shopsSellingWeight())
                - (normalizedListedItems * formulaSettings.listedItemsWeight());
    }
}
