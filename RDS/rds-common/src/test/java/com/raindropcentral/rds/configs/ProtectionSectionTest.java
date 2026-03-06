package com.raindropcentral.rds.configs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests protection section parsing and normalization.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class ProtectionSectionTest {

    @Test
    void returnsDefaultsWhenSectionIsMissing(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, "taxes: {}\n");

        final ProtectionSection section = ProtectionSection.fromFile(configFile.toFile());
        assertFalse(section.isOnlyPlayerShops());
        assertFalse(section.isShopTaxesFallbackToPlayer());
        assertFalse(section.hasShopTaxes());
        assertTrue(section.getShopTaxes().isEmpty());
        assertFalse(section.isShopTaxCurrency("vault"));
    }

    @Test
    void normalizesConfiguredTaxCurrencies(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            protection:
              only_player_shops: true
              shop_taxes_fallback_to_player: true
              shop_taxes:
                Vault:
                  initial_cost: 150.0
                  growth_rate: 1.05
                  maximum_tax: 2000.0
                Coins:
                  initial_cost: 7.5
                  growth_rate: 1.02
                  maximum_tax: 99999.0
            """);

        final ProtectionSection section = ProtectionSection.fromFile(configFile.toFile());
        assertTrue(section.isOnlyPlayerShops());
        assertTrue(section.isShopTaxesFallbackToPlayer());
        assertTrue(section.hasShopTaxes());
        assertTrue(section.getShopTaxes().containsKey("vault"));
        assertTrue(section.getShopTaxes().containsKey("coins"));
        assertEquals(2, section.getShopTaxes().size());
        assertNotNull(section.getShopTaxCurrency("vault"));
        assertNotNull(section.getShopTaxCurrency("coins"));
        assertEquals(150.0D, section.getShopTaxCurrency("vault").getInitialCost());
        assertEquals(7.5D, section.getShopTaxCurrency("coins").getInitialCost());
        assertTrue(section.isShopTaxCurrency("VaUlT"));
        assertTrue(section.isShopTaxCurrency("coins"));
        assertFalse(section.isShopTaxCurrency("gems"));
    }

    @Test
    void mapsLegacyCurrencyListToTaxesSection(final @TempDir Path tempDir) throws IOException {
        final Path configFile = tempDir.resolve("config.yml");
        Files.writeString(configFile, """
            taxes:
              vault:
                initial_cost: 100.0
                growth_rate: 1.125
                maximum_tax: -1
              raindrops:
                initial_cost: 5.0
                growth_rate: 1.075
                maximum_tax: 50000
            protection:
              shop_taxes:
                - vault
                - raindrops
            """);

        final ProtectionSection section = ProtectionSection.fromFile(configFile.toFile());
        assertFalse(section.isShopTaxesFallbackToPlayer());
        assertTrue(section.hasShopTaxes());
        assertNotNull(section.getShopTaxCurrency("vault"));
        assertNotNull(section.getShopTaxCurrency("raindrops"));
        assertEquals(100.0D, section.getShopTaxCurrency("vault").getInitialCost());
        assertEquals(5.0D, section.getShopTaxCurrency("raindrops").getInitialCost());
    }
}
