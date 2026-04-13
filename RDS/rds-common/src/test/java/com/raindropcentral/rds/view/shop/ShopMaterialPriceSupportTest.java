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

package com.raindropcentral.rds.view.shop;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests material-based default item price resolution.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class ShopMaterialPriceSupportTest {

    @Test
    void loadsMultipleCurrenciesPerMaterial(final @TempDir Path tempDir) throws IOException {
        final Path materialPricesFile = tempDir.resolve("material-prices.yml");
        Files.writeString(materialPricesFile, """
            materials:
              STONE:
                vault: 1.0
                raindrops: 0.25
              COBBLESTONE:
                vault: 0.8
            """);

        final Map<String, Map<String, Double>> prices = ShopMaterialPriceSupport.loadMaterialPrices(
                materialPricesFile.toFile()
        );

        assertEquals(2, prices.size());
        assertEquals(1.0D, prices.get("STONE").get("vault"));
        assertEquals(0.25D, prices.get("STONE").get("raindrops"));
        assertEquals(0.8D, prices.get("COBBLESTONE").get("vault"));
    }

    @Test
    void resolvesConfiguredPriceForDefaultCurrency() {
        final Map<String, Map<String, Double>> materialPrices = Map.of(
                "STONE", Map.of(
                        "vault", 2.0D,
                        "raindrops", 0.5D
                )
        );

        final ShopMaterialPriceSupport.MaterialPrice price = ShopMaterialPriceSupport.resolveDefaultPrice(
                Material.STONE,
                materialPrices,
                "vault",
                10.0D,
                List.of()
        );

        assertEquals("vault", price.currencyType());
        assertEquals(2.0D, price.value());
    }

    @Test
    void fallsBackToFirstNonBlacklistedConfiguredCurrencyWhenDefaultIsMissing() {
        final Map<String, Map<String, Double>> materialPrices = Map.of(
                "STONE", Map.of(
                        "raindrops", 0.5D,
                        "tokens", 0.25D
                )
        );

        final ShopMaterialPriceSupport.MaterialPrice price = ShopMaterialPriceSupport.resolveDefaultPrice(
                Material.STONE,
                materialPrices,
                "vault",
                10.0D,
                List.of("raindrops")
        );

        assertEquals("tokens", price.currencyType());
        assertEquals(0.25D, price.value());
    }

    @Test
    void usesFallbackGlobalPriceWhenMaterialIsMissing() {
        final ShopMaterialPriceSupport.MaterialPrice price = ShopMaterialPriceSupport.resolveDefaultPrice(
                Material.DIAMOND,
                Map.of(),
                "vault",
                15.0D,
                List.of()
        );

        assertEquals("vault", price.currencyType());
        assertEquals(15.0D, price.value());
    }

    @Test
    void ignoresInvalidConfiguredPrices(final @TempDir Path tempDir) throws IOException {
        final Path materialPricesFile = tempDir.resolve("material-prices.yml");
        Files.writeString(materialPricesFile, """
            materials:
              STONE:
                vault: -1
                broken: nope
              COBBLESTONE:
                vault: 1.5
            """);

        final Map<String, Map<String, Double>> prices = ShopMaterialPriceSupport.loadMaterialPrices(
                materialPricesFile.toFile()
        );

        assertTrue(prices.containsKey("COBBLESTONE"));
        assertFalse(prices.containsKey("STONE"));
    }

    @Test
    void loadsQuotedValuesAndIgnoresInlineComments(final @TempDir Path tempDir) throws IOException {
        final Path materialPricesFile = tempDir.resolve("material-prices.yml");
        Files.writeString(materialPricesFile, """
            # Top-level comment
            materials:
              "STONE":
                vault: "1.0" # default shop currency
                raindrops: '0.25'
              DIAMOND:
                tokens: 2.5
            ignored_section:
              should_not_load: 999
            """);

        final Map<String, Map<String, Double>> prices = ShopMaterialPriceSupport.loadMaterialPrices(
                materialPricesFile.toFile()
        );

        assertEquals(2, prices.size());
        assertEquals(1.0D, prices.get("STONE").get("vault"));
        assertEquals(0.25D, prices.get("STONE").get("raindrops"));
        assertEquals(2.5D, prices.get("DIAMOND").get("tokens"));
        assertFalse(prices.containsKey("IGNORED_SECTION"));
    }
}
