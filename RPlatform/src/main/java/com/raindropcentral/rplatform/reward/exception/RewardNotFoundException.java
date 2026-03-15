package com.raindropcentral.rplatform.reward.exception;

import org.jetbrains.annotations.NotNull;

/**
 * Represents the RewardNotFoundException API type.
 */
public final class RewardNotFoundException extends RewardException {

    /**
     * Executes RewardNotFoundException.
     */
    public RewardNotFoundException(@NotNull String rewardType) {
        super("Reward type not found: " + rewardType, rewardType);
    }

    /**
     * Executes RewardNotFoundException.
     */
    public RewardNotFoundException(@NotNull String message, @NotNull String rewardType) {
        super(message, rewardType);
    }
}
