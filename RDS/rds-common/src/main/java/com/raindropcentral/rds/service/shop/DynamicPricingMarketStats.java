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

/**
 * Represents aggregated market signals for one item material/currency pair.
 *
 * @param shopsSelling number of unique shops currently selling the item
 * @param listedItems effective listed stock amount
 * @param recentSales recent completed sales count according to configured sales mode
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public record DynamicPricingMarketStats(
        int shopsSelling,
        long listedItems,
        long recentSales
) {

    /**
     * Creates normalized market stats.
     *
     * @param shopsSelling number of unique shops currently selling the item
     * @param listedItems effective listed stock amount
     * @param recentSales recent completed sales count according to configured sales mode
     */
    public DynamicPricingMarketStats {
        shopsSelling = Math.max(0, shopsSelling);
        listedItems = Math.max(0L, listedItems);
        recentSales = Math.max(0L, recentSales);
    }

    /**
     * Returns an empty market-stats value.
     *
     * @return empty market-stats value
     */
    public static DynamicPricingMarketStats empty() {
        return new DynamicPricingMarketStats(0, 0L, 0L);
    }
}
