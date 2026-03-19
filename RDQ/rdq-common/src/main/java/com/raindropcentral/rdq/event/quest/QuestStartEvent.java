package com.raindropcentral.rdq.event.quest;

import com.raindropcentral.rdq.database.entity.quest.Quest;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player starts a quest.
 * <p>
 * This event is called on the main thread after a quest is successfully started.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestStartEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final Quest quest;
    
    /**
     * Constructs a new quest start event.
     *
     * @param player the player who started the quest
     * @param quest  the started quest
     */
    public QuestStartEvent(
            @NotNull final Player player,
            @NotNull final Quest quest
    ) {
        this.player = player;
        this.quest = quest;
    }
    
    /**
     * Gets the player who started the quest.
     *
     * @return the player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Gets the started quest.
     *
     * @return the quest
     */
    @NotNull
    public Quest getQuest() {
        return quest;
    }
    
    @Override
    @NotNull
    public HandlerList getHandlers() {
        return HANDLERS;
    }
    
    /**
     * Gets the handler list for this event.
     *
     * @return the handler list
     */
    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
