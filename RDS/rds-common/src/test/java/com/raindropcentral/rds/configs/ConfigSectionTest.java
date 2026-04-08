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

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests config section behavior.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class ConfigSectionTest {

    @Test
    void returnsExpectedDefaultsWhenValuesAreUnset() {
        final ConfigSection section = new ConfigSection(new EvaluationEnvironmentBuilder());

        assertEquals(10, section.getMaxShops());
        assertTrue(section.hasShopLimit());
        assertTrue(section.shouldWarnMissingRequirements());
        assertTrue(section.getRequirements().isEmpty());
        assertEquals(0.0D, section.getDefaultItemPrice());
        assertFalse(section.getDynamicPricing().isEnabled());
        assertEquals(200L, section.getDynamicPricing().getRefreshIntervalTicks());
        assertEquals(
            IntStream.rangeClosed(1, 10).boxed().toList(),
            section.getMissingRequirementPurchases()
        );
    }

    @Test
    void readsMultipleRequirementEntriesFromYamlFile(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            max_shops: 4
            warn_missing_requirements: false
            requirements:
              2:
                vault_purchase:
                  type: "CURRENCY"
                  icon:
                    type: "GOLD_INGOT"
                  currency: vault
                  amount: 1265.62
                supply_gate:
                  type: "ITEM"
                  icon:
                    type: "CHEST"
                  requiredItems:
                    dirt:
                      type: "DIRT"
                      amount: 16
            """);

        final ConfigSection section = ConfigSection.fromFile(configFile.toFile());

        assertFalse(section.shouldWarnMissingRequirements());
        assertEquals(1, section.getRequirements().size());
        assertTrue(section.getRequirements().containsKey(2));
        assertEquals("CURRENCY", section.getRequirementsForPurchase(2).get("vault_purchase").getType());
        assertEquals("ITEM", section.getRequirementsForPurchase(2).get("supply_gate").getType());
        assertEquals("CHEST", section.getRequirementsForPurchase(2).get("supply_gate").getIcon().getType());
        assertTrue(section.getRequirementsForPurchase(4).isEmpty());
        assertEquals(List.of(1, 3, 4), section.getMissingRequirementPurchases());
    }

    @Test
    void logsMissingPurchaseRequirementWarningsForFiniteLimits(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            max_shops: 4
            warn_missing_requirements: true
            requirements:
              1:
                vault_purchase:
                  type: "CURRENCY"
                  icon:
                    type: "GOLD_INGOT"
                  currency: vault
                  amount: 1000.0
            """);

        final ConfigSection section = ConfigSection.fromFile(configFile.toFile());
        final Logger logger = Logger.getLogger("RDSConfigSectionTestFinite");
        logger.setUseParentHandlers(false);
        final CapturingHandler handler = new CapturingHandler();
        logger.addHandler(handler);

        section.logMissingRequirementWarnings(logger);

        logger.removeHandler(handler);
        assertEquals(1, handler.messages.size());
        assertTrue(handler.messages.getFirst().contains("2, 3, 4"));
    }

    @Test
    void logsUnlimitedPurchaseRequirementWarningBeyondHighestConfiguredTier(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            max_shops: -1
            warn_missing_requirements: true
            requirements:
              1:
                vault_purchase:
                  type: "CURRENCY"
                  icon:
                    type: "GOLD_INGOT"
                  currency: vault
                  amount: 1000.0
            """);

        final ConfigSection section = ConfigSection.fromFile(configFile.toFile());
        final Logger logger = Logger.getLogger("RDSConfigSectionTestUnlimited");
        logger.setUseParentHandlers(false);
        final CapturingHandler handler = new CapturingHandler();
        logger.addHandler(handler);

        section.logMissingRequirementWarnings(logger);

        logger.removeHandler(handler);
        assertEquals(1, handler.messages.size());
        assertTrue(handler.messages.getFirst().contains("Purchases above that tier"));
    }

    @Test
    void loadsServerBankConfiguration(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            server_bank:
              enabled: false
              transfer_interval_ticks: 3600
            """);

        final ConfigSection section = ConfigSection.fromFile(configFile.toFile());
        assertFalse(section.getServerBank().isEnabled());
        assertEquals(3600L, section.getServerBank().getTransferIntervalTicks());
    }

    @Test
    void loadsProtectionConfiguration(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            protection:
              only_player_shops: true
              shop_taxes_fallback_to_player: true
              shop_taxes:
                vault: 5000.0
                coins: 1000.0
            """);

        final ConfigSection section = ConfigSection.fromFile(configFile.toFile());
        assertTrue(section.getProtection().isOnlyPlayerShops());
        assertTrue(section.getProtection().isShopTaxesFallbackToPlayer());
        assertTrue(section.getProtection().isShopTaxCurrency("vault"));
        assertTrue(section.getProtection().isShopTaxCurrency("coins"));
        assertFalse(section.getProtection().isShopTaxCurrency("gems"));
        assertEquals(5000.0D, section.getProtection().getShopTaxMaximum("vault"));
        assertEquals(1000.0D, section.getProtection().getShopTaxMaximum("coins"));
    }

    @Test
    void readsDefaultItemPriceFromConfig(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            default_item_price: 42.5
            """);

        final ConfigSection section = ConfigSection.fromFile(configFile.toFile());
        assertEquals(42.5D, section.getDefaultItemPrice());
    }

    @Test
    void loadsDynamicPricingConfiguration(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            dynamic_pricing:
              enabled: true
              refresh_interval_ticks: 120
              recent_sales_window_minutes: 90
              recent_sales_mode: TRANSACTIONS
              missing_base_price_mode: FIXED_FALLBACK
              missing_base_price_fallback: 3.5
              formula:
                base_multiplier: 1.4
                minimum_multiplier: 0.5
                maximum_multiplier: 2.5
                shops_selling_weight: 0.4
                shops_selling_baseline: 6.0
                listed_items_weight: 0.2
                listed_items_baseline: 256.0
                recent_sales_weight: 0.75
                recent_sales_baseline: 64.0
              stability:
                minimum_price: 0.5
                maximum_price: 9999
                max_step_change_percent: 0.15
                smoothing_alpha: 0.25
                rounding_scale: 3
              infinite_stock:
                mode: USE_HIGHEST_FINITE
                fixed_cap_amount: 2048
                fallback_amount_when_no_finite: 300
                count_as_seller: false
            """);

        final ConfigSection section = ConfigSection.fromFile(configFile.toFile());
        final DynamicPricingSection dynamicPricing = section.getDynamicPricing();

        assertTrue(dynamicPricing.isEnabled());
        assertEquals(120L, dynamicPricing.getRefreshIntervalTicks());
        assertEquals(90L, dynamicPricing.getRecentSalesWindowMinutes());
        assertEquals(DynamicPricingRecentSalesMode.TRANSACTIONS, dynamicPricing.getRecentSalesMode());
        assertEquals(DynamicPricingMissingBasePriceMode.FIXED_FALLBACK, dynamicPricing.getMissingBasePriceMode());
        assertEquals(3.5D, dynamicPricing.getMissingBasePriceFallback());

        final DynamicPricingSection.FormulaSettings formulaSettings = dynamicPricing.getFormulaSettings();
        assertEquals(1.4D, formulaSettings.baseMultiplier());
        assertEquals(0.5D, formulaSettings.minimumMultiplier());
        assertEquals(2.5D, formulaSettings.maximumMultiplier());
        assertEquals(0.4D, formulaSettings.shopsSellingWeight());
        assertEquals(6.0D, formulaSettings.shopsSellingBaseline());
        assertEquals(0.2D, formulaSettings.listedItemsWeight());
        assertEquals(256.0D, formulaSettings.listedItemsBaseline());
        assertEquals(0.75D, formulaSettings.recentSalesWeight());
        assertEquals(64.0D, formulaSettings.recentSalesBaseline());

        final DynamicPricingSection.StabilitySettings stabilitySettings = dynamicPricing.getStabilitySettings();
        assertEquals(0.5D, stabilitySettings.minimumPrice());
        assertEquals(9999.0D, stabilitySettings.maximumPrice());
        assertEquals(0.15D, stabilitySettings.maxStepChangePercent());
        assertEquals(0.25D, stabilitySettings.smoothingAlpha());
        assertEquals(3, stabilitySettings.roundingScale());

        final DynamicPricingSection.InfiniteStockSettings infiniteStockSettings = dynamicPricing.getInfiniteStockSettings();
        assertEquals(DynamicPricingInfiniteStockMode.USE_HIGHEST_FINITE, infiniteStockSettings.mode());
        assertEquals(2048L, infiniteStockSettings.fixedCapAmount());
        assertEquals(300L, infiniteStockSettings.fallbackAmountWhenNoFinite());
        assertFalse(infiniteStockSettings.countAsSeller());
    }

    private static double getDouble(final Map<String, Object> definition, final String key) {
        return ((Number) definition.get(key)).doubleValue();
    }

    private static final class CapturingHandler extends Handler {

        private final List<String> messages = new ArrayList<>();

        @Override
        public void publish(final LogRecord record) {
            if (record != null) {
                this.messages.add(record.getMessage());
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
