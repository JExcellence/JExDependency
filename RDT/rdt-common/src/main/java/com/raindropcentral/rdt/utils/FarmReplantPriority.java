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
 * Enumerates the configured seed-consumption priority used by Farm auto-replanting.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public enum FarmReplantPriority {
    INVENTORY_FIRST,
    SEED_BOX_FIRST;

    /**
     * Parses one configured priority value and falls back when the input is blank or invalid.
     *
     * @param rawPriority raw configured priority value
     * @param fallback fallback priority
     * @return parsed priority, or {@code fallback} when invalid
     */
    public static @NotNull FarmReplantPriority fromValue(
        final @Nullable String rawPriority,
        final @NotNull FarmReplantPriority fallback
    ) {
        if (rawPriority == null || rawPriority.isBlank()) {
            return fallback;
        }
        try {
            return FarmReplantPriority.valueOf(rawPriority.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
