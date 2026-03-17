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

import java.io.File;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents dynamic-pricing settings.
 *
 * <p>This section controls how market signals influence runtime item prices. The pricing algorithm
 * uses material base prices, then applies formula weights, multiplier caps, and stability guards.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@CSAlways
@SuppressWarnings("unused")
public class DynamicPricingSection extends AConfigSection {

    private static final long DEFAULT_REFRESH_INTERVAL_TICKS = 200L;
    private static final long DEFAULT_RECENT_SALES_WINDOW_MINUTES = 180L;
    private static final double DEFAULT_MISSING_BASE_PRICE_FALLBACK = 0.0D;
    private static final double DEFAULT_BASE_MULTIPLIER = 1.0D;
    private static final double DEFAULT_MINIMUM_MULTIPLIER = 0.20D;
    private static final double DEFAULT_MAXIMUM_MULTIPLIER = 4.00D;
    private static final double DEFAULT_SHOPS_SELLING_WEIGHT = 0.35D;
    private static final double DEFAULT_SHOPS_SELLING_BASELINE = 4.0D;
    private static final double DEFAULT_LISTED_ITEMS_WEIGHT = 0.25D;
    private static final double DEFAULT_LISTED_ITEMS_BASELINE = 512.0D;
    private static final double DEFAULT_RECENT_SALES_WEIGHT = 0.60D;
    private static final double DEFAULT_RECENT_SALES_BASELINE = 128.0D;
    private static final double DEFAULT_MINIMUM_PRICE = 0.01D;
    private static final double DEFAULT_MAXIMUM_PRICE = -1.0D;
    private static final double DEFAULT_MAX_STEP_CHANGE_PERCENT = 0.35D;
    private static final double DEFAULT_SMOOTHING_ALPHA = 0.35D;
    private static final int DEFAULT_ROUNDING_SCALE = 2;
    private static final long DEFAULT_INFINITE_STOCK_FIXED_CAP_AMOUNT = 4096L;
    private static final long DEFAULT_INFINITE_STOCK_FALLBACK_AMOUNT = 512L;

    private Boolean enabled;
    private Long refresh_interval_ticks;
    private Long recent_sales_window_minutes;
    private String recent_sales_mode;
    private String missing_base_price_mode;
    private Double missing_base_price_fallback;

    private Double base_multiplier;
    private Double minimum_multiplier;
    private Double maximum_multiplier;
    private Double shops_selling_weight;
    private Double shops_selling_baseline;
    private Double listed_items_weight;
    private Double listed_items_baseline;
    private Double recent_sales_weight;
    private Double recent_sales_baseline;

    private Double minimum_price;
    private Double maximum_price;
    private Double max_step_change_percent;
    private Double smoothing_alpha;
    private Integer rounding_scale;

    private String infinite_stock_mode;
    private Long infinite_stock_fixed_cap_amount;
    private Long infinite_stock_fallback_amount;
    private Boolean infinite_stock_count_as_seller;

    /**
     * Creates a new dynamic-pricing section.
     *
     * @param baseEnvironment evaluation environment used for config expressions
     */
    public DynamicPricingSection(
            final @NotNull EvaluationEnvironmentBuilder baseEnvironment
    ) {
        super(baseEnvironment);
    }

