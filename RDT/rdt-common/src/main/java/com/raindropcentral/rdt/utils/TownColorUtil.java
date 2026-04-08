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

package com.raindropcentral.rdt.utils;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Normalizes town color input and maps stored town colors to supported boss-bar colors.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownColorUtil {

    private static final Map<String, String> NAMED_COLORS = Map.ofEntries(
        Map.entry("BLACK", "#000000"),
        Map.entry("DARK_BLUE", "#0000AA"),
        Map.entry("DARK_GREEN", "#00AA00"),
        Map.entry("DARK_AQUA", "#00AAAA"),
        Map.entry("DARK_RED", "#AA0000"),
        Map.entry("DARK_PURPLE", "#AA00AA"),
        Map.entry("GOLD", "#FFAA00"),
        Map.entry("GRAY", "#AAAAAA"),
        Map.entry("GREY", "#AAAAAA"),
        Map.entry("DARK_GRAY", "#555555"),
        Map.entry("DARK_GREY", "#555555"),
        Map.entry("BLUE", "#5555FF"),
        Map.entry("LIGHT_BLUE", "#55FFFF"),
        Map.entry("GREEN", "#55FF55"),
        Map.entry("AQUA", "#55FFFF"),
        Map.entry("RED", "#FF5555"),
        Map.entry("LIGHT_RED", "#FF5555"),
        Map.entry("PINK", "#FF55FF"),
        Map.entry("LIGHT_PURPLE", "#FF55FF"),
        Map.entry("YELLOW", "#FFFF55"),
        Map.entry("WHITE", "#FFFFFF")
    );

    private TownColorUtil() {
    }

    /**
     * Parses player town-color input into a canonical {@code #RRGGBB} string.
     *
     * @param rawColor raw player input
     * @return canonical upper-case hex color string
     * @throws IllegalArgumentException if the color cannot be parsed
     */
    public static @NotNull String parseTownColor(final @Nullable String rawColor) {
        if (rawColor == null || rawColor.isBlank()) {
            throw new IllegalArgumentException("Town color cannot be blank.");
        }

        final String normalized = rawColor.trim().toUpperCase(Locale.ROOT);
        final String namedColor = NAMED_COLORS.get(normalized);
        if (namedColor != null) {
            return namedColor;
        }

        final String candidate = normalized.startsWith("#") ? normalized : "#" + normalized;
        if (!candidate.matches("^#[0-9A-F]{6}$")) {
            throw new IllegalArgumentException("Town color must be a named Minecraft color or #RRGGBB.");
        }
        return candidate;
    }

    /**
     * Returns an Adventure color for the supplied town-color string.
     *
     * @param colorHex canonical or raw town color
     * @return parsed Adventure color
     */
    public static @NotNull TextColor toTextColor(final @Nullable String colorHex) {
        final TextColor parsedColor = TextColor.fromHexString(parseTownColor(colorHex));
        return Objects.requireNonNullElseGet(parsedColor, () -> TextColor.color(0x55CDFC));
    }

    /**
     * Maps an arbitrary town color to the closest supported boss-bar color.
     *
     * @param colorHex canonical or raw town color
     * @return nearest supported boss-bar color
     */
    public static @NotNull BossBar.Color toBossBarColor(final @Nullable String colorHex) {
        final int rgb = toTextColor(colorHex).value();
        final int red = (rgb >> 16) & 0xFF;
        final int green = (rgb >> 8) & 0xFF;
        final int blue = rgb & 0xFF;

        if (red > 200 && green > 200 && blue < 120) {
            return BossBar.Color.YELLOW;
        }
        if (red > 200 && blue > 200) {
            return BossBar.Color.PINK;
        }
        if (red > 200 && green < 140 && blue < 140) {
            return BossBar.Color.RED;
        }
        if (green > 180 && red < 160) {
            return BossBar.Color.GREEN;
        }
        if (blue > 180 && red < 160) {
            return BossBar.Color.BLUE;
        }
        if (red > 170 && green > 120 && blue < 120) {
            return BossBar.Color.YELLOW;
        }
        if (red < 90 && green < 90 && blue < 90) {
            return BossBar.Color.WHITE;
        }
        return BossBar.Color.PURPLE;
    }
}
