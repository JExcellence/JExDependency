package com.raindropcentral.rdq.bounty.claim;

import com.raindropcentral.rdq.bounty.DamageTracker;
import com.raindropcentral.rdq.bounty.type.EClaimMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Handles bounty claim attribution based on configured claim modes.
 * Determines which player(s) should receive bounty rewards.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class ClaimHandler {

    private final DamageTracker damageTracker;
    private final EClaimMode claimMode;

    public ClaimHandler(@NotNull DamageTracker damageTracker, @NotNull EClaimMode claimMode) {
        this.damageTracker = damageTracker;
        this.claimMode = claimMode;
    }

    /**
     * Determines the winner(s) of a bounty based on the configured claim mode.
     *
     * @param victimUuid the UUID of the bounty target who was killed
     * @param lastHitterUuid the UUID of the player who dealt the final blow (may be null)
     * @return a ClaimResult containing the winner(s) and their reward proportions
     */
    @NotNull
    public ClaimResult determineClaim(@NotNull UUID victimUuid, @Nullable UUID lastHitterUuid) {
        Map<UUID, Double> damageMap = damageTracker.getDamageMap(victimUuid);

        // Handle edge cases (environmental death, suicide)
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
     * LAST_HIT mode: The player who dealt the final blow receives the full bounty.
     */
    @NotNull
    private ClaimResult handleLastHit(@NotNull UUID lastHitterUuid) {
        return ClaimResult.singleWinner(lastHitterUuid);
    }

    /**
     * MOST_DAMAGE mode: The player who dealt the most damage receives the bounty.
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

        return ClaimResult.singleWinner(winner);
    }

    /**
     * DAMAGE_SPLIT mode: The bounty is split proportionally among all damage dealers.
     */
    @NotNull
    private ClaimResult handleDamageSplit(@NotNull Map<UUID, Double> damageMap) {
        if (damageMap.isEmpty()) {
            return ClaimResult.empty();
        }

        double totalDamage = damageMap.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        if (totalDamage <= 0) {
            return ClaimResult.empty();
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
            return ClaimResult.empty();
        }

        UUID winner = damageMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (winner == null) {
            return ClaimResult.empty();
        }

        return ClaimResult.singleWinner(winner);
    }

    /**
     * Clears damage tracking for a victim after claim processing.
     */
    public void clearDamage(@NotNull UUID victimUuid) {
        damageTracker.clearDamage(victimUuid);
    }
}