    /**
     * Loads the dynamic-pricing section from the plugin config file.
     *
     * @param configFile plugin config file
     * @return parsed dynamic-pricing section
     */
    public static @NotNull DynamicPricingSection fromFile(
            final @NotNull File configFile
    ) {
        final DynamicPricingSection section = new DynamicPricingSection(new EvaluationEnvironmentBuilder());
        final ConfigurationSection dynamicSection = YamlConfiguration.loadConfiguration(configFile)
                .getConfigurationSection("dynamic_pricing");
        if (dynamicSection == null) {
            return section;
        }

        section.enabled = dynamicSection.contains("enabled")
                ? dynamicSection.getBoolean("enabled")
                : null;
        section.refresh_interval_ticks = dynamicSection.contains("refresh_interval_ticks")
                ? dynamicSection.getLong("refresh_interval_ticks")
                : null;
        section.recent_sales_window_minutes = dynamicSection.contains("recent_sales_window_minutes")
                ? dynamicSection.getLong("recent_sales_window_minutes")
                : null;
        section.recent_sales_mode = dynamicSection.getString("recent_sales_mode");
        section.missing_base_price_mode = dynamicSection.getString("missing_base_price_mode");
        section.missing_base_price_fallback = dynamicSection.contains("missing_base_price_fallback")
                ? dynamicSection.getDouble("missing_base_price_fallback")
                : null;

        final ConfigurationSection formulaSection = dynamicSection.getConfigurationSection("formula");
        if (formulaSection != null) {
            section.base_multiplier = formulaSection.contains("base_multiplier")
                    ? formulaSection.getDouble("base_multiplier")
                    : null;
            section.minimum_multiplier = formulaSection.contains("minimum_multiplier")
                    ? formulaSection.getDouble("minimum_multiplier")
                    : null;
            section.maximum_multiplier = formulaSection.contains("maximum_multiplier")
                    ? formulaSection.getDouble("maximum_multiplier")
                    : null;
            section.shops_selling_weight = formulaSection.contains("shops_selling_weight")
                    ? formulaSection.getDouble("shops_selling_weight")
                    : null;
            section.shops_selling_baseline = formulaSection.contains("shops_selling_baseline")
                    ? formulaSection.getDouble("shops_selling_baseline")
                    : null;
            section.listed_items_weight = formulaSection.contains("listed_items_weight")
                    ? formulaSection.getDouble("listed_items_weight")
                    : null;
            section.listed_items_baseline = formulaSection.contains("listed_items_baseline")
                    ? formulaSection.getDouble("listed_items_baseline")
                    : null;
            section.recent_sales_weight = formulaSection.contains("recent_sales_weight")
                    ? formulaSection.getDouble("recent_sales_weight")
                    : null;
            section.recent_sales_baseline = formulaSection.contains("recent_sales_baseline")
                    ? formulaSection.getDouble("recent_sales_baseline")
                    : null;
        }

        final ConfigurationSection stabilitySection = dynamicSection.getConfigurationSection("stability");
        if (stabilitySection != null) {
            section.minimum_price = stabilitySection.contains("minimum_price")
                    ? stabilitySection.getDouble("minimum_price")
                    : null;
            section.maximum_price = stabilitySection.contains("maximum_price")
                    ? stabilitySection.getDouble("maximum_price")
                    : null;
            section.max_step_change_percent = stabilitySection.contains("max_step_change_percent")
                    ? stabilitySection.getDouble("max_step_change_percent")
                    : null;
            section.smoothing_alpha = stabilitySection.contains("smoothing_alpha")
                    ? stabilitySection.getDouble("smoothing_alpha")
                    : null;
            section.rounding_scale = stabilitySection.contains("rounding_scale")
                    ? stabilitySection.getInt("rounding_scale")
                    : null;
        }

        final ConfigurationSection infiniteStockSection = dynamicSection.getConfigurationSection("infinite_stock");
        if (infiniteStockSection != null) {
            section.infinite_stock_mode = infiniteStockSection.getString("mode");
            section.infinite_stock_fixed_cap_amount = infiniteStockSection.contains("fixed_cap_amount")
                    ? infiniteStockSection.getLong("fixed_cap_amount")
                    : null;
            section.infinite_stock_fallback_amount = infiniteStockSection.contains("fallback_amount_when_no_finite")
                    ? infiniteStockSection.getLong("fallback_amount_when_no_finite")
                    : null;
            section.infinite_stock_count_as_seller = infiniteStockSection.contains("count_as_seller")
                    ? infiniteStockSection.getBoolean("count_as_seller")
                    : null;
        }

        return section;
    }

    /**
     * Indicates whether dynamic pricing is enabled.
     *
     * @return {@code true} when dynamic pricing is enabled
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(this.enabled);
    }

    /**
     * Returns the minimum number of ticks between market refreshes.
     *
     * @return refresh interval in ticks
     */
    public long getRefreshIntervalTicks() {
        return sanitizePositiveLong(this.refresh_interval_ticks, DEFAULT_REFRESH_INTERVAL_TICKS);
    }

