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

package com.raindropcentral.rdr.configs;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests config section behavior.
 */
class ConfigSectionTest {

    @Test
    void returnsExpectedDefaultsWhenValuesAreUnset() {
        final ConfigSection section = new ConfigSection(new EvaluationEnvironmentBuilder());

        assertEquals(1, section.getStartingStorages());
        assertEquals(1, section.getMaxStorages());
        assertEquals(9, section.getMaxHotkeys());
        assertTrue(section.shouldWarnMissingRequirements());
        assertEquals(List.of("NETHER_STAR"), section.getGlobalBlacklist());
        assertTrue(section.isGloballyBlacklisted("nether_star"));
        assertTrue(section.getRequirements().isEmpty());
        assertTrue(section.getMissingRequirementPurchases().isEmpty());
        assertEquals(1, section.getInitialProvisionedStorages());
        assertEquals("none", section.getProtectionRestrictedStorages());
        assertEquals("all", section.getProtectionTaxedStorages());
        assertEquals(0.0D, section.getProtectionOpenStorageTaxes().get("vault"));
        assertEquals(0.0D, section.getProtectionOpenStorageTaxes().get("raindrops"));
        assertEquals(1_728_000L, section.getProtectionFilledStorageTaxIntervalTicks());
        assertEquals(-1, section.getProtectionFilledStorageMaximumFreeze());
        assertEquals(-1.0D, section.getProtectionFilledStorageMaximumDebtByCurrency().get("vault"));
        assertEquals(-1.0D, section.getProtectionFilledStorageMaximumDebtByCurrency().get("raindrops"));
        assertEquals(0.0D, section.getProtectionFilledStorageTaxes().get("vault"));
        assertEquals(0.0D, section.getProtectionFilledStorageTaxes().get("raindrops"));
        assertTrue(section.isTradeEnabled());
        assertEquals(60L, section.getTradeInviteTimeoutSeconds());
        assertEquals(20L, section.getTradePollIntervalTicks());
        assertEquals(9, section.getTradeMaxOfferSlots());
        assertEquals(5L, section.getTradeInviteCooldownSeconds());
        assertFalse(section.isProxyEnabled());
        assertFalse(section.isTradeProxyPresenceEnabled());
        assertFalse(section.isTradeProxyJoinActionEnabled());
        assertEquals("", section.getProxyServerRouteId());
        assertFalse(section.isTradeTaxationEnabled());
        assertEquals(ConfigSection.TradeTaxMode.FLAT, section.getTradeTaxationCurrencies().get("vault").mode());
        assertEquals(0.0D, section.getTradeTaxationCurrencies().get("vault").flatAmount());
        assertEquals(ConfigSection.TradeTaxMode.FLAT, section.getTradeTaxationCurrencies().get("raindrops").mode());
        assertEquals(0.0D, section.getTradeTaxationCurrencies().get("raindrops").flatAmount());
        assertFalse(section.isProtectionRestrictedStorage("storage-1"));
        assertTrue(section.isProtectionTaxedStorage("storage-1"));
    }

    @Test
    void readsMaxStoragesFromYamlFile(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            starting_storages: 1
            max_storages: 10
            max_hotkeys: 9
            global_blacklist:
              - NETHER_STAR
              - bedrock
            warn_missing_requirements: true
            requirements:
              "2":
                vault_purchase:
                  type: "CURRENCY"
                  icon:
                    type: "GOLD_NUGGET"
                  currency: vault
                  amount: 1265.62
            """);

        final ConfigSection section = ConfigSection.fromFile(configFile.toFile());

        assertEquals(10, section.getMaxStorages());
        assertEquals(List.of("NETHER_STAR", "BEDROCK"), section.getGlobalBlacklist());
        assertTrue(section.isGloballyBlacklisted("bedrock"));
    }

    @Test
    void readsMultipleRequirementEntriesFromYamlFile(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            starting_storages: 1
            max_storages: 10
            max_hotkeys: 9
            warn_missing_requirements: false
            requirements:
              "2":
                vault_purchase:
                  type: "CURRENCY"
                  icon:
                    type: "GOLD_NUGGET"
                  currency: vault
                  amount: 1265.62
                access_gate:
                  type: "PERMISSION"
                  icon:
                    type: "PAPER"
                  requiredPermissions:
                    - "rdr.store.access"
            """);

