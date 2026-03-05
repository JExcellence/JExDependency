package com.raindropcentral.rds.service.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Provides type detection and parsing helpers for editable config values.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class ShopConfigEditSupport {

    /**
     * Supported editable config value types.
     */
    public enum SettingType {
        STRING,
        BOOLEAN,
        INTEGER,
        LONG,
        DOUBLE,
        LIST
    }

    private ShopConfigEditSupport() {
    }

    /**
     * Resolves the editable setting type for a config value.
     *
     * @param value raw config value
     * @return detected setting type
     */
    public static @NotNull SettingType determineType(
            final @Nullable Object value
    ) {
        if (value instanceof Boolean) {
            return SettingType.BOOLEAN;
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer) {
            return SettingType.INTEGER;
        }
        if (value instanceof Long) {
            return SettingType.LONG;
        }
        if (value instanceof Float || value instanceof Double) {
            return SettingType.DOUBLE;
        }
        if (value instanceof List<?>) {
            return SettingType.LIST;
        }
        return SettingType.STRING;
    }

    /**
     * Indicates whether the provided config value can be edited via the config UI.
     *
     * @param value raw config value
     * @return {@code true} when editable
     */
    public static boolean isEditableValue(
            final @Nullable Object value
    ) {
        return value == null
                || value instanceof String
                || value instanceof Boolean
                || value instanceof Number
                || value instanceof List<?>;
    }

    /**
     * Parses an input string to the requested setting type.
     *
     * @param input raw anvil input
     * @param settingType target setting type
     * @return parsed typed value
     * @throws IllegalArgumentException when input cannot be parsed for the type
     */
    public static @NotNull Object parseInput(
            final @NotNull String input,
            final @NotNull SettingType settingType
    ) {
        final String trimmed = input.trim();

        return switch (settingType) {
            case STRING -> input;
            case BOOLEAN -> parseBoolean(trimmed);
            case INTEGER -> parseInteger(trimmed);
            case LONG -> parseLong(trimmed);
            case DOUBLE -> parseDouble(trimmed);
            case LIST -> parseList(input);
        };
    }

    /**
     * Formats a config value for display in UI placeholders.
     *
     * @param value raw config value
     * @return formatted value string
     */
    public static @NotNull String formatValue(
            final @Nullable Object value
    ) {
        if (value == null) {
            return "";
        }

        if (value instanceof List<?> listValue) {
            final List<String> parts = new ArrayList<>();
            for (final Object part : listValue) {
                if (part == null) {
                    continue;
                }
                parts.add(String.valueOf(part));
            }
            return String.join(", ", parts);
        }

        return String.valueOf(value);
    }

    private static @NotNull Boolean parseBoolean(
            final @NotNull String input
    ) {
        final String normalized = input.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "true", "yes", "y", "on", "1" -> true;
            case "false", "no", "n", "off", "0" -> false;
            default -> throw new IllegalArgumentException("Expected a boolean value");
        };
    }

    private static @NotNull Integer parseInteger(
            final @NotNull String input
    ) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Expected a whole number", exception);
        }
    }

    private static @NotNull Long parseLong(
            final @NotNull String input
    ) {
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Expected a long whole number", exception);
        }
    }

    private static @NotNull Double parseDouble(
            final @NotNull String input
    ) {
        try {
            return Double.parseDouble(input);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Expected a decimal number", exception);
        }
    }

    private static @NotNull List<String> parseList(
            final @NotNull String input
    ) {
        final List<String> parsed = new ArrayList<>();
        if (input.isBlank()) {
            return parsed;
        }

        final String[] split = input.split(",");
        for (final String value : split) {
            final String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            parsed.add(trimmed);
        }
        return parsed;
    }
}
