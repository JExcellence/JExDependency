package com.raindropcentral.rplatform.reward.exception;

import org.jetbrains.annotations.NotNull;

public final class RewardNotFoundException extends RewardException {

    public RewardNotFoundException(@NotNull String rewardType) {
        super("Reward type not found: " + rewardType, rewardType);
    }

    public RewardNotFoundException(@NotNull String message, @NotNull String rewardType) {
        super(message, rewardType);
    }
}
