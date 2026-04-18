package de.jexcellence.jexplatform.requirement.event;

import de.jexcellence.jexplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired before requirement resources are consumed. Cancelling prevents consumption.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class RequirementConsumeEvent extends RequirementEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;

    /**
     * Creates a consume event.
     *
     * @param player      the player whose resources are consumed
     * @param requirement the requirement being consumed
     */
    public RequirementConsumeEvent(@NotNull Player player,
                                   @NotNull AbstractRequirement requirement) {
        super(player, requirement);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Returns the handler list.
     *
     * @return the handler list
     */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
