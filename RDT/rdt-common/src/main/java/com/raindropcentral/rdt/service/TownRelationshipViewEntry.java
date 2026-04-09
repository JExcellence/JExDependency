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

package com.raindropcentral.rdt.service;

import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.utils.TownRelationshipState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * View-model snapshot for one source-town and target-town diplomacy pairing.
 *
 * @param sourceTown viewing town
 * @param targetTown related target town
 * @param confirmedState stored confirmed diplomacy state
 * @param effectiveState currently effective diplomacy state after unlock rules are applied
 * @param pendingState pending requested diplomacy state, or {@code null} when none exists
 * @param pendingRequesterTownUuid town UUID that created the pending request, or {@code null} when
 *     none exists
 * @param cooldownRemainingMillis remaining cooldown in milliseconds before this pair may change
 *     again
 * @param sourceUnlocked whether the viewing town has unlocked diplomacy
 * @param targetUnlocked whether the target town has unlocked diplomacy
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public record TownRelationshipViewEntry(
    @NotNull RTown sourceTown,
    @NotNull RTown targetTown,
    @NotNull TownRelationshipState confirmedState,
    @NotNull TownRelationshipState effectiveState,
    @Nullable TownRelationshipState pendingState,
    @Nullable UUID pendingRequesterTownUuid,
    long cooldownRemainingMillis,
    boolean sourceUnlocked,
    boolean targetUnlocked
) {

    /**
     * Creates an immutable relationship view entry.
     */
    public TownRelationshipViewEntry {
        Objects.requireNonNull(sourceTown, "sourceTown");
        Objects.requireNonNull(targetTown, "targetTown");
        Objects.requireNonNull(confirmedState, "confirmedState");
        Objects.requireNonNull(effectiveState, "effectiveState");
        cooldownRemainingMillis = Math.max(0L, cooldownRemainingMillis);
    }

    /**
     * Returns whether diplomacy is locked for this town pair because one side lacks the required
     * Nexus level.
     *
     * @return {@code true} when diplomacy is forced to neutral by unlock rules
     */
    public boolean lockedByLevel() {
        return !this.sourceUnlocked || !this.targetUnlocked;
    }

    /**
     * Returns whether a non-hostile confirmation request is currently pending.
     *
     * @return {@code true} when one town has a pending request awaiting confirmation
     */
    public boolean hasPendingRequest() {
        return this.pendingState != null && this.pendingRequesterTownUuid != null;
    }

    /**
     * Returns whether the viewing town created the current pending request.
     *
     * @return {@code true} when the viewing town initiated the pending request
     */
    public boolean pendingRequestedBySource() {
        return this.hasPendingRequest()
            && Objects.equals(this.pendingRequesterTownUuid, this.sourceTown.getTownUUID());
    }

    /**
     * Returns whether the target town created the current pending request.
     *
     * @return {@code true} when the target town initiated the pending request
     */
    public boolean pendingRequestedByTarget() {
        return this.hasPendingRequest()
            && Objects.equals(this.pendingRequesterTownUuid, this.targetTown.getTownUUID());
    }
}
