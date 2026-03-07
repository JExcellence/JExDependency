package com.raindropcentral.rdq.quest.event;

import com.raindropcentral.rdq.database.entity.quest.Quest;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event fired when a player abandons a quest.
 * <p>
 * This event is called on the main thread after a quest is successfully abandoned.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestAbandonEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final Quest quest;
    
    /**
     * Constructs a new quest abandon event.
     *
     * @param player the player who abandoned the quest
     * @param quest  the abandoned quest
     */
    public QuestAbandonEvent(
            @NotNull final Player player,
            @NotNull final Quest quest
    ) {
        this.player = player;
        this.quest = quest;
    }
    
    /**
     * Gets the player who abandoned the quest.
     *
     * @return the player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Gets the abandoned quest.
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
