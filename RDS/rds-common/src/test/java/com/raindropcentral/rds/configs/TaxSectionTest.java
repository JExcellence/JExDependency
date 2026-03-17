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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests tax section parsing and defaults.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class TaxSectionTest {

    @Test
    void usesDefaultNeverItemPenaltyRateWhenUnset() {
        final TaxSection section = TaxSection.createDefault("vault");

        assertEquals(0.25D, section.getNeverItemPenaltyRate(), 1.0E-9D);
        assertEquals(-1D, section.getMaximumBankruptcyAmount("vault"), 1.0E-9D);
        assertEquals(-1D, section.getMaximumBankruptcyAmount("raindrops"), 1.0E-9D);
        assertTrue(section.getMaximumBankruptcyAmounts().containsKey("vault"));
        assertTrue(section.getMaximumBankruptcyAmounts().containsKey("raindrops"));
        assertEquals(-1D, section.getMaximumBankruptcyAmount(), 1.0E-9D);
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

    @Test
    void readsMaximumBankruptcyAmountByCurrencyFromConfig(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            taxes:
              vault:
                initial_cost: 100.0
                growth_rate: 1.125
                maximum_tax: -1
              maximum_bankruptcy_amount:
                vault: 2500.0
                raindrops: 500.0
            """);

        final TaxSection section = TaxSection.fromFile(configFile.toFile(), "vault");
        assertEquals(2500.0D, section.getMaximumBankruptcyAmount("vault"), 1.0E-9D);
        assertEquals(500.0D, section.getMaximumBankruptcyAmount("raindrops"), 1.0E-9D);
        assertEquals(-1.0D, section.getMaximumBankruptcyAmount("coins"), 1.0E-9D);
    }

    @Test
    void treatsNonPositiveMaximumBankruptcyAmountAsUnlimited(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            taxes:
              vault:
                initial_cost: 100.0
                growth_rate: 1.125
                maximum_tax: -1
              maximum_bankruptcy_amount:
                vault: 0.0
                raindrops: -10.0
            """);

        final TaxSection section = TaxSection.fromFile(configFile.toFile(), "vault");
        assertEquals(-1.0D, section.getMaximumBankruptcyAmount("vault"), 1.0E-9D);
        assertEquals(-1.0D, section.getMaximumBankruptcyAmount("raindrops"), 1.0E-9D);
        assertTrue(section.getMaximumBankruptcyAmount("vault") < 0D);
    }

    @Test
    void supportsLegacyGlobalMaximumBankruptcyAmount(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            taxes:
              vault:
                initial_cost: 100.0
                growth_rate: 1.125
                maximum_tax: -1
              maximum_bankruptcy_amount: 2500.0
            """);

        final TaxSection section = TaxSection.fromFile(configFile.toFile(), "vault");
        assertEquals(2500.0D, section.getMaximumBankruptcyAmount("vault"), 1.0E-9D);
        assertEquals(2500.0D, section.getMaximumBankruptcyAmount("raindrops"), 1.0E-9D);
        assertEquals(2500.0D, section.getMaximumBankruptcyAmount("coins"), 1.0E-9D);
    }
}
