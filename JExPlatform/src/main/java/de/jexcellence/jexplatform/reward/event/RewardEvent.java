package de.jexcellence.jexplatform.reward.event;

import de.jexcellence.jexplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Abstract base for reward events. */
public abstract class RewardEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final AbstractReward reward;

    protected RewardEvent(@NotNull Player player, @NotNull AbstractReward reward) {
        this.player = player;
        this.reward = reward;
    }

    public @NotNull Player getPlayer() { return player; }
    public @NotNull AbstractReward getReward() { return reward; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