        final ConfigSection section = ConfigSection.fromFile(configFile.toFile());

        assertFalse(section.shouldWarnMissingRequirements());
        assertEquals(1, section.getRequirements().size());
        assertTrue(section.getRequirements().containsKey(2));
        assertTrue(section.getRequirementsForPurchase(2).containsKey("vault_purchase"));
        assertTrue(section.getRequirementsForPurchase(2).containsKey("access_gate"));
        assertEquals("PERMISSION", section.getRequirementsForPurchase(2).get("access_gate").getType());
        assertEquals("PAPER", section.getRequirementsForPurchase(2).get("access_gate").getIcon().getType());
        assertTrue(section.getRequirementsForPurchase(9).isEmpty());
        assertEquals(List.of(1, 3, 4, 5, 6, 7, 8, 9), section.getMissingRequirementPurchases());
    }

    @Test
    void parsesProtectionSelectorsAndStorageTaxes(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            starting_storages: 1
            max_storages: 10
            max_hotkeys: 9
            protection:
              restricted_storages: "1-3,5"
              taxed_storages: "2,4-6"
              open_storage_taxes:
                vault: 12.5
                raindrops: 3.75
                gems: 2.25
              filled_storage_taxes:
                interval_ticks: 1440
                maximum_freeze: 3
                maximum_debt:
                  vault: 120.0
                  raindrops: 45.5
                  gems: 8.0
                currencies:
                  vault: 4.5
                  raindrops: 1.5
                  gems: 0.25
            """);

        final ConfigSection section = ConfigSection.fromFile(configFile.toFile());

        assertEquals("1-3,5", section.getProtectionRestrictedStorages());
        assertEquals("2,4-6", section.getProtectionTaxedStorages());
        assertTrue(section.isProtectionRestrictedStorage("storage-1"));
        assertTrue(section.isProtectionRestrictedStorage("storage-3"));
        assertTrue(section.isProtectionRestrictedStorage("storage-5"));
        assertFalse(section.isProtectionRestrictedStorage("storage-4"));
        assertFalse(section.isProtectionRestrictedStorage("storage-20"));
        assertFalse(section.isProtectionRestrictedStorage("custom-key"));
        assertFalse(section.isProtectionRestrictedStorage(""));
        assertTrue(section.isProtectionTaxedStorage("storage-2"));
        assertTrue(section.isProtectionTaxedStorage("storage-4"));
        assertTrue(section.isProtectionTaxedStorage("storage-6"));
        assertFalse(section.isProtectionTaxedStorage("storage-1"));
        assertEquals(12.5D, section.getProtectionOpenStorageTaxes().get("vault"));
        assertEquals(3.75D, section.getProtectionOpenStorageTaxes().get("raindrops"));
        assertEquals(2.25D, section.getProtectionOpenStorageTaxes().get("gems"));
        assertEquals(1440L, section.getProtectionFilledStorageTaxIntervalTicks());
        assertEquals(3, section.getProtectionFilledStorageMaximumFreeze());
        assertEquals(120.0D, section.getProtectionFilledStorageMaximumDebtByCurrency().get("vault"));
        assertEquals(45.5D, section.getProtectionFilledStorageMaximumDebtByCurrency().get("raindrops"));
        assertEquals(8.0D, section.getProtectionFilledStorageMaximumDebtByCurrency().get("gems"));
        assertEquals(4.5D, section.getProtectionFilledStorageTaxes().get("vault"));
        assertEquals(1.5D, section.getProtectionFilledStorageTaxes().get("raindrops"));
        assertEquals(0.25D, section.getProtectionFilledStorageTaxes().get("gems"));
    }

    @Test
    void normalizesInvalidProtectionSelectorsAndTaxValues(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            starting_storages: 1
            max_storages: 2
            max_hotkeys: 9
            protection:
              restricted_storages: "invalid,3-"
              taxed_storages: "none"
              open_storage_taxes:
                vault: -15
                raindrops: "not-a-number"
              filled_storage_taxes:
                interval_ticks: -1
                maximum_freeze: 0
                maximum_debt:
                  vault: -3
                  raindrops: "oops"
                  gems: 11.5
                currencies:
                  vault: -3
                  raindrops: "oops"
            """);

        final ConfigSection section = ConfigSection.fromFile(configFile.toFile());

        assertFalse(section.isProtectionRestrictedStorage("storage-1"));
        assertFalse(section.isProtectionRestrictedStorage("storage-3"));
        assertFalse(section.isProtectionTaxedStorage("storage-1"));
        assertEquals(0.0D, section.getProtectionOpenStorageTaxes().get("vault"));
        assertEquals(0.0D, section.getProtectionOpenStorageTaxes().get("raindrops"));
        assertEquals(0L, section.getProtectionFilledStorageTaxIntervalTicks());
        assertEquals(0, section.getProtectionFilledStorageMaximumFreeze());
        assertEquals(-1.0D, section.getProtectionFilledStorageMaximumDebtByCurrency().get("vault"));
        assertEquals(-1.0D, section.getProtectionFilledStorageMaximumDebtByCurrency().get("raindrops"));
        assertEquals(11.5D, section.getProtectionFilledStorageMaximumDebtByCurrency().get("gems"));
        assertEquals(0.0D, section.getProtectionFilledStorageTaxes().get("vault"));
        assertEquals(0.0D, section.getProtectionFilledStorageTaxes().get("raindrops"));
    }

    @Test
    void supportsLegacyStorageTaxesKeyForBackwardCompatibility(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            starting_storages: 1
            max_storages: 2
            max_hotkeys: 9
            protection:
              taxed_storages: "all"
              storage_taxes:
                vault: 8.0
                raindrops: 2.0
            """);

        final ConfigSection section = ConfigSection.fromFile(configFile.toFile());

        assertEquals(8.0D, section.getProtectionOpenStorageTaxes().get("vault"));
        assertEquals(2.0D, section.getProtectionOpenStorageTaxes().get("raindrops"));
    }

    @Test
    void readsTradeSettingsAndNormalizesInvalidValues(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            starting_storages: 1
            max_storages: 2
            max_hotkeys: 9
            trade:
              enabled: false
              invite_timeout_seconds: -1
              poll_interval_ticks: 40
              max_offer_slots: 99
              invite_cooldown_seconds: -10
            """);

        final ConfigSection section = ConfigSection.fromFile(configFile.toFile());

        assertFalse(section.isTradeEnabled());
        assertEquals(60L, section.getTradeInviteTimeoutSeconds());
        assertEquals(40L, section.getTradePollIntervalTicks());
        assertEquals(12, section.getTradeMaxOfferSlots());
        assertEquals(0L, section.getTradeInviteCooldownSeconds());
    }

    @Test
    void readsProxySettingsAndTradeProxyToggles(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            starting_storages: 1
            max_storages: 2
            max_hotkeys: 9
            proxy:
              enabled: true
              server_route_id: "alpha"
            trade:
              proxy:
                presence_enabled: true
                join_partner_action_enabled: true
            """);

        final ConfigSection section = ConfigSection.fromFile(configFile.toFile());

        assertTrue(section.isProxyEnabled());
        assertEquals("alpha", section.getProxyServerRouteId());
        assertTrue(section.isTradeProxyPresenceEnabled());
        assertTrue(section.isTradeProxyJoinActionEnabled());
    }

    @Test
    void parsesTradeTaxationSettingsAndNormalizesInvalidValues(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            starting_storages: 1
            max_storages: 2
            max_hotkeys: 9
            trade:
              taxation:
                enabled: true
                currencies:
                  vault:
                    mode: GROWTH
                    flat_amount: 10.0
                    growth_per_currency_amount: 0.05
                    growth_per_item: 2.5
                  raindrops:
                    mode: FLAT
                    flat_amount: -2.0
                  tokens:
                    mode: "UNKNOWN"
                    flat_amount: 3
                    growth_rate_currency_amount: 0.1
                    growth_rate_item_count: 0.2
            """);

        final ConfigSection section = ConfigSection.fromFile(configFile.toFile());

        assertTrue(section.isTradeTaxationEnabled());
        assertEquals(ConfigSection.TradeTaxMode.GROWTH, section.getTradeTaxationCurrencies().get("vault").mode());
        assertEquals(10.0D, section.getTradeTaxationCurrencies().get("vault").flatAmount());
        assertEquals(0.05D, section.getTradeTaxationCurrencies().get("vault").growthPerCurrencyAmount());
        assertEquals(2.5D, section.getTradeTaxationCurrencies().get("vault").growthPerItem());
        assertEquals(ConfigSection.TradeTaxMode.FLAT, section.getTradeTaxationCurrencies().get("raindrops").mode());
        assertEquals(0.0D, section.getTradeTaxationCurrencies().get("raindrops").flatAmount());
        assertEquals(ConfigSection.TradeTaxMode.FLAT, section.getTradeTaxationCurrencies().get("tokens").mode());
        assertEquals(3.0D, section.getTradeTaxationCurrencies().get("tokens").flatAmount());
        assertEquals(0.1D, section.getTradeTaxationCurrencies().get("tokens").growthPerCurrencyAmount());
        assertEquals(0.2D, section.getTradeTaxationCurrencies().get("tokens").growthPerItem());
    }

    @Test
    void returnsNoRequirementsForUndefinedPurchaseTiers(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            starting_storages: 1
            max_storages: 4
            max_hotkeys: 9
            requirements:
              "1":
                vault_purchase:
                  type: "CURRENCY"
                  icon:
                    type: "DIAMOND"
                  currency: vault
                  amount: 1125.0
            """);

        final ConfigSection section = ConfigSection.fromFile(configFile.toFile());

        assertEquals(1, section.getRequirements().size());
        assertTrue(section.getRequirements().containsKey(1));
        assertTrue(section.getRequirementsForPurchase(2).isEmpty());
        assertEquals("CURRENCY", section.getRequirementsForPurchase(1).get("vault_purchase").getType());
        assertEquals("DIAMOND", section.getRequirementsForPurchase(1).get("vault_purchase").getIcon().getType());
        assertEquals(1125.0D, getDouble(section.getRequirementsForPurchase(1).get("vault_purchase").toRequirementMap(), "amount"));
        assertEquals(List.of(2, 3), section.getMissingRequirementPurchases());
    }

    @Test
    void logsMissingPurchaseRequirementWarnings(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            starting_storages: 1
            max_storages: 4
            max_hotkeys: 9
            warn_missing_requirements: true
            requirements:
              "1":
                vault_purchase:
                  type: "CURRENCY"
                  icon:
                    type: "DIAMOND"
                  currency: vault
                  amount: 1125.0
            """);

        final ConfigSection section = ConfigSection.fromFile(configFile.toFile());
        final Logger logger = Logger.getLogger("ConfigSectionTest");
        logger.setUseParentHandlers(false);
        final CapturingHandler handler = new CapturingHandler();
        logger.addHandler(handler);

        section.logMissingRequirementWarnings(logger);

        logger.removeHandler(handler);
        assertEquals(1, handler.messages.size());
        assertTrue(handler.messages.getFirst().contains("2, 3"));
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
