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
