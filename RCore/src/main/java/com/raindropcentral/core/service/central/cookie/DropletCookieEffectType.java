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

import com.raindropcentral.rplatform.cookie.CookieBoostType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Effect types supported by droplet cookies.
 */
public enum DropletCookieEffectType {
    SKILL_LEVEL(null),
    JOB_LEVEL(null),
    SKILL_XP_RATE(CookieBoostType.SKILL_XP),
    JOB_XP_RATE(CookieBoostType.JOB_XP),
    JOB_VAULT_RATE(CookieBoostType.JOB_VAULT),
    DOUBLE_DROP_RATE(CookieBoostType.DOUBLE_DROP);

    private final CookieBoostType boostType;

    DropletCookieEffectType(final @Nullable CookieBoostType boostType) {
        this.boostType = boostType;
    }

    public boolean isTimedBoost() {
        return this.boostType != null;
    }

    public @Nullable CookieBoostType boostType() {
        return this.boostType;
    }

    public @NotNull String statisticKey() {
        return switch (this) {
            case SKILL_XP_RATE -> "skill_xp";
            case JOB_XP_RATE -> "job_xp";
            case JOB_VAULT_RATE -> "job_vault";
            case DOUBLE_DROP_RATE -> "double_drop";
            case SKILL_LEVEL -> "skill_level";
            case JOB_LEVEL -> "job_level";
        };
    }
}
