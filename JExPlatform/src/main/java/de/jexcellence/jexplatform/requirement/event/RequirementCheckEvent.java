package de.jexcellence.jexplatform.requirement.event;

import de.jexcellence.jexplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired before a requirement check is performed. Cancelling prevents the check.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class RequirementCheckEvent extends RequirementEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;

    /**
     * Creates a check event.
     *
     * @param player      the player being checked
     * @param requirement the requirement being evaluated
     */
    public RequirementCheckEvent(@NotNull Player player,
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
