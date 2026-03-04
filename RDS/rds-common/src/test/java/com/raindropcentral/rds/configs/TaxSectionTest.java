/*
 * TaxSectionTest.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
 */

package com.raindropcentral.rds.configs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests tax section parsing and defaults.
 */
class TaxSectionTest {

    @Test
    void usesDefaultNeverItemPenaltyRateWhenUnset() {
        final TaxSection section = TaxSection.createDefault("vault");

        assertEquals(0.25D, section.getNeverItemPenaltyRate(), 1.0E-9D);
    }

    @Test
    void readsNeverItemPenaltyRateFromConfig(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            taxes:
              vault:
                initial_cost: 100.0
                growth_rate: 1.125
                maximum_tax: -1
              never_item_penalty_rate: 0.75
            """);

        final TaxSection section = TaxSection.fromFile(configFile.toFile(), "vault");
        assertEquals(0.75D, section.getNeverItemPenaltyRate(), 1.0E-9D);
    }

    @Test
    void clampsNegativeNeverItemPenaltyRateToZero(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            taxes:
              vault:
                initial_cost: 100.0
                growth_rate: 1.125
                maximum_tax: -1
              never_item_penalty_rate: -5.0
            """);

        final TaxSection section = TaxSection.fromFile(configFile.toFile(), "vault");
        assertEquals(0.0D, section.getNeverItemPenaltyRate(), 1.0E-9D);
    }
}
