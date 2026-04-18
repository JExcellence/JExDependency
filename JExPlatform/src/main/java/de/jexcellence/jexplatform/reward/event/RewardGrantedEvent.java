package de.jexcellence.jexplatform.reward.event;

import de.jexcellence.jexplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired after a reward is successfully granted. */
public class RewardGrantedEvent extends RewardEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public RewardGrantedEvent(@NotNull Player player, @NotNull AbstractReward reward) {
        super(player, reward);
    }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