    /**
     * Returns the recent-sales aggregation window in minutes.
     *
     * @return recent-sales window in minutes
     */
    public long getRecentSalesWindowMinutes() {
        return sanitizePositiveLong(this.recent_sales_window_minutes, DEFAULT_RECENT_SALES_WINDOW_MINUTES);
    }

    /**
     * Returns how recent sales should be counted.
     *
     * @return recent-sales counting mode
     */
    public @NotNull DynamicPricingRecentSalesMode getRecentSalesMode() {
        return DynamicPricingRecentSalesMode.fromString(this.recent_sales_mode);
    }

    /**
     * Returns the fallback mode used when no material base price exists for the current currency.
     *
     * @return missing-base-price mode
     */
    public @NotNull DynamicPricingMissingBasePriceMode getMissingBasePriceMode() {
        return DynamicPricingMissingBasePriceMode.fromString(this.missing_base_price_mode);
    }

    /**
     * Returns the fixed fallback value used by {@link DynamicPricingMissingBasePriceMode#FIXED_FALLBACK}.
     *
     * @return non-negative fixed fallback value
     */
    public double getMissingBasePriceFallback() {
        return sanitizeNonNegativeDouble(this.missing_base_price_fallback, DEFAULT_MISSING_BASE_PRICE_FALLBACK);
    }

    /**
     * Returns formula settings used to convert market signals into a raw price multiplier.
     *
     * @return dynamic-pricing formula settings
     */
    public @NotNull FormulaSettings getFormulaSettings() {
        return new FormulaSettings(
                sanitizeFiniteDouble(this.base_multiplier, DEFAULT_BASE_MULTIPLIER),
                sanitizeNonNegativeDouble(this.minimum_multiplier, DEFAULT_MINIMUM_MULTIPLIER),
                sanitizeFiniteDouble(this.maximum_multiplier, DEFAULT_MAXIMUM_MULTIPLIER),
                sanitizeFiniteDouble(this.shops_selling_weight, DEFAULT_SHOPS_SELLING_WEIGHT),
                sanitizePositiveDouble(this.shops_selling_baseline, DEFAULT_SHOPS_SELLING_BASELINE),
                sanitizeFiniteDouble(this.listed_items_weight, DEFAULT_LISTED_ITEMS_WEIGHT),
                sanitizePositiveDouble(this.listed_items_baseline, DEFAULT_LISTED_ITEMS_BASELINE),
                sanitizeFiniteDouble(this.recent_sales_weight, DEFAULT_RECENT_SALES_WEIGHT),
                sanitizePositiveDouble(this.recent_sales_baseline, DEFAULT_RECENT_SALES_BASELINE)
        );
    }

    /**
     * Returns stability settings used to control volatility.
     *
     * @return dynamic-pricing stability settings
     */
    public @NotNull StabilitySettings getStabilitySettings() {
        return new StabilitySettings(
                sanitizeNonNegativeDouble(this.minimum_price, DEFAULT_MINIMUM_PRICE),
                sanitizeMaximumPrice(this.maximum_price, DEFAULT_MAXIMUM_PRICE),
                sanitizeNonNegativeDouble(this.max_step_change_percent, DEFAULT_MAX_STEP_CHANGE_PERCENT),
                sanitizeSmoothingAlpha(this.smoothing_alpha, DEFAULT_SMOOTHING_ALPHA),
                sanitizeRoundingScale(this.rounding_scale, DEFAULT_ROUNDING_SCALE)
        );
    }

    /**
     * Returns infinite-stock handling settings.
     *
     * @return infinite-stock settings
     */
    public @NotNull InfiniteStockSettings getInfiniteStockSettings() {
        return new InfiniteStockSettings(
                DynamicPricingInfiniteStockMode.fromString(this.infinite_stock_mode),
                sanitizeNonNegativeLong(this.infinite_stock_fixed_cap_amount, DEFAULT_INFINITE_STOCK_FIXED_CAP_AMOUNT),
                sanitizeNonNegativeLong(this.infinite_stock_fallback_amount, DEFAULT_INFINITE_STOCK_FALLBACK_AMOUNT),
                this.infinite_stock_count_as_seller == null || this.infinite_stock_count_as_seller
        );
    }

