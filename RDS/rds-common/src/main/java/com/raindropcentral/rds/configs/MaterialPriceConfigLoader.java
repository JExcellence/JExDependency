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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Loads the nested material-price map stored in {@code material-prices.yml}.
 *
 * <p>The bundled defaults file is large enough that repeatedly traversing Bukkit's YAML tree can
 * become expensive during startup on Folia. This loader parses the narrow
 * {@code materials -> material -> currency -> price} structure directly so RDS can rebuild dynamic
 * pricing caches without blocking the watchdog.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class MaterialPriceConfigLoader {

    private static final String MATERIALS_SECTION = "materials";

    private MaterialPriceConfigLoader() {
    }

    /**
     * Loads configured material prices from disk.
     *
     * <p>The parser accepts the generated default layout as well as common hand edits such as quoted
     * numeric values and inline comments. Invalid prices are ignored so callers can continue using
     * remaining valid material entries.</p>
     *
     * @param materialPricesFile source file to read
     * @return immutable material-to-currency price map, or an empty map when the file is missing or
     * unreadable
     */
    public static @NotNull Map<String, Map<String, Double>> loadMaterialPrices(
            final @NotNull File materialPricesFile
    ) {
        if (!materialPricesFile.exists() || !materialPricesFile.isFile()) {
            return Map.of();
        }

        final Map<String, Map<String, Double>> materialPrices = new LinkedHashMap<>();
        boolean insideMaterialsSection = false;
        @Nullable String currentMaterialKey = null;
        int currentMaterialIndent = -1;
        Map<String, Double> currentCurrencyPrices = new LinkedHashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(
                materialPricesFile.toPath(),
                StandardCharsets.UTF_8
        )) {
            String rawLine;
            while ((rawLine = reader.readLine()) != null) {
                final ParsedLine parsedLine = ParsedLine.parse(rawLine);
                if (parsedLine == null) {
                    continue;
                }

                if (!insideMaterialsSection) {
                    if (parsedLine.indent() == 0
                            && parsedLine.value() == null
                            && MATERIALS_SECTION.equalsIgnoreCase(parsedLine.key())) {
                        insideMaterialsSection = true;
                    }
                    continue;
                }

                if (parsedLine.indent() == 0) {
                    storeMaterialEntry(materialPrices, currentMaterialKey, currentCurrencyPrices);
                    break;
                }

                if (parsedLine.value() == null) {
                    if (currentMaterialKey != null && parsedLine.indent() > currentMaterialIndent) {
                        continue;
                    }

                    storeMaterialEntry(materialPrices, currentMaterialKey, currentCurrencyPrices);
                    currentMaterialKey = normalizeMaterialKey(parsedLine.key());
                    currentMaterialIndent = parsedLine.indent();
                    currentCurrencyPrices = new LinkedHashMap<>();
                    continue;
                }

                if (currentMaterialKey == null || parsedLine.indent() <= currentMaterialIndent) {
                    continue;
                }

                final Double parsedPrice = parsePrice(parsedLine.value());
                if (parsedPrice == null || !Double.isFinite(parsedPrice) || parsedPrice < 0D) {
                    continue;
                }

                currentCurrencyPrices.put(normalizeCurrencyType(parsedLine.key()), parsedPrice);
            }
        } catch (IOException ignored) {
            return Map.of();
        }

        storeMaterialEntry(materialPrices, currentMaterialKey, currentCurrencyPrices);
        return materialPrices.isEmpty() ? Map.of() : Map.copyOf(materialPrices);
    }

    private static void storeMaterialEntry(
            final @NotNull Map<String, Map<String, Double>> materialPrices,
            final @Nullable String materialKey,
            final @NotNull Map<String, Double> currencyPrices
    ) {
        if (materialKey == null || materialKey.isBlank() || currencyPrices.isEmpty()) {
            return;
        }

        materialPrices.put(materialKey, Map.copyOf(currencyPrices));
    }

    private static @Nullable Double parsePrice(
            final @Nullable String rawValue
    ) {
        if (rawValue == null) {
            return null;
        }

        final String normalized = unquote(rawValue).trim();
        if (normalized.isEmpty()) {
            return null;
        }

        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static @NotNull String normalizeMaterialKey(
            final @NotNull String materialKey
    ) {
        return unquote(materialKey).trim().toUpperCase(Locale.ROOT);
    }

    private static @NotNull String normalizeCurrencyType(
            final @NotNull String currencyType
    ) {
        final String normalized = unquote(currencyType).trim();
        if (normalized.isEmpty()) {
            return "vault";
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private static @NotNull String stripInlineComment(
            final @NotNull String line
    ) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int index = 0; index < line.length(); index++) {
            final char character = line.charAt(index);
            if (character == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (character == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            if (character == '#' && !inSingleQuote && !inDoubleQuote) {
                return line.substring(0, index);
            }
        }

        return line;
    }

    private static int findUnquotedDelimiter(
            final @NotNull String text,
            final char delimiter
    ) {
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int index = 0; index < text.length(); index++) {
            final char character = text.charAt(index);
            if (character == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }
            if (character == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }
            if (character == delimiter && !inSingleQuote && !inDoubleQuote) {
                return index;
            }
        }

        return -1;
    }

    private static @NotNull String unquote(
            final @NotNull String text
    ) {
        final String trimmed = text.trim();
        if (trimmed.length() < 2) {
            return trimmed;
        }

        final char first = trimmed.charAt(0);
        final char last = trimmed.charAt(trimmed.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return trimmed.substring(1, trimmed.length() - 1);
        }

        return trimmed;
    }

    private record ParsedLine(
            int indent,
            @NotNull String key,
            @Nullable String value
    ) {

        private static @Nullable ParsedLine parse(
                final @NotNull String rawLine
        ) {
            final String withoutComment = stripInlineComment(rawLine);
            if (withoutComment.isBlank()) {
                return null;
            }

            final int indent = countLeadingWhitespace(withoutComment);
            final String trimmed = withoutComment.trim();
            final int separatorIndex = findUnquotedDelimiter(trimmed, ':');
            if (separatorIndex < 0) {
                return null;
            }

            final String key = unquote(trimmed.substring(0, separatorIndex).trim());
            if (key.isBlank()) {
                return null;
            }

            final String rawValue = trimmed.substring(separatorIndex + 1).trim();
            final String value = rawValue.isEmpty() ? null : rawValue;
            return new ParsedLine(indent, key, value);
        }

        private static int countLeadingWhitespace(
                final @NotNull String value
        ) {
            int count = 0;
            while (count < value.length() && Character.isWhitespace(value.charAt(count))) {
                count++;
            }
            return count;
        }
    }
}
