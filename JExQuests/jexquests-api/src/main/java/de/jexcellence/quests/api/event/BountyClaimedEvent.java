package de.jexcellence.quests.api.event;

import de.jexcellence.quests.api.BountySnapshot;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired asynchronously after a bounty is claimed and the payout has
 * been dispatched to the killer.
 */
public class BountyClaimedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final BountySnapshot bounty;
    private final UUID killerUuid;

    public BountyClaimedEvent(@NotNull BountySnapshot bounty, @NotNull UUID killerUuid) {
        super(false);
        this.bounty = bounty;
        this.killerUuid = killerUuid;
    }

    public @NotNull BountySnapshot bounty() { return this.bounty; }
    public @NotNull UUID killerUuid() { return this.killerUuid; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
