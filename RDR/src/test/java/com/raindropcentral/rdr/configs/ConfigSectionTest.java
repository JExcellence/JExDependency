/*
 * ConfigSectionTest.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rdr.configs;

import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigSectionTest {

    @Test
    void returnsExpectedDefaultsWhenValuesAreUnset() {
        final ConfigSection section = new ConfigSection(new EvaluationEnvironmentBuilder());

        assertEquals(1, section.getStartingStorages());
        assertEquals(1, section.getMaxStorages());
        assertEquals(9, section.getMaxHotkeys());
        assertEquals("vault", section.getDefaultCurrencyType());
        assertTrue(section.getStore().containsKey("vault"));
        assertEquals(1000.0D, section.getDefaultStoreCurrency().getInitialCost());
        assertEquals(1.125D, section.getDefaultStoreCurrency().getGrowthRate());
        assertEquals(1, section.getInitialProvisionedStorages());
    }

    @Test
    void readsMaxStoragesFromYamlFile(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            starting_storages: 1
            max_storages: 10
            max_hotkeys: 9
            store:
              vault:
                initial_cost: 1000.0
                growth_rate: 1.125
            """);

        final ConfigSection section = ConfigSection.fromFile(configFile.toFile());

        assertEquals(10, section.getMaxStorages());
    }

    @Test
    void readsMultipleStoreCurrenciesFromYamlFile(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            starting_storages: 1
            max_storages: 10
            max_hotkeys: 9
            store:
              vault:
                initial_cost: 1000.0
                growth_rate: 1.125
              coins:
                initial_cost: 50.0
                growth_rate: 1.075
            """);

        final ConfigSection section = ConfigSection.fromFile(configFile.toFile());

        assertEquals(2, section.getStore().size());
        assertTrue(section.getStore().containsKey("vault"));
        assertTrue(section.getStore().containsKey("coins"));
        assertEquals(50.0D, section.getStoreCurrency("coins").getInitialCost());
    }
}
