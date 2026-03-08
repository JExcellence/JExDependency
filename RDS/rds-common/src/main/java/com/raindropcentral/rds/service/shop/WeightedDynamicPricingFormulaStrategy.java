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
