package com.raindropcentral.rds.service.shop;

import org.junit.jupiter.api.Test;

import com.raindropcentral.rds.configs.DynamicPricingSection;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests weighted dynamic-pricing formula behavior.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class WeightedDynamicPricingFormulaStrategyTest {

    @Test
    void computesExpectedRawMultiplierFromWeightedSignals() {
        final DynamicPricingSection.FormulaSettings formulaSettings = new DynamicPricingSection.FormulaSettings(
                1.0D,
                0.2D,
                4.0D,
                0.35D,
                4.0D,
                0.25D,
                100.0D,
                0.60D,
                20.0D
        );
        final DynamicPricingMarketStats marketStats = new DynamicPricingMarketStats(8, 200L, 40L);

        final double multiplier = new WeightedDynamicPricingFormulaStrategy().computeRawMultiplier(
                marketStats,
                formulaSettings
        );

        assertEquals(1.0D, multiplier, 1.0E-9D);
    }
}
