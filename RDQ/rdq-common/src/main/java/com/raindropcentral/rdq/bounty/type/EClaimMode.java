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

package com.raindropcentral.rdq.bounty.type;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Map;
import java.util.UUID;

/**
 * Defines how bounty kills are attributed to hunters.
 */
public enum EClaimMode {
    /**
     * The player who dealt the final blow receives the full bounty.
     */
    LAST_HIT {
        /**
         * Executes determineWinner.
         */
        @Override
        public OfflinePlayer determineWinner(Map<UUID, Double> damageMap, OfflinePlayer lastHitter) {
            return lastHitter;
        }
    },
    
    /**
     * The player who dealt the most damage within the tracking window receives the bounty.
     */
    MOST_DAMAGE {
        /**
         * Executes determineWinner.
         */
        @Override
        public OfflinePlayer determineWinner(Map<UUID, Double> damageMap, OfflinePlayer lastHitter) {
            return damageMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).map(Bukkit::getOfflinePlayer)
                .orElse(lastHitter);
        }
    },
    
    /**
     * The bounty is split proportionally among all damage dealers.
     */
    DAMAGE_SPLIT {
        /**
         * Executes determineWinner.
         */
        @Override
        public OfflinePlayer determineWinner(Map<UUID, Double> damageMap, OfflinePlayer lastHitter) {
            // For DAMAGE_SPLIT, we return null to indicate multiple winners
            // The actual distribution logic is handled separately
            return null;
        }
    };
    
    /**
     * Determines the winner of a bounty based on damage dealt.
     *
     * @param damageMap Map of player UUIDs to damage dealt
     * @param lastHitter UUID of the player who dealt the final blow
     * @return UUID of the winning player, or null for DAMAGE_SPLIT mode
     */
    public abstract OfflinePlayer determineWinner(Map<UUID, Double> damageMap, OfflinePlayer lastHitter);

    /**
     * Executes of.
     */
    public static EClaimMode of(String value) {
        try  {
            return EClaimMode.valueOf(value);
        } catch (IllegalArgumentException e) {
            return EClaimMode.LAST_HIT;
        }
    }
}
