package de.jexcellence.quests.api.event;

import de.jexcellence.quests.api.QuestSnapshot;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired asynchronously after a player completes a quest and the
 * reward has been granted (or skipped). {@link #completionCount} is
 * the post-increment completion count for repeatable quests.
 */
public class QuestCompletedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;
    private final QuestSnapshot quest;
    private final int completionCount;

    public QuestCompletedEvent(@NotNull UUID playerUuid, @NotNull QuestSnapshot quest, int completionCount) {
        super(false);
        this.playerUuid = playerUuid;
        this.quest = quest;
        this.completionCount = completionCount;
    }

    public @NotNull UUID playerUuid() { return this.playerUuid; }
    public @NotNull QuestSnapshot quest() { return this.quest; }
    public int completionCount() { return this.completionCount; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
