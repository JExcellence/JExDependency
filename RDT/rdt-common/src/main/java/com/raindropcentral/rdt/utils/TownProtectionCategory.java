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
 * Classifies town protections by the type of editor state they support in the GUI.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public enum TownProtectionCategory {
    ROLE_BASED("role_based"),
    BINARY_TOGGLE("binary_toggle");

    private final String translationKey;

    TownProtectionCategory(final @NotNull String translationKey) {
        this.translationKey = translationKey;
    }

    /**
     * Returns the translation key suffix for this category.
     *
     * @return translation key suffix
     */
    public @NotNull String getTranslationKey() {
        return this.translationKey;
    }

    /**
     * Resolves a category enum from a raw stored key.
     *
     * @param categoryKey raw category key
     * @return matching category enum, or {@code null} when no category matches
     */
    public static @Nullable TownProtectionCategory fromKey(final @Nullable String categoryKey) {
        if (categoryKey == null || categoryKey.isBlank()) {
            return null;
        }

        try {
            return TownProtectionCategory.valueOf(categoryKey.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ignored) {
            return null;
        }
    }
}
