package com.raindropcentral.rdq.type;

import java.util.Map;
import java.util.UUID;

public enum EBountyClaimMode {
    MOST_DAMAGE {
        @Override
        public UUID determineWinner(Map<UUID, Double> damageMap, UUID lastHitter) {
            return damageMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        }
    },
    LAST_HIT {
        @Override
        public UUID determineWinner(Map<UUID, Double> damageMap, UUID lastHitter) {
            return lastHitter;
        }
    };

    public abstract UUID determineWinner(Map<UUID, Double> damageMap, UUID lastHitter);
}
