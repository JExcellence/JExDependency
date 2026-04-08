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

package com.raindropcentral.rdq.event.quest;

import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.entity.quest.QuestReward;
import com.raindropcentral.rdq.model.quest.RewardDistributionResult;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;

/**
 * Event fired when a player completes a quest.
 * <p>
 * This event is called on the main thread after a quest is marked as completed
 * and after rewards have been distributed.
 *
 * @author RaindropCentral
 * @version 2.0.0
 */
public class QuestCompleteEvent extends Event {
    
    private static final HandlerList HANDLERS = new HandlerList();
    
    private final Player player;
    private final Quest quest;
    private final Duration completionTime;
    private final RewardDistributionResult rewardResult;
    
    /**
     * Constructs a new quest complete event.
     *
     * @param player         the player who completed the quest
     * @param quest          the completed quest
     * @param completionTime the time taken to complete the quest
     * @param rewardResult   the result of reward distribution (null if no rewards)
     */
    public QuestCompleteEvent(
            @NotNull final Player player,
            @NotNull final Quest quest,
            @NotNull final Duration completionTime,
            @Nullable final RewardDistributionResult rewardResult
    ) {
        this.player = player;
        this.quest = quest;
        this.completionTime = completionTime;
        this.rewardResult = rewardResult;
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
     * Gets the reward distribution result.
     * <p>
     * This will be null if the quest had no rewards.
     *
     * @return the reward distribution result, or null if no rewards
     */
    @Nullable
    public RewardDistributionResult getRewardResult() {
        return rewardResult;
    }
    
    /**
     * Checks if all rewards were successfully distributed.
     *
     * @return true if all rewards succeeded or there were no rewards
     */
    public boolean allRewardsSuccessful() {
        return rewardResult == null || rewardResult.allSuccessful();
    }
    
    /**
     * Gets the list of successfully distributed rewards.
     *
     * @return the list of successful rewards (empty if no rewards or all failed)
     */
    @NotNull
    public List<QuestReward> getSuccessfulRewards() {
        return rewardResult != null ? rewardResult.getSuccessfulRewards() : List.of();
    }
    
    /**
     * Gets the list of failed rewards.
     *
     * @return the list of failed rewards (empty if no rewards or all succeeded)
     */
    @NotNull
    public List<QuestReward> getFailedRewards() {
        return rewardResult != null ? rewardResult.getFailedRewards() : List.of();
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
