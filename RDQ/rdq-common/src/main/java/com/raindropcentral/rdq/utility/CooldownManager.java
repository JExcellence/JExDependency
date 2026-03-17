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

package com.raindropcentral.rdq.utility;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for managing cooldowns for arbitrary keys (such as player UUIDs or location identifiers).
 *
 * <p>This class provides thread-safe methods to check, set, and clear cooldowns for unique keys,
 * allowing you to prevent repeated actions within a specified time window.
 *
 * <ul>
 *   <li>Use {@link #isOnCooldown(String, long)} to check if a key is still on cooldown.</li>
 *   <li>Use {@link #resetCooldown(String)} to start or reset a cooldown for a key.</li>
 *   <li>Use {@link #getRemainingCooldown(String, long)} to get the remaining cooldown time.</li>
 *   <li>Use {@link #clearCooldown(String)} to remove a cooldown entry.</li>
 * </ul>
 *
 *
 * <p>Example usage: Preventing a player from performing an action more than once every 5 seconds.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
public class CooldownManager {

    /**
     * Map storing the last-used timestamp (in ms) for each key.
 *
 * <p>The key can be any unique identifier, such as a player UUID or a location string.
     */
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    /**
     * Checks if the specified key is currently on cooldown.
     *
     * @param key            The unique identifier (e.g., player UUID, location key).
     * @param cooldownMillis The cooldown duration in milliseconds.
     * @return {@code true} if the key is still on cooldown; {@code false} if the cooldown has expired or was never set.
     */
    public boolean isOnCooldown(String key, long cooldownMillis) {
        Long lastUsed = cooldowns.get(key);
        if (lastUsed == null) return false;
        return (System.currentTimeMillis() - lastUsed) < cooldownMillis;
    }

    /**
     * Gets the remaining cooldown time in milliseconds for the specified key.
     *
     * @param key            The unique identifier.
     * @param cooldownMillis The cooldown duration in milliseconds.
     * @return The remaining cooldown time in milliseconds, or 0 if not on cooldown.
     */
    public long getRemainingCooldown(String key, long cooldownMillis) {
        Long lastUsed = cooldowns.get(key);
        if (lastUsed == null) return 0;
        long elapsed = System.currentTimeMillis() - lastUsed;
        long remaining = cooldownMillis - elapsed;
        return Math.max(0, remaining);
    }

    /**
     * Resets or starts the cooldown for the given key.
 *
 * <p>Sets the last-used timestamp to the current system time.
     *
     * @param key The unique identifier.
     */
    public void resetCooldown(String key) {
        cooldowns.put(key, System.currentTimeMillis());
    }

    /**
     * Removes the cooldown entry for the given key.
     *
     * @param key The unique identifier.
     */
    public void clearCooldown(String key) {
        cooldowns.remove(key);
    }
}
