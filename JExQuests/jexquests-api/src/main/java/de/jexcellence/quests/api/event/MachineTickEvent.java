package de.jexcellence.quests.api.event;

import de.jexcellence.quests.api.MachineSnapshot;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired asynchronously by the machine tick scheduler once per
 * configured {@code tick-interval-ticks} per placed machine.
 *
 * <p>Subscribers implement machine behaviour (recipes, input→output
 * transformations, fuel consumption). The event carries a snapshot —
 * for mutating state, look up the live {@code Machine} via
 * {@code MachineService.findByIdAsync(event.snapshot().id())} on the
 * returned future and call {@code writeStorageAsync} / etc.
 *
 * <p>Event is async ({@code super(true)}) so handlers may perform DB
 * work directly without scheduling.
 */
public class MachineTickEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final MachineSnapshot snapshot;
    private final long tickNumber;

    public MachineTickEvent(@NotNull MachineSnapshot snapshot, long tickNumber) {
        super(true);
        this.snapshot = snapshot;
        this.tickNumber = tickNumber;
    }

    /** Identity + location of the ticking machine. */
    public @NotNull MachineSnapshot snapshot() {
        return this.snapshot;
    }

    /** Monotonic tick counter for this machine since server boot. */
    public long tickNumber() {
        return this.tickNumber;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
}
