package de.jexcellence.quests.api.event;

import de.jexcellence.quests.api.PerkSnapshot;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired asynchronously after a player activates a perk and (for
 * {@code ACTIVE} kind) the cooldown has been reset.
 */
public class PerkActivatedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final PerkSnapshot snapshot;

    public PerkActivatedEvent(@NotNull PerkSnapshot snapshot) {
        super(false);
        this.snapshot = snapshot;
    }

    public @NotNull PerkSnapshot snapshot() { return this.snapshot; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
