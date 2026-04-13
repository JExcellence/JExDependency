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

import java.util.Locale;

/**
 * Player-selectable trigger modes for active skill abilities.
 *
 * @author Codex
 * @since 1.2.0
 * @version 1.2.0
 */
public enum ActivationMode {

    /**
     * Activates through {@code /ra cast <skill>}.
     */
    COMMAND,

    /**
     * Activates on an unsneaking left-click.
     */
    LEFT_CLICK,

    /**
     * Activates on a sneaking left-click.
     */
    SHIFT_LEFT_CLICK,

    /**
     * Activates on an unsneaking right-click.
     */
    RIGHT_CLICK,

    /**
     * Activates on a sneaking right-click.
     */
    SHIFT_RIGHT_CLICK;

    /**
     * Returns the translation key used for the localized label.
     *
     * @return localized trigger-mode translation key
     */
    public @NotNull String getTranslationKey() {
        return "ra_activation_modes." + this.name().toLowerCase(Locale.ROOT);
    }

    /**
     * Resolves the localized display label for this trigger mode.
     *
     * @param player player whose locale should be used
     * @return localized plain-text label
     */
    public @NotNull String getDisplayName(final @NotNull Player player) {
        return PlainTextComponentSerializer.plainText().serialize(
            new I18n.Builder(this.getTranslationKey(), player).build().component()
        );
    }
}
