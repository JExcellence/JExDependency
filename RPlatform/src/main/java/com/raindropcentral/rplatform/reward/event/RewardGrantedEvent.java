package com.raindropcentral.rplatform.reward.event;

import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class RewardGrantedEvent extends RewardEvent {

    public RewardGrantedEvent(@NotNull Player player, @NotNull AbstractReward reward) {
        super(player, reward);
    }

    public RewardGrantedEvent(@NotNull Player player, @NotNull AbstractReward reward, boolean async) {
        super(player, reward, async);
    }
}
