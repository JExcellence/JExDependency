package com.raindropcentral.rplatform.reward.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RewardException extends RuntimeException {

    private final String rewardType;

    public RewardException(@NotNull String message) {
        super(message);
        this.rewardType = null;
    }

    public RewardException(@NotNull String message, @Nullable String rewardType) {
        super(message);
        this.rewardType = rewardType;
    }

    public RewardException(@NotNull String message, @NotNull Throwable cause) {
        super(message, cause);
        this.rewardType = null;
    }

    public RewardException(@NotNull String message, @Nullable String rewardType, @NotNull Throwable cause) {
        super(message, cause);
        this.rewardType = rewardType;
    }

    public @Nullable String getRewardType() {
        return rewardType;
    }
}
