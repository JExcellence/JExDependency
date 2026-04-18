package de.jexcellence.jexplatform.reward.exception;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base exception for reward system errors.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class RewardException extends RuntimeException {

    private final String rewardType;

    /** Creates an exception with a message. */
    public RewardException(@NotNull String message) {
        this(message, null, null);
    }

    /** Creates an exception with a message and cause. */
    public RewardException(@NotNull String message, @Nullable Throwable cause) {
        this(message, null, cause);
    }

    /** Creates an exception with full details. */
    public RewardException(@NotNull String message, @Nullable String rewardType,
                           @Nullable Throwable cause) {
        super(message, cause);
        this.rewardType = rewardType;
    }

    /** Returns the reward type that caused this error. */
    public @Nullable String getRewardType() {
        return rewardType;
    }
}
