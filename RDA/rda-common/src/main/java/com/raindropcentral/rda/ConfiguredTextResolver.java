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

package com.raindropcentral.rda;

import de.jexcellence.jextranslate.i18n.I18n;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Resolves config-backed text that may come from either a translation key or a literal fallback.
 *
 * @author Codex
 * @since 1.2.0
 * @version 1.2.0
 */
public final class ConfiguredTextResolver {

    private ConfiguredTextResolver() {
    }

    /**
     * Resolves a plain-text value from an optional translation key with a literal fallback.
     *
     * @param player player whose locale should be used
     * @param translationKey optional translation key
     * @param fallbackText literal fallback used when no translation key is configured
     * @return resolved plain-text value
     */
    public static @NotNull String resolvePlainText(
        final @NotNull Player player,
        final @Nullable String translationKey,
        final @Nullable String fallbackText
    ) {
        Objects.requireNonNull(player, "player");

        final String trimmedKey = translationKey == null ? "" : translationKey.trim();
        if (!trimmedKey.isBlank()) {
            final String resolved = PlainTextComponentSerializer.plainText().serialize(
                new I18n.Builder(trimmedKey, player).build().component()
            );
            if (!resolved.equals(trimmedKey)) {
                return resolved;
            }
        }

        return fallbackText == null ? "" : fallbackText;
    }
}
