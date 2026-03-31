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

package com.raindropcentral.rplatform.reward.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the RewardException API type.
 */
public class RewardException extends RuntimeException {

    private final String rewardType;

    /**
     * Executes RewardException.
     */
    public RewardException(@NotNull String message) {
        super(message);
        this.rewardType = null;
    }

    /**
     * Executes RewardException.
     */
    public RewardException(@NotNull String message, @Nullable String rewardType) {
        super(message);
        this.rewardType = rewardType;
    }

    /**
     * Executes RewardException.
     */
    public RewardException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
        this.rewardType = null;
    }

    /**
     * Executes RewardException.
     */
    public RewardException(@NotNull String message, @Nullable String rewardType, @NotNull Throwable cause) {
        super(message, cause);
        this.rewardType = rewardType;
    }

    /**
     * Gets rewardType.
     */
    public @Nullable String getRewardType() {
        return rewardType;
    }
}
