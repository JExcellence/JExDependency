package de.jexcellence.jexplatform.requirement.event;

import de.jexcellence.jexplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Informational event fired when a player meets a requirement.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class RequirementMetEvent extends RequirementEvent {

    private static final HandlerList HANDLERS = new HandlerList();
    private final double progress;

    /**
     * Creates a met event.
     *
     * @param player      the player who met the requirement
     * @param requirement the requirement that was met
     * @param progress    the final progress value
     */
    public RequirementMetEvent(@NotNull Player player,
                               @NotNull AbstractRequirement requirement,
                               double progress) {
        super(player, requirement);
        this.progress = progress;
    }

    /**
     * Returns the progress value at the time the requirement was met.
     *
     * @return progress between {@code 0.0} and {@code 1.0}
     */
    public double getProgress() {
        return progress;
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
