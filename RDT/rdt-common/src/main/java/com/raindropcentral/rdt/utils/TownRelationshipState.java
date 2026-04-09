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
 * Enumerates the confirmed and requested diplomacy states between two towns.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public enum TownRelationshipState {
    NEUTRAL("neutral"),
    ALLIED("allied"),
    HOSTILE("hostile");

    private final String translationKey;

    TownRelationshipState(final @NotNull String translationKey) {
        this.translationKey = translationKey;
    }

    /**
     * Returns the translation key suffix for this relationship state.
     *
     * @return translation key suffix
     */
    public @NotNull String getTranslationKey() {
        return this.translationKey;
    }

    /**
     * Resolves a relationship state from a stored key.
     *
     * @param rawKey stored key to parse
     * @return matching state, or {@code null} when the key does not match any value
     */
    public static @Nullable TownRelationshipState fromKey(final @Nullable String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return null;
        }

        try {
            return TownRelationshipState.valueOf(rawKey.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ignored) {
            return null;
        }
    }
}
