package com.raindropcentral.rplatform.reward.event;

import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the RewardEvent API type.
 */
public abstract class RewardEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final AbstractReward reward;

    protected RewardEvent(@NotNull Player player, @NotNull AbstractReward reward) {
        this.player = player;
        this.reward = reward;
    }

    protected RewardEvent(@NotNull Player player, @NotNull AbstractReward reward, boolean async) {
        super(async);
        this.player = player;
        this.reward = reward;
    }

    /**
     * Gets player.
     */
    public @NotNull Player getPlayer() {
        return player;
    }

    /**
     * Gets reward.
     */
    public @NotNull AbstractReward getReward() {
        return reward;
    }

    /**
     * Gets handlers.
     */
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Gets handlerList.
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
