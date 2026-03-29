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

package com.raindropcentral.core.service.central.cookie;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Registry of supported droplet cookie definitions.
 */
public final class DropletCookieDefinitions {

    public static final long DEFAULT_DURATION_SECONDS = 3_600L;

    private static final Map<String, DropletCookieDefinition> DEFINITIONS = createDefinitions();

    private DropletCookieDefinitions() {
    }

    /**
     * Returns every supported droplet-store item code in registration order.
     *
     * @return immutable list of supported item codes
     */
    public static @NotNull List<String> allItemCodes() {
        return List.copyOf(DEFINITIONS.keySet());
    }

    /**
     * Resolves a cookie definition by store item code.
     *
     * @param itemCode backend item code supplied by the droplet-store API
     * @return matching definition, or {@code null} when unsupported
     */
    public static @Nullable DropletCookieDefinition get(final @Nullable String itemCode) {
        if (itemCode == null || itemCode.isBlank()) {
            return null;
        }
        return DEFINITIONS.get(normalize(itemCode));
    }

    private static @NotNull Map<String, DropletCookieDefinition> createDefinitions() {
        final Map<String, DropletCookieDefinition> definitions = new LinkedHashMap<>();
        register(definitions, "skill-level-cookie", DropletCookieTargetType.SKILL, DropletCookieEffectType.SKILL_LEVEL, 0.0D, 0L);
        register(definitions, "skill-xp-rate-10-cookie", DropletCookieTargetType.SKILL, DropletCookieEffectType.SKILL_XP_RATE, 0.10D, DEFAULT_DURATION_SECONDS);
        register(definitions, "skill-xp-rate-50-cookie", DropletCookieTargetType.SKILL, DropletCookieEffectType.SKILL_XP_RATE, 0.50D, DEFAULT_DURATION_SECONDS);
        register(definitions, "skill-xp-rate-100-cookie", DropletCookieTargetType.SKILL, DropletCookieEffectType.SKILL_XP_RATE, 1.00D, DEFAULT_DURATION_SECONDS);
        register(definitions, "job-level-cookie", DropletCookieTargetType.JOB, DropletCookieEffectType.JOB_LEVEL, 0.0D, 0L);
        register(definitions, "job-xp-rate-10-cookie", DropletCookieTargetType.JOB, DropletCookieEffectType.JOB_XP_RATE, 0.10D, DEFAULT_DURATION_SECONDS);
        register(definitions, "job-xp-rate-50-cookie", DropletCookieTargetType.JOB, DropletCookieEffectType.JOB_XP_RATE, 0.50D, DEFAULT_DURATION_SECONDS);
        register(definitions, "job-xp-rate-100-cookie", DropletCookieTargetType.JOB, DropletCookieEffectType.JOB_XP_RATE, 1.00D, DEFAULT_DURATION_SECONDS);
        register(definitions, "job-vault-rate-10-cookie", DropletCookieTargetType.JOB, DropletCookieEffectType.JOB_VAULT_RATE, 0.10D, DEFAULT_DURATION_SECONDS);
        register(definitions, "job-vault-rate-50-cookie", DropletCookieTargetType.JOB, DropletCookieEffectType.JOB_VAULT_RATE, 0.50D, DEFAULT_DURATION_SECONDS);
        register(definitions, "job-vault-rate-100-cookie", DropletCookieTargetType.JOB, DropletCookieEffectType.JOB_VAULT_RATE, 1.00D, DEFAULT_DURATION_SECONDS);
        register(definitions, "double-drop-rate-cookie", DropletCookieTargetType.NONE, DropletCookieEffectType.DOUBLE_DROP_RATE, 1.00D, DEFAULT_DURATION_SECONDS);
        return Map.copyOf(definitions);
    }

    private static void register(
            final @NotNull Map<String, DropletCookieDefinition> definitions,
            final @NotNull String itemCode,
            final @NotNull DropletCookieTargetType targetType,
            final @NotNull DropletCookieEffectType effectType,
            final double rateBonus,
            final long durationSeconds
    ) {
        definitions.put(normalize(itemCode), new DropletCookieDefinition(itemCode, targetType, effectType, rateBonus, durationSeconds));
    }

    private static @NotNull String normalize(final @NotNull String itemCode) {
        return itemCode.trim().toLowerCase(Locale.ROOT);
    }
}
