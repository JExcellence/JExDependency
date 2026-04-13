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

import java.util.Objects;

/**
 * Immutable skill profile snapshot tailored for GUI rendering.
 *
 * @param skillType owning skill type
 * @param progressionEnabled whether the skill is enabled
 * @param internalLevel stored internal level
 * @param displayLevel displayed level
 * @param overlevel post-cap overlevel count
 * @param displayLevelText formatted level text used by the GUI
 * @param currentLevelXp stored XP progress inside the current level
 * @param xpToNextLevel XP needed to reach the next internal level, or {@code 0} when capped
 * @param prestige completed prestige count
 * @param maxPrestiges configured prestige cap
 * @param prestigeAvailable whether the player can manually prestige now
 * @param maxPrestigeReached whether the configured prestige cap has been reached
 * @param prestigeSymbols rendered prestige symbol string
 * @param totalPower derived total power contribution
 * @param currentXpBonusPercent total XP bonus percent from prestige
 * @param softMaxLevel configured soft cap
 *
 * @author Codex
 * @since 1.1.0
 * @version 1.1.0
 */
public record SkillProfileSnapshot(
    @NotNull SkillType skillType,
    boolean progressionEnabled,
    int internalLevel,
    int displayLevel,
    int overlevel,
    @NotNull String displayLevelText,
    long currentLevelXp,
    long xpToNextLevel,
    int prestige,
    int maxPrestiges,
    boolean prestigeAvailable,
    boolean maxPrestigeReached,
    @NotNull String prestigeSymbols,
    long totalPower,
    int currentXpBonusPercent,
    int softMaxLevel
) {

    /**
     * Creates a skill profile snapshot.
     */
    public SkillProfileSnapshot {
        Objects.requireNonNull(skillType, "skillType");
        Objects.requireNonNull(displayLevelText, "displayLevelText");
        Objects.requireNonNull(prestigeSymbols, "prestigeSymbols");
    }

    /**
     * Creates an empty snapshot for an unavailable skill runtime.
     *
     * @param skillType owning skill type
     * @return empty snapshot
     */
    public static @NotNull SkillProfileSnapshot empty(final @NotNull SkillType skillType) {
        return new SkillProfileSnapshot(
            Objects.requireNonNull(skillType, "skillType"),
            false,
            0,
            0,
            0,
            "0",
            0L,
            0L,
            0,
            0,
            false,
            false,
            "-",
            0L,
            0,
            0
        );
    }
}
