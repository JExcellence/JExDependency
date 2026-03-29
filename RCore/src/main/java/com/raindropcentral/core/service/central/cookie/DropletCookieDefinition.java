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

    /**
     * Returns whether redeeming the cookie activates a timed runtime boost.
     *
     * @return {@code true} for boost-style cookies persisted through player statistics
     */
    public boolean isTimedBoost() {
        return this.effectType.isTimedBoost();
    }

    /**
     * Returns whether the cookie requires a skill selection before activation.
     *
     * @return {@code true} when the cookie targets a skill bridge entry
     */
    public boolean isSkillTargeted() {
        return this.targetType == DropletCookieTargetType.SKILL;
    }

    /**
     * Returns whether the cookie requires a job selection before activation.
     *
     * @return {@code true} when the cookie targets a job bridge entry
     */
    public boolean isJobTargeted() {
        return this.targetType == DropletCookieTargetType.JOB;
    }

    /**
     * Returns whether the cookie can be activated immediately without a target picker.
     *
     * @return {@code true} when no skill or job selection is required
     */
    public boolean activatesDirectly() {
        return this.targetType == DropletCookieTargetType.NONE;
    }

    /**
     * Converts the decimal bonus into a whole-number percentage for UI messages.
     *
     * @return rounded percentage value, such as {@code 50} for {@code 0.5}
     */
    public int ratePercent() {
        return (int) Math.round(this.rateBonus * 100.0D);
    }

    /**
     * Returns the boost duration in whole minutes for UI presentation.
     *
     * @return non-negative duration rounded down to minutes
     */
    public long durationMinutes() {
        return Math.max(0L, this.durationSeconds / 60L);
    }

    /**
     * Returns the persistent item type identifier written into cookie item metadata.
     *
     * @return store item code backing this definition
     */
    public @NotNull String itemType() {
        return this.itemCode;
    }
}
