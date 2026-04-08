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
import org.junit.jupiter.api.Test;

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
