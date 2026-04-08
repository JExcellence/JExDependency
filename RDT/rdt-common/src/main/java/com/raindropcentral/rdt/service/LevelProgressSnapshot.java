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
import java.util.UUID;

/**
 * Immutable live or preview snapshot of one level progression target.
 *
 * @param scope progression scope
 * @param available whether the scope currently supports leveling
 * @param townUuid owning town UUID
 * @param townName owning town name
 * @param chunkUuid target chunk UUID when scope is Security
 * @param worldName target chunk world name when scope is Security
 * @param chunkType target chunk type when scope is Security
 * @param chunkX target chunk X coordinate when scope is Security
 * @param chunkZ target chunk Z coordinate when scope is Security
 * @param currentLevel current persisted level
 * @param displayLevel level shown in the current view
 * @param sourceLevel configured source level for this displayed definition
 * @param maxLevel highest configured level for this scope
 * @param preview whether the snapshot is previewing a non-active level
 * @param maxLevelReached whether no higher configured level exists
 * @param completedLevel whether the displayed level has already been reached
 * @param readyToLevelUp whether the active target is ready to finalize
 * @param progress overall average progress for the displayed level
 * @param requirements requirement progression entries
 * @param rewards reward preview entries
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public record LevelProgressSnapshot(
    @NotNull LevelScope scope,
    boolean available,
    @NotNull UUID townUuid,
    @NotNull String townName,
    @Nullable UUID chunkUuid,
    @Nullable String worldName,
    @Nullable String chunkType,
    int chunkX,
    int chunkZ,
    int currentLevel,
    int displayLevel,
    int sourceLevel,
    int maxLevel,
    boolean preview,
    boolean maxLevelReached,
    boolean completedLevel,
    boolean readyToLevelUp,
    double progress,
    @NotNull List<TownLevelRequirementSnapshot> requirements,
    @NotNull List<TownLevelRewardSnapshot> rewards
) {

    /**
     * Creates an immutable level-progress snapshot.
     *
     * @param scope progression scope
     * @param available whether the scope currently supports leveling
     * @param townUuid owning town UUID
     * @param townName owning town name
     * @param chunkUuid target chunk UUID when scope is Security
     * @param worldName target chunk world name when scope is Security
     * @param chunkType target chunk type when scope is Security
     * @param chunkX target chunk X coordinate when scope is Security
     * @param chunkZ target chunk Z coordinate when scope is Security
     * @param currentLevel current persisted level
     * @param displayLevel level shown in the current view
     * @param sourceLevel configured source level for this displayed definition
     * @param maxLevel highest configured level for this scope
     * @param preview whether the snapshot is previewing a non-active level
     * @param maxLevelReached whether no higher configured level exists
     * @param completedLevel whether the displayed level has already been reached
     * @param readyToLevelUp whether the active target is ready to finalize
     * @param progress overall average progress for the displayed level
     * @param requirements requirement progression entries
     * @param rewards reward preview entries
     */
    public LevelProgressSnapshot {
        scope = Objects.requireNonNull(scope, "scope");
        townUuid = Objects.requireNonNull(townUuid, "townUuid");
        townName = Objects.requireNonNull(townName, "townName");
        currentLevel = Math.max(1, currentLevel);
        displayLevel = Math.max(1, displayLevel);
        sourceLevel = Math.max(1, sourceLevel);
        maxLevel = Math.max(1, maxLevel);
        progress = Math.max(0.0D, Math.min(1.0D, progress));
        requirements = List.copyOf(requirements);
        rewards = List.copyOf(rewards);
    }

    /**
     * Returns the requirement entry matching the supplied key.
     *
     * @param entryKey entry key to resolve
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
