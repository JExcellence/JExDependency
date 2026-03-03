/*
 * ConfigSectionTest.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rdr.configs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
