package com.raindropcentral.rds.configs;

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

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
                vault:
                  initial_cost: 250.0
                  growth_rate: 1.5
                  maximum_tax: 5000.0
                coins:
                  initial_cost: 10.0
                  growth_rate: 1.25
                  maximum_tax: 1000.0
            """);

        final ConfigSection section = ConfigSection.fromFile(configFile.toFile());
        assertTrue(section.getProtection().isOnlyPlayerShops());
        assertTrue(section.getProtection().isShopTaxesFallbackToPlayer());
        assertTrue(section.getProtection().isShopTaxCurrency("vault"));
        assertTrue(section.getProtection().isShopTaxCurrency("coins"));
        assertFalse(section.getProtection().isShopTaxCurrency("gems"));
        assertEquals(250.0D, section.getProtection().getShopTaxCurrency("vault").getInitialCost());
        assertEquals(10.0D, section.getProtection().getShopTaxCurrency("coins").getInitialCost());
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