    private static long sanitizePositiveLong(
            final @Nullable Long value,
            final long fallback
    ) {
        if (value == null || value <= 0L) {
            return fallback;
        }
        return value;
    }

    private static long sanitizeNonNegativeLong(
            final @Nullable Long value,
            final long fallback
    ) {
        if (value == null || value < 0L) {
            return fallback;
        }
        return value;
    }

    private static double sanitizePositiveDouble(
            final @Nullable Double value,
            final double fallback
    ) {
        if (value == null || !Double.isFinite(value) || value <= 0.0D) {
            return fallback;
        }
        return value;
    }

    private static double sanitizeNonNegativeDouble(
            final @Nullable Double value,
            final double fallback
    ) {
        if (value == null || !Double.isFinite(value) || value < 0.0D) {
            return fallback;
        }
        return value;
    }

    private static double sanitizeFiniteDouble(
            final @Nullable Double value,
            final double fallback
    ) {
        if (value == null || !Double.isFinite(value)) {
            return fallback;
        }
        return value;
    }

    private static double sanitizeMaximumPrice(
            final @Nullable Double value,
            final double fallback
    ) {
        if (value == null || !Double.isFinite(value)) {
            return fallback;
        }
        return value > 0.0D ? value : -1.0D;
    }

    private static double sanitizeSmoothingAlpha(
            final @Nullable Double value,
            final double fallback
    ) {
        final double normalized = sanitizeFiniteDouble(value, fallback);
        return Math.max(0.0D, Math.min(1.0D, normalized));
    }

    private static int sanitizeRoundingScale(
            final @Nullable Integer value,
            final int fallback
    ) {
        if (value == null || value < 0) {
            return fallback;
        }
        return Math.min(6, value);
    }

    /**
     * Represents formula parameters used to calculate an unbounded market multiplier.
     *
     * @param baseMultiplier baseline multiplier when market signals are neutral
     * @param minimumMultiplier minimum allowed multiplier before absolute price caps
     * @param maximumMultiplier maximum allowed multiplier before absolute price caps
     * @param shopsSellingWeight supply pressure weight for selling shop count
     * @param shopsSellingBaseline baseline used to normalize selling shop count
     * @param listedItemsWeight supply pressure weight for listed item count
     * @param listedItemsBaseline baseline used to normalize listed item count
     * @param recentSalesWeight demand pressure weight for recent sales
     * @param recentSalesBaseline baseline used to normalize recent sales
     *
     * @author ItsRainingHP
     * @since 1.0.0
     * @version 1.0.0
     */
    public record FormulaSettings(
            double baseMultiplier,
            double minimumMultiplier,
            double maximumMultiplier,
            double shopsSellingWeight,
            double shopsSellingBaseline,
            double listedItemsWeight,
            double listedItemsBaseline,
            double recentSalesWeight,
            double recentSalesBaseline
    ) {

        /**
         * Creates normalized formula settings.
         *
         * @param baseMultiplier baseline multiplier when market signals are neutral
         * @param minimumMultiplier minimum allowed multiplier before absolute price caps
         * @param maximumMultiplier maximum allowed multiplier before absolute price caps
         * @param shopsSellingWeight supply pressure weight for selling shop count
         * @param shopsSellingBaseline baseline used to normalize selling shop count
         * @param listedItemsWeight supply pressure weight for listed item count
         * @param listedItemsBaseline baseline used to normalize listed item count
         * @param recentSalesWeight demand pressure weight for recent sales
         * @param recentSalesBaseline baseline used to normalize recent sales
         */
        public FormulaSettings {
            minimumMultiplier = Math.max(0.0D, minimumMultiplier);
            maximumMultiplier = Double.isFinite(maximumMultiplier) ? maximumMultiplier : minimumMultiplier;
            if (maximumMultiplier < minimumMultiplier) {
                maximumMultiplier = minimumMultiplier;
            }

            baseMultiplier = Double.isFinite(baseMultiplier)
                    ? baseMultiplier
                    : minimumMultiplier;
            shopsSellingWeight = Double.isFinite(shopsSellingWeight) ? shopsSellingWeight : 0.0D;
            listedItemsWeight = Double.isFinite(listedItemsWeight) ? listedItemsWeight : 0.0D;
            recentSalesWeight = Double.isFinite(recentSalesWeight) ? recentSalesWeight : 0.0D;
            shopsSellingBaseline = Math.max(1.0D, shopsSellingBaseline);
            listedItemsBaseline = Math.max(1.0D, listedItemsBaseline);
            recentSalesBaseline = Math.max(1.0D, recentSalesBaseline);
        }
    }

