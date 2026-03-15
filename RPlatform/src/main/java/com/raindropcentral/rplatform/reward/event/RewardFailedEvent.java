package com.raindropcentral.rplatform.reward.event;

import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the RewardFailedEvent API type.
 */
public final class RewardFailedEvent extends RewardEvent {

    private final Throwable cause;
    private final String reason;

    /**
     * Executes RewardFailedEvent.
     */
    public RewardFailedEvent(@NotNull Player player, @NotNull AbstractReward reward, @Nullable Throwable cause) {
        super(player, reward);
        this.cause = cause;
        this.reason = cause != null ? cause.getMessage() : "Unknown error";
    }

    /**
     * Executes RewardFailedEvent.
     */
    public RewardFailedEvent(@NotNull Player player, @NotNull AbstractReward reward, @NotNull String reason) {
        super(player, reward);
        this.cause = null;
        this.reason = reason;
    }

    /**
     * Gets cause.
     */
    public @Nullable Throwable getCause() {
        return cause;
    }

    /**
     * Gets reason.
     */
    public @NotNull String getReason() {
        return reason;
    }
}
