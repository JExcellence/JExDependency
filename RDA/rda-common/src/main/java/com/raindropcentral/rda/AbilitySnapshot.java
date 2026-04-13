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

package com.raindropcentral.rda;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Immutable ability snapshot used by the stat and skill menus.
 *
 * @param skillType owning skill
 * @param abilityDefinition configured ability definition
 * @param unlocked whether the ability has at least one unlocked tier
 * @param currentTierIndex zero-based unlocked tier index, or {@code -1} when locked
 * @param currentTier current unlocked tier, or {@code null} when locked
 * @param potency resolved ability potency
 * @param primaryStatPoints spent points in the primary stat
 * @param secondaryStatPoints spent points in the secondary stat
 * @param nextRequiredSkillLevel next skill-level gate, or {@code 0} when fully unlocked
 * @param nextRequiredStatPoints next stat gate, or {@code 0} when fully unlocked
 */
public record AbilitySnapshot(
    @NotNull SkillType skillType,
    @NotNull SkillConfig.AbilityDefinition abilityDefinition,
    boolean unlocked,
    int currentTierIndex,
    @Nullable SkillConfig.AbilityTierDefinition currentTier,
    double potency,
    int primaryStatPoints,
    int secondaryStatPoints,
    int nextRequiredSkillLevel,
    int nextRequiredStatPoints
) {

    /**
     * Creates an ability snapshot.
     */
    public AbilitySnapshot {
        Objects.requireNonNull(skillType, "skillType");
        Objects.requireNonNull(abilityDefinition, "abilityDefinition");
    }

    /**
     * Returns the display-friendly tier number.
     *
     * @return one-based tier number, or {@code 0} when locked
     */
    public int displayTier() {
        return this.currentTierIndex < 0 ? 0 : this.currentTierIndex + 1;
    }
}
