package com.raindropcentral.rds.service.shop;

import com.raindropcentral.rds.configs.DynamicPricingSection;
import org.jetbrains.annotations.NotNull;

/**
 * Computes a raw market multiplier for dynamic pricing.
 *
 * <p>The returned multiplier is later bounded by config multiplier caps and stability guards.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public interface DynamicPricingFormulaStrategy {

    /**
     * Computes a raw price multiplier.
     *
     * @param marketStats aggregated market signals for one material/currency market
     * @param formulaSettings configured formula settings
     * @return unbounded market multiplier
     */
    double computeRawMultiplier(
            @NotNull DynamicPricingMarketStats marketStats,
            @NotNull DynamicPricingSection.FormulaSettings formulaSettings
    );
}
