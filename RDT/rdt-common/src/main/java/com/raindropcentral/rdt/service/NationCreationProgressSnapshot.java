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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Immutable live snapshot of the nation-creation requirement flow.
 *
 * @param available whether nation creation can currently be used
 * @param alreadyInNation whether the viewing town already belongs to an active nation
 * @param pendingNation whether the viewing town is already involved in a pending nation flow
 * @param readyToCreate whether every configured nation creation requirement is complete
 * @param progress overall average nation creation progress
 * @param requirements nation creation requirement snapshots
 * @param rewards nation creation reward previews
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public record NationCreationProgressSnapshot(
    boolean available,
    boolean alreadyInNation,
    boolean pendingNation,
    boolean readyToCreate,
    double progress,
    @NotNull List<TownLevelRequirementSnapshot> requirements,
    @NotNull List<TownLevelRewardSnapshot> rewards
) {

    /**
     * Creates an immutable nation-creation progress snapshot.
     */
    public NationCreationProgressSnapshot {
        progress = Math.max(0.0D, Math.min(1.0D, progress));
        requirements = List.copyOf(requirements);
        rewards = List.copyOf(rewards);
    }

    /**
     * Returns the requirement entry matching the supplied key.
     *
     * @param entryKey requirement entry key
     * @return matching requirement snapshot, or {@code null} when absent
     */
    public @Nullable TownLevelRequirementSnapshot findRequirement(final @Nullable String entryKey) {
        if (entryKey == null || entryKey.isBlank()) {
            return null;
        }
        return this.requirements.stream()
            .filter(requirement -> Objects.equals(requirement.entryKey(), entryKey))
            .findFirst()
            .orElse(null);
    }
}
