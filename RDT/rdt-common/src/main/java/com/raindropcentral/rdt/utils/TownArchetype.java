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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Enumerates the supported town archetypes used for town-wide modifiers.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public enum TownArchetype {
    CAPITALIST,
    COMMUNIST,
    SOCIALIST,
    THEOCRACY,
    MONARCHY;

    /**
     * Returns a player-friendly archetype name.
     *
     * @return display label for this archetype
     */
    public @NotNull String getDisplayName() {
        final String lowerCase = this.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lowerCase.charAt(0)) + lowerCase.substring(1);
    }

    /**
     * Resolves an archetype from a nullable raw input string.
     *
     * @param rawValue raw config or user value
     * @return matching archetype, or {@code null} when no archetype matches
     */
    public static @Nullable TownArchetype fromString(final @Nullable String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            return TownArchetype.valueOf(rawValue.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ignored) {
            return null;
        }
    }
}
