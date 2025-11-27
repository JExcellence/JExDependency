package com.raindropcentral.rdq.bounty.type;

import java.util.Map;
import java.util.UUID;

/**
 * Defines how bounty kills are attributed to hunters.
 */
public enum ClaimMode {
    /**
     * The player who dealt the final blow receives the full bounty.
     */
    LAST_HIT {
        @Override
        public UUID determineWinner(Map<UUID, Double> damageMap, UUID lastHitter) {
            return lastHitter;
        }
    },
    
    /**
     * The player who dealt the most damage within the tracking window receives the bounty.
     */
    MOST_DAMAGE {
        @Override
        public UUID determineWinner(Map<UUID, Double> damageMap, UUID lastHitter) {
            return damageMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(lastHitter);
        }
    },
    
    /**
     * The bounty is split proportionally among all damage dealers.
     */
    DAMAGE_SPLIT {
        @Override
        public UUID determineWinner(Map<UUID, Double> damageMap, UUID lastHitter) {
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
    public abstract UUID determineWinner(Map<UUID, Double> damageMap, UUID lastHitter);
}
