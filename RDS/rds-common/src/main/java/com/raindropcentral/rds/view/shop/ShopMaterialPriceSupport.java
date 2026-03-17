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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves material-based default shop item pricing from {@code material-prices.yml}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
final class ShopMaterialPriceSupport {

    private static final String MATERIALS_SECTION = "materials";

    private ShopMaterialPriceSupport() {
    }

    static @NotNull Map<String, Map<String, Double>> loadMaterialPrices(
            final @NotNull File materialPricesFile
    ) {
        if (!materialPricesFile.exists()) {
            return Map.of();
        }

        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(materialPricesFile);
        final ConfigurationSection materialsSection = configuration.getConfigurationSection(MATERIALS_SECTION);
        if (materialsSection == null) {
            return Map.of();
        }

        final Map<String, Map<String, Double>> materialPrices = new LinkedHashMap<>();
        for (final String materialKey : materialsSection.getKeys(false)) {
            if (materialKey == null || materialKey.isBlank()) {
                continue;
            }

            final ConfigurationSection materialSection = materialsSection.getConfigurationSection(materialKey);
            if (materialSection == null) {
                continue;
            }

            final Map<String, Double> currencyPrices = new LinkedHashMap<>();
            for (final String currencyKey : materialSection.getKeys(false)) {
                if (currencyKey == null || currencyKey.isBlank()) {
                    continue;
                }

                final Double parsedPrice = parsePrice(materialSection.get(currencyKey));
                if (parsedPrice == null || !Double.isFinite(parsedPrice) || parsedPrice < 0D) {
                    continue;
                }

                currencyPrices.put(normalizeCurrencyType(currencyKey), parsedPrice);
            }

            if (!currencyPrices.isEmpty()) {
                materialPrices.put(materialKey.trim().toUpperCase(Locale.ROOT), Map.copyOf(currencyPrices));
            }
        }

        return Map.copyOf(materialPrices);
    }

    static @NotNull MaterialPrice resolveDefaultPrice(
            final @NotNull Material material,
            final @NotNull Map<String, Map<String, Double>> materialPrices,
            final @NotNull String defaultCurrencyType,
            final double fallbackPrice,
            final @NotNull List<String> blacklistedCurrencies
    ) {
        final String normalizedDefaultCurrency = normalizeCurrencyType(defaultCurrencyType);
        final String normalizedMaterial = material.name().toUpperCase(Locale.ROOT);
        final Map<String, Double> configuredPrices = materialPrices.get(normalizedMaterial);

        if (configuredPrices != null && !configuredPrices.isEmpty()) {
            final Double defaultCurrencyPrice = configuredPrices.get(normalizedDefaultCurrency);
            if (isUsablePrice(defaultCurrencyPrice)
                    && !containsIgnoreCase(blacklistedCurrencies, normalizedDefaultCurrency)) {
                return new MaterialPrice(normalizedDefaultCurrency, defaultCurrencyPrice);
            }

            for (final Map.Entry<String, Double> entry : configuredPrices.entrySet()) {
                final String currencyType = normalizeCurrencyType(entry.getKey());
                final Double configuredPrice = entry.getValue();
                if (!isUsablePrice(configuredPrice) || containsIgnoreCase(blacklistedCurrencies, currencyType)) {
                    continue;
                }

                return new MaterialPrice(currencyType, configuredPrice);
            }
        }

        return new MaterialPrice(normalizedDefaultCurrency, sanitizeFallbackPrice(fallbackPrice));
    }

    private static @Nullable Double parsePrice(
            final @Nullable Object rawValue
    ) {
        if (rawValue instanceof Number number) {
            return number.doubleValue();
        }
        if (rawValue instanceof String rawText) {
            final String trimmed = rawText.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            try {
                return Double.parseDouble(trimmed);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static @NotNull String normalizeCurrencyType(
            final @Nullable String currencyType
    ) {
        if (currencyType == null || currencyType.isBlank()) {
            return "vault";
        }
        return currencyType.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean containsIgnoreCase(
            final @NotNull List<String> values,
            final @NotNull String target
    ) {
        for (final String value : values) {
            if (value != null && value.equalsIgnoreCase(target)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUsablePrice(
            final @Nullable Double price
    ) {
        return price != null && Double.isFinite(price) && price >= 0D;
    }

    private static double sanitizeFallbackPrice(
            final double fallbackPrice
    ) {
        return !Double.isFinite(fallbackPrice) || fallbackPrice < 0D ? 0D : fallbackPrice;
    }

    record MaterialPrice(
            @NotNull String currencyType,
            double value
    ) {
    }
}
