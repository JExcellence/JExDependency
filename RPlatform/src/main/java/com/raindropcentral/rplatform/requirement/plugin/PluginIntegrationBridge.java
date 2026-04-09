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

package com.raindropcentral.rplatform.requirement.plugin;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generic bridge contract used by {@code PLUGIN} requirements.
 *
 * <p>Implementations expose a key/value requirement surface over third-party plugins while keeping
 * runtime dependencies optional. Built-in categories include skills, jobs, and town progression.</p>
 *
 * @author ItsRainingHP, JExcellence
 * @since 2.0.0
 * @version 1.0.0
 */
public interface PluginIntegrationBridge {

    /**
     * Gets the normalized integration identifier used in configs.
     *
     * @return lower-case integration identifier
     */
    @NotNull String getIntegrationId();

    /**
     * Gets the plugin name used for availability checks.
     *
     * @return external plugin name
     */
    @NotNull String getPluginName();

    /**
     * Gets the high-level integration category.
     *
     * @return category identifier (for example {@code "SKILLS"}, {@code "JOBS"}, or {@code "TOWNS"})
     */
    @NotNull String getCategory();

    /**
     * Checks whether the bridged integration is available.
     *
     * @return {@code true} when values can be queried
     */
    boolean isAvailable();

    /**
     * Gets a single numeric value from the integration.
     *
     * @param player player whose values are being queried
     * @param key key identifier (for example skill or job name)
     * @return resolved value, or {@code 0} when unavailable
     * @throws NullPointerException if {@code player} or {@code key} is {@code null}
     */
    double getValue(@NotNull Player player, @NotNull String key);

    /**
     * Gets multiple numeric values from the integration.
     *
     * @param player player whose values are being queried
     * @param keys keys to resolve
     * @return map of keys to resolved values
     * @throws NullPointerException if {@code player} or {@code keys} is {@code null}
     */
    default @NotNull Map<String, Double> getValues(@NotNull Player player, @NotNull String... keys) {
        final Map<String, Double> values = new LinkedHashMap<>();
        for (final String key : keys) {
            if (key == null || key.isBlank()) {
                continue;
            }
            values.put(key, getValue(player, key));
        }
        return values;
    }

    /**
     * Consumes a numeric value from the integration.
     *
     * <p>Most integrations are read-only and should return {@code false} unless a safe write API
     * is explicitly supported.</p>
     *
     * @param player player whose value should be consumed
     * @param key value key to consume
     * @param amount amount to consume
     * @return {@code true} when the consumption operation succeeds
     * @throws NullPointerException if {@code player} or {@code key} is {@code null}
     */
    default boolean consume(@NotNull Player player, @NotNull String key, double amount) {
        return false;
    }
}
