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
 * Immutable preview of the build reset caused by a skill prestige.
 *
 * @param skillType prestiged skill
 * @param resetPoints total allocated points that will be reset for reallocation
 * @param earnedPointsAfterPrestige total entitled points after the skill reset
 * @param unspentPointsAfterPrestige resulting unspent points after the build reset
 */
public record PrestigeAdjustmentPreview(
    @NotNull SkillType skillType,
    int resetPoints,
    int earnedPointsAfterPrestige,
    int unspentPointsAfterPrestige
) {

    /**
     * Creates a prestige-adjustment preview.
     */
    public PrestigeAdjustmentPreview {
        Objects.requireNonNull(skillType, "skillType");
    }

    /**
     * Returns the total allocated point count that will reset.
     *
     * @return total allocated point count that will reset
     */
    public int getTotalResetPoints() {
        return Math.max(0, this.resetPoints);
    }
}
