package com.raindropcentral.rdq.bounty.claim;

import com.raindropcentral.rdq.bounty.tracking.DamageTracker;
import com.raindropcentral.rdq.bounty.type.ClaimMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Handles bounty claim attribution based on configured claim modes.
 * This class determines which player(s) should receive bounty rewards.
 */
public class ClaimHandler {
    
    private final DamageTracker damageTracker;
    private final ClaimMode claimMode;
    
    /**
     * Creates a new ClaimHandler.
     *
     * @param damageTracker the damage tracker instance
     * @param claimMode the claim mode to use
     */
    public ClaimHandler(@NotNull DamageTracker damageTracker, @NotNull ClaimMode claimMode) {
        this.damageTracker = Objects.requireNonNull(damageTracker, "damageTracker cannot be null");
        this.claimMode = Objects.requireNonNull(claimMode, "claimMode cannot be null");
    }
    
    /**
     * Determines the winner(s) of a bounty based on the configured claim mode.
     *
     * @param victimUuid the UUID of the bounty target who was killed
     * @param lastHitterUuid the UUID of the player who dealt the final blow (may be null for environmental deaths)
     * @return a ClaimResult containing the winner(s) and their reward proportions
     */
    @NotNull
    public ClaimResult determineClaim(@NotNull UUID victimUuid, @Nullable UUID lastHitterUuid) {
        Objects.requireNonNull(victimUuid, "victimUuid cannot be null");
        
        Map<UUID, Double> damageMap = damageTracker.getDamageMap(victimUuid);

        if (lastHitterUuid == null || lastHitterUuid.equals(victimUuid)) {
            return handleEdgeCase(damageMap);
        }
        
        return switch (claimMode) {
            case LAST_HIT -> handleLastHit(lastHitterUuid);
            case MOST_DAMAGE -> handleMostDamage(damageMap, lastHitterUuid);
            case DAMAGE_SPLIT -> handleDamageSplit(damageMap);
        };
    }
    
    /**
     * Handles LAST_HIT claim mode.
     * The player who dealt the final blow receives the full bounty.
     */
    @NotNull
    private ClaimResult handleLastHit(@NotNull UUID lastHitterUuid) {
        return new ClaimResult(Map.of(lastHitterUuid, 1.0));
    }
    
    /**
     * Handles MOST_DAMAGE claim mode.
     * The player who dealt the most damage within the tracking window receives the bounty.
     * In case of a tie, the first attacker wins.
     */
    @NotNull
    private ClaimResult handleMostDamage(@NotNull Map<UUID, Double> damageMap, @NotNull UUID lastHitterUuid) {
        if (damageMap.isEmpty()) {
            return handleLastHit(lastHitterUuid);
        }

        UUID winner = damageMap.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(lastHitterUuid);
        
        return new ClaimResult(Map.of(winner, 1.0));
    }
    
    /**
     * Handles DAMAGE_SPLIT claim mode.
     * The bounty is split proportionally among all damage dealers.
     */
    @NotNull
    private ClaimResult handleDamageSplit(@NotNull Map<UUID, Double> damageMap) {
        if (damageMap.isEmpty()) {
            return new ClaimResult(Collections.emptyMap());
        }
        
        double totalDamage = damageMap.values().stream()
            .mapToDouble(Double::doubleValue)
            .sum();
        
        if (totalDamage <= 0) {
            return new ClaimResult(Collections.emptyMap());
        }

        Map<UUID, Double> shares = new HashMap<>();
        for (Map.Entry<UUID, Double> entry : damageMap.entrySet()) {
            double proportion = entry.getValue() / totalDamage;
            shares.put(entry.getKey(), proportion);
        }
        
        return new ClaimResult(shares);
    }
    
    /**
     * Handles edge cases like environmental death or suicide.
     * Awards the bounty to the player who dealt the most damage, if any.
     */
    @NotNull
    private ClaimResult handleEdgeCase(@NotNull Map<UUID, Double> damageMap) {
        if (damageMap.isEmpty()) {
            return new ClaimResult(Collections.emptyMap());
        }

        UUID winner = damageMap.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
        
        if (winner == null) {
            return new ClaimResult(Collections.emptyMap());
        }
        
        return new ClaimResult(Map.of(winner, 1.0));
    }
    
    /**
     * Clears damage tracking for a victim after claim processing.
     *
     * @param victimUuid the UUID of the victim
     */
    public void clearDamage(@NotNull UUID victimUuid) {
        damageTracker.clearDamage(victimUuid);
    }
}
