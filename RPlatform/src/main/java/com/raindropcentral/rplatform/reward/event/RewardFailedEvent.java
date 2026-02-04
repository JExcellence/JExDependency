package com.raindropcentral.rplatform.reward.event;

import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RewardFailedEvent extends RewardEvent {

    private final Throwable cause;
    private final String reason;

    public RewardFailedEvent(@NotNull Player player, @NotNull AbstractReward reward, @Nullable Throwable cause) {
        super(player, reward);
        this.cause = cause;
        this.reason = cause != null ? cause.getMessage() : "Unknown error";
    }

    public RewardFailedEvent(@NotNull Player player, @NotNull AbstractReward reward, @NotNull String reason) {
        super(player, reward);
        this.cause = null;
        this.reason = reason;
    }

    public @Nullable Throwable getCause() {
        return cause;
    }

    public @NotNull String getReason() {
        return reason;
    }
}
