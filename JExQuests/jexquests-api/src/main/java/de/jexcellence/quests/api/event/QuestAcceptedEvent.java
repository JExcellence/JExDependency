package de.jexcellence.quests.api.event;

import de.jexcellence.quests.api.QuestSnapshot;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Fired asynchronously after a player successfully accepts a quest —
 * after the requirement gate has passed and the progression rows have
 * been created.
 */
public class QuestAcceptedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID playerUuid;
    private final QuestSnapshot quest;

    public QuestAcceptedEvent(@NotNull UUID playerUuid, @NotNull QuestSnapshot quest) {
        super(false);
        this.playerUuid = playerUuid;
        this.quest = quest;
    }

    public @NotNull UUID playerUuid() { return this.playerUuid; }
    public @NotNull QuestSnapshot quest() { return this.quest; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
