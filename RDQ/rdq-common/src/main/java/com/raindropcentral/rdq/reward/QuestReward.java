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

package com.raindropcentral.rdq.reward;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.raindropcentral.rdq.service.quest.QuestService;
import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reward that grants quest start or completion as a reward.
 * <p>
 * This reward integrates with the quest service to automatically start a quest
 * for a player or mark a quest as completed. This is useful for quest chains
 * where completing one quest automatically starts or completes another.
 * <p>
 * Example JSON configuration:
 * <pre>
 * {
 *   "type": "QUEST",
 *   "questIdentifier": "advanced_zombie_slayer",
 *   "action": "START"
 * }
 * </pre>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
@JsonTypeName("QUEST")
public final class QuestReward extends AbstractReward {
    
    private static final Logger LOGGER = Logger.getLogger(QuestReward.class.getName());
    
    /**
     * Action to perform with the quest.
     */
    public enum QuestAction {
        /**
         * Start the quest for the player.
         */
        START,
        
        /**
         * Complete the quest for the player (admin/cheat).
         */
        COMPLETE
    }
    
    @JsonProperty("questIdentifier")
    private final String questIdentifier;
    
    @JsonProperty("action")
    private final QuestAction action;
    
    @JsonIgnore
    private QuestService questService;
    
    /**
     * Simple constructor that defaults to starting the quest.
     *
     * @param questIdentifier the quest identifier
     */
    public QuestReward(@NotNull final String questIdentifier) {
        this(questIdentifier, QuestAction.START);
    }
    
    /**
     * Full constructor with action specification.
     *
     * @param questIdentifier the quest identifier
     * @param action          the action to perform
     */
    @JsonCreator
    public QuestReward(
            @JsonProperty("questIdentifier") @NotNull final String questIdentifier,
            @JsonProperty("action") @NotNull final QuestAction action
    ) {
        if (questIdentifier == null || questIdentifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Quest identifier cannot be null or empty");
        }
        
        if (action == null) {
            throw new IllegalArgumentException("Quest action cannot be null");
        }
        
        this.questIdentifier = questIdentifier.toLowerCase();
        this.action = action;
    }
    
    /**
     * Sets the quest service for this reward.
     * <p>
     * This is called during initialization by the quest system.
     *
     * @param questService the quest service
     */
    public void setQuestService(@NotNull final QuestService questService) {
        this.questService = questService;
    }
    
    /**
     * Gets the quest identifier.
     *
     * @return the quest identifier
     */
    @NotNull
    public String getQuestIdentifier() {
        return questIdentifier;
    }
    
    /**
     * Gets the action to perform.
     *
     * @return the quest action
     */
    @NotNull
    public QuestAction getAction() {
        return action;
    }
    
    @Override
    @NotNull
    public String getTypeId() {
        return "QUEST";
    }
    
    @Override
    @NotNull
    public CompletableFuture<Boolean> grant(@NotNull final Player player) {
        if (questService == null) {
            LOGGER.log(Level.SEVERE, "QuestService not set for QuestReward");
            return CompletableFuture.completedFuture(false);
        }
        
        return switch (action) {
            case START -> grantQuestStart(player);
            case COMPLETE -> grantQuestCompletion(player);
        };
    }
    
    /**
     * Grants quest start to the player.
     *
     * @param player the player
     * @return a future completing with true if successful
     */
    private CompletableFuture<Boolean> grantQuestStart(@NotNull final Player player) {
        return questService.startQuest(player.getUniqueId(), questIdentifier)
                .thenApply(result -> {
                    if (result.success()) {
                        LOGGER.log(Level.FINE, "Quest reward: Started quest " + questIdentifier + 
                                " for " + player.getName());
                        return true;
                    } else {
                        LOGGER.log(Level.WARNING, "Quest reward: Failed to start quest " + 
                                questIdentifier + " for " + player.getName() + ": " + result.failureReason());
                        return false;
                    }
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error granting quest start reward for " + 
                            player.getName(), ex);
                    return false;
                });
    }
    
    /**
     * Grants quest completion to the player.
     * <p>
     * This is an admin/cheat function that immediately completes the quest
     * without requiring the player to complete tasks.
     *
     * @param player the player
     * @return a future completing with true if successful
     */
    private CompletableFuture<Boolean> grantQuestCompletion(@NotNull final Player player) {
        // This would require a special admin method in QuestService
        // For now, we'll log a warning and return false
        LOGGER.log(Level.WARNING, "Quest COMPLETE action not yet implemented for rewards");
        return CompletableFuture.completedFuture(false);
    }
    
    @Override
    public double getEstimatedValue() {
        // Quest rewards have variable value depending on the quest
        // Return a moderate value as an estimate
        return 50.0;
    }
    
    @Override
    @NotNull
    public String getDescriptionKey() {
        return "reward.quest." + action.name().toLowerCase();
    }
    
    @Override
    public void validate() {
        if (questIdentifier == null || questIdentifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Quest identifier cannot be empty");
        }
        
        if (action == null) {
            throw new IllegalArgumentException("Quest action cannot be null");
        }
    }
    
    /**
     * Gets a detailed description of this reward.
     *
     * @return the detailed description
     */
    @JsonIgnore
    @NotNull
    public String getDetailedDescription() {
        return switch (action) {
            case START -> "Start quest: " + questIdentifier;
            case COMPLETE -> "Complete quest: " + questIdentifier;
        };
    }
}
