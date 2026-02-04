package com.raindropcentral.rplatform.reward.event;

import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.jetbrains.annotations.NotNull;

public final class RewardGrantEvent extends RewardEvent implements Cancellable {

    private boolean cancelled = false;

    public RewardGrantEvent(@NotNull Player player, @NotNull AbstractReward reward) {
        super(player, reward);
    }

    public RewardGrantEvent(@NotNull Player player, @NotNull AbstractReward reward, boolean async) {
        super(player, reward, async);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
