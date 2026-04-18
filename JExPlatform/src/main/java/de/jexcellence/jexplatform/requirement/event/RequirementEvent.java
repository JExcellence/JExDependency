package de.jexcellence.jexplatform.requirement.event;

import de.jexcellence.jexplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Abstract base for all requirement-related events.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public abstract class RequirementEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final AbstractRequirement requirement;

    /**
     * Creates a requirement event.
     *
     * @param player      the player involved
     * @param requirement the requirement involved
     */
    protected RequirementEvent(@NotNull Player player,
                               @NotNull AbstractRequirement requirement) {
        this.player = player;
        this.requirement = requirement;
    }

    /**
     * Returns the player involved in this event.
     *
     * @return the player
     */
    public @NotNull Player getPlayer() {
        return player;
    }

    /**
     * Returns the requirement involved in this event.
     *
     * @return the requirement
     */
    public @NotNull AbstractRequirement getRequirement() {
        return requirement;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Returns the handler list for this event type.
     *
     * @return the handler list
     */
    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
