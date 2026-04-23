package de.jexcellence.quests.api.event;

import de.jexcellence.quests.api.RankSnapshot;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired after a player is promoted within a rank tree — after the
 * gate has passed and the rank reward has been granted.
 *
 * <p>{@link #snapshot()} reflects the post-promotion state.
 *
 * <p>Declared as a <b>synchronous</b> event. The promotion path runs
 * partially on the main thread (the requirement evaluator schedules
 * its check there) and partially async — an async-only event would
 * crash half the call sites. Listeners that need async work should
 * schedule it explicitly.
 */
public class RankPromotedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final RankSnapshot snapshot;
    private final String previousRankIdentifier;

    public RankPromotedEvent(@NotNull RankSnapshot snapshot, @NotNull String previousRankIdentifier) {
        super(false);
        this.snapshot = snapshot;
        this.previousRankIdentifier = previousRankIdentifier;
    }

    public @NotNull RankSnapshot snapshot() { return this.snapshot; }
    public @NotNull String previousRankIdentifier() { return this.previousRankIdentifier; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
