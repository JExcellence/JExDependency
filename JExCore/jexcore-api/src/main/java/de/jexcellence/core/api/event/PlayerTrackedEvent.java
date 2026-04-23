package de.jexcellence.core.api.event;

import de.jexcellence.core.api.CorePlayerSnapshot;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired once asynchronously after a player's {@code CorePlayer} record is
 * created or updated during the join flow.
 */
public class PlayerTrackedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final CorePlayerSnapshot snapshot;

    /**
     * Creates the event.
     *
     * @param snapshot projection of the persisted player record
     */
    public PlayerTrackedEvent(@NotNull CorePlayerSnapshot snapshot) {
        super(true);
        this.snapshot = snapshot;
    }

    public @NotNull CorePlayerSnapshot snapshot() {
        return this.snapshot;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
