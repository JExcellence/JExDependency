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