    /**
     * Represents stability controls applied after formula multiplier computation.
     *
     * @param minimumPrice absolute minimum unit price
     * @param maximumPrice absolute maximum unit price ({@code -1.0} means unlimited)
     * @param maxStepChangePercent maximum percentage change allowed per refresh step
     * @param smoothingAlpha exponential smoothing alpha in range {@code [0.0, 1.0]}
     * @param roundingScale decimal places used for final rounding
     *
     * @author ItsRainingHP
     * @since 1.0.0
     * @version 1.0.0
     */
    public record StabilitySettings(
            double minimumPrice,
            double maximumPrice,
            double maxStepChangePercent,
            double smoothingAlpha,
            int roundingScale
    ) {

        /**
         * Creates normalized stability settings.
         *
         * @param minimumPrice absolute minimum unit price
         * @param maximumPrice absolute maximum unit price ({@code -1.0} means unlimited)
         * @param maxStepChangePercent maximum percentage change allowed per refresh step
         * @param smoothingAlpha exponential smoothing alpha in range {@code [0.0, 1.0]}
         * @param roundingScale decimal places used for final rounding
         */
        public StabilitySettings {
            minimumPrice = Math.max(0.0D, minimumPrice);
            maximumPrice = maximumPrice > 0.0D ? Math.max(maximumPrice, minimumPrice) : -1.0D;
            maxStepChangePercent = Math.max(0.0D, maxStepChangePercent);
            smoothingAlpha = Math.max(0.0D, Math.min(1.0D, smoothingAlpha));
            roundingScale = Math.max(0, Math.min(6, roundingScale));
        }
    }

    /**
     * Represents infinite-stock handling settings for listed-item market supply.
     *
     * @param mode mode used to translate infinite listings into effective listed-item supply
     * @param fixedCapAmount fixed amount used by {@link DynamicPricingInfiniteStockMode#FIXED_CAP}
     * @param fallbackAmountWhenNoFinite fallback amount for
     *                                   {@link DynamicPricingInfiniteStockMode#USE_HIGHEST_FINITE}
     *                                   when no finite listings exist
     * @param countAsSeller whether infinite listings count toward selling-shop totals
     *
     * @author ItsRainingHP
     * @since 1.0.0
     * @version 1.0.0
     */
    public record InfiniteStockSettings(
            @NotNull DynamicPricingInfiniteStockMode mode,
            long fixedCapAmount,
            long fallbackAmountWhenNoFinite,
            boolean countAsSeller
    ) {

        /**
         * Creates normalized infinite-stock settings.
         *
         * @param mode mode used to translate infinite listings into effective listed-item supply
         * @param fixedCapAmount fixed amount used by {@link DynamicPricingInfiniteStockMode#FIXED_CAP}
         * @param fallbackAmountWhenNoFinite fallback amount for
         *                                   {@link DynamicPricingInfiniteStockMode#USE_HIGHEST_FINITE}
         *                                   when no finite listings exist
         * @param countAsSeller whether infinite listings count toward selling-shop totals
         */
        public InfiniteStockSettings {
            mode = mode == null ? DynamicPricingInfiniteStockMode.FIXED_CAP : mode;
            fixedCapAmount = Math.max(0L, fixedCapAmount);
            fallbackAmountWhenNoFinite = Math.max(0L, fallbackAmountWhenNoFinite);
        }
    }
}
