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

package com.raindropcentral.core.service.statistics.vanilla.privacy;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-player privacy preferences for vanilla statistics collection.
 *
 * <p>This manager tracks which players have opted out of statistics collection
 * and provides methods to check opt-out status before collecting statistics.
 *
 * <p>Opt-out preferences are stored in memory and can be persisted to disk
 * for persistence across server restarts.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * PlayerPrivacyManager privacyManager = new PlayerPrivacyManager();
 * 
 * // Check if player has opted out
 * if (privacyManager.hasOptedOut(playerId)) {
 *     // Skip statistics collection
 *     return;
 * }
 * 
 * // Player opts out
 * privacyManager.setOptOut(playerId, true);
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class PlayerPrivacyManager {

    private final Map<UUID, Boolean> optOutFlags;

    /**
     * Creates a new player privacy manager.
     */
    public PlayerPrivacyManager() {
        this.optOutFlags = new ConcurrentHashMap<>();
    }

    /**
     * Checks if a player has opted out of statistics collection.
     *
     * @param playerId the UUID of the player to check
     * @return true if the player has opted out, false otherwise
     */
    public boolean hasOptedOut(final @NotNull UUID playerId) {
        return optOutFlags.getOrDefault(playerId, false);
    }

    /**
     * Sets the opt-out status for a player.
     *
     * @param playerId the UUID of the player
     * @param optedOut true to opt out, false to opt in
     */
    public void setOptOut(final @NotNull UUID playerId, final boolean optedOut) {
        if (optedOut) {
            optOutFlags.put(playerId, true);
        } else {
            optOutFlags.remove(playerId);
        }
    }

    /**
     * Removes opt-out status for a player (opts them back in).
     *
     * @param playerId the UUID of the player
     */
    public void clearOptOut(final @NotNull UUID playerId) {
        optOutFlags.remove(playerId);
    }

    /**
     * Gets the number of players who have opted out.
     *
     * @return the count of opted-out players
     */
    public int getOptOutCount() {
        return optOutFlags.size();
    }

    /**
     * Clears all opt-out preferences.
     */
    public void clearAll() {
        optOutFlags.clear();
    }

    /**
     * Exports opt-out preferences for persistence.
     *
     * @return a map of player UUIDs to opt-out status
     */
    public @NotNull Map<UUID, Boolean> exportOptOuts() {
        return Map.copyOf(optOutFlags);
    }

    /**
     * Imports opt-out preferences from a persisted state.
     *
     * @param optOuts the map of player UUIDs to opt-out status
     */
    public void importOptOuts(final @NotNull Map<UUID, Boolean> optOuts) {
        optOutFlags.clear();
        optOuts.forEach((uuid, opted) -> {
            if (opted) {
                optOutFlags.put(uuid, true);
            }
        });
    }
}
