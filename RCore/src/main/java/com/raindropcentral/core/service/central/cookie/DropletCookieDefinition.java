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

package com.raindropcentral.core.service.central.cookie;

import org.jetbrains.annotations.NotNull;

/**
 * Static cookie definition resolved from a droplet-store item code.
 *
 * @param itemCode backend item code
 * @param targetType required target kind
 * @param effectType effect applied when redeemed
 * @param rateBonus bonus percentage expressed as a decimal, for example {@code 0.5} for +50%
 * @param durationSeconds boost duration in seconds for timed cookies; {@code 0} for instant grants
 */
public record DropletCookieDefinition(
        @NotNull String itemCode,
        @NotNull DropletCookieTargetType targetType,
        @NotNull DropletCookieEffectType effectType,
        double rateBonus,
        long durationSeconds
) {

    public boolean isTimedBoost() {
        return this.effectType.isTimedBoost();
    }

    public boolean isSkillTargeted() {
        return this.targetType == DropletCookieTargetType.SKILL;
    }

    public boolean isJobTargeted() {
        return this.targetType == DropletCookieTargetType.JOB;
    }

    public boolean activatesDirectly() {
        return this.targetType == DropletCookieTargetType.NONE;
    }

    public int ratePercent() {
        return (int) Math.round(this.rateBonus * 100.0D);
    }

    public long durationMinutes() {
        return Math.max(0L, this.durationSeconds / 60L);
    }

    public @NotNull String itemType() {
        return this.itemCode;
    }
}
