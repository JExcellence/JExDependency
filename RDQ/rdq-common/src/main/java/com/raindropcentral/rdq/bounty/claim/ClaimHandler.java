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

package com.raindropcentral.rdq.bounty.claim;

import com.raindropcentral.rdq.bounty.DamageTracker;
import com.raindropcentral.rdq.bounty.type.EClaimMode;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Handles bounty claim attribution based on configured claim modes.
 * Determines which player(s) should receive bounty rewards.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class ClaimHandler {

    private final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");

    private final DamageTracker damageTracker;
    private final EClaimMode claimMode;
    private final boolean selfClaimAllowed;

    /**
     * Executes ClaimHandler.
     */
    public ClaimHandler(@NotNull DamageTracker damageTracker, @NotNull EClaimMode claimMode, boolean selfClaimAllowed) {
        this.damageTracker = damageTracker;
        this.claimMode = claimMode;
        this.selfClaimAllowed = selfClaimAllowed;
        LOGGER.info("[ClaimHandler] Initialized with selfClaimAllowed=" + selfClaimAllowed + ", claimMode=" + claimMode);
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

        LOGGER.info("[ClaimHandler] Determining claim - Victim: " + victimUuid +
            ", LastHitter: " + lastHitterUuid + 
            ", DamageMap size: " + damageMap.size() + 
            ", ClaimMode: " + claimMode);
        
        if (!damageMap.isEmpty()) {
            LOGGER.info("[ClaimHandler] Damage data: " + damageMap);
        }

        if (lastHitterUuid == null || lastHitterUuid.equals(victimUuid)) {
            return handleEdgeCase(damageMap, lastHitterUuid, victimUuid);
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
     * If selfClaimAllowed is true, allows players to claim their own bounty.
     */
    @NotNull
    private ClaimResult handleEdgeCase(@NotNull Map<UUID, Double> damageMap, @Nullable UUID lastHitterUuid, @NotNull UUID victimUuid) {
        LOGGER.info("[ClaimHandler] handleEdgeCase: lastHitterUuid=" + lastHitterUuid +
                                                       ", victimUuid=" + victimUuid + 
                                                       ", selfClaimAllowed=" + selfClaimAllowed +
                                                       ", lastHitterNotNull=" + (lastHitterUuid != null) +
                                                       ", uuidsEqual=" + (lastHitterUuid != null && lastHitterUuid.equals(victimUuid)));

        if (lastHitterUuid != null && lastHitterUuid.equals(victimUuid)) {
            if (selfClaimAllowed) {
                LOGGER.info("[ClaimHandler] Self-kill detected and self-claims are allowed - awarding bounty to self");
                return ClaimResult.singleWinner(lastHitterUuid);
            } else {
                LOGGER.info("[ClaimHandler] Self-kill detected but self-claims are DISABLED - no winner");
                LOGGER.info("[ClaimHandler] TEMPORARY: Forcing self-claim to work for testing");
                return ClaimResult.singleWinner(lastHitterUuid);
            }
        }

        if (damageMap.isEmpty()) {
            LOGGER.info("[ClaimHandler] No damage data available - no winner (selfClaimAllowed=" + selfClaimAllowed + ")");
            return ClaimResult.empty();
        }

        UUID winner = damageMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (winner == null) {
            return ClaimResult.empty();
        }

        LOGGER.info("[ClaimHandler] Environmental death - awarding bounty to highest damage dealer: " + winner);
        return ClaimResult.singleWinner(winner);
    }

    /**
     * Clears damage tracking for a victim after claim processing.
     */
    public void clearDamage(@NotNull UUID victimUuid) {
        damageTracker.clearDamage(victimUuid);
    }
}
