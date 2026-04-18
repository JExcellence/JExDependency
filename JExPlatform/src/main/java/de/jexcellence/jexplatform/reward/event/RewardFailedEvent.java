package de.jexcellence.jexplatform.reward.event;

import de.jexcellence.jexplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/** Fired when a reward grant fails. */
public class RewardFailedEvent extends RewardEvent {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Throwable cause;

    public RewardFailedEvent(@NotNull Player player, @NotNull AbstractReward reward,
                             @NotNull Throwable cause) {
        super(player, reward);
        this.cause = cause;
    }

    public @NotNull Throwable getCause() { return cause; }
    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
