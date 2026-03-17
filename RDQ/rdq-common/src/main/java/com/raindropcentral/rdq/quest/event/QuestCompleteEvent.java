/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdq.quest.event;

import com.raindropcentral.rdq.database.entity.quest.Quest;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * Event fired when a player completes a quest.
 *
 * <p>This event is called on the main thread after a quest is marked as completed
 * and before rewards are distributed.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestCompleteEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final Quest quest;
    private final Duration completionTime;
    
    /**
     * Constructs a new quest complete event.
     *
     * @param player         the player who completed the quest
     * @param quest          the completed quest
     * @param completionTime the time taken to complete the quest
     */
    public QuestCompleteEvent(
            @NotNull final Player player,
            @NotNull final Quest quest,
            @NotNull final Duration completionTime
    ) {
        this.player = player;
        this.quest = quest;
        this.completionTime = completionTime;
    }
    
    /**
     * Gets the player who completed the quest.
     *
     * @return the player
     */
    @NotNull
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Gets the completed quest.
     *
     * @return the quest
     */
    @NotNull
    public Quest getQuest() {
        return quest;
    }
    
    /**
     * Gets the time taken to complete the quest.
     *
     * @return the completion time
     */
    @NotNull
    public Duration getCompletionTime() {
        return completionTime;
    }
    
    /**
     * Gets handlers.
     */
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
