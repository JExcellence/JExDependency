package com.raindropcentral.rdq.service.quest;

import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.entity.quest.QuestCategory;
import com.raindropcentral.rdq.model.quest.ActiveQuest;
import com.raindropcentral.rdq.model.quest.QuestAbandonResult;
import com.raindropcentral.rdq.model.quest.QuestProgress;
import com.raindropcentral.rdq.model.quest.QuestStartResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for quest management operations.
 * <p>
 * This service provides all quest-related functionality including:
 * <ul>
 *     <li>Quest discovery and browsing</li>
 *     <li>Quest starting and abandoning</li>
 *     <li>Active quest tracking</li>
 *     <li>Progress monitoring</li>
 *     <li>Requirement validation</li>
 * </ul>
 * </p>
 * <p>
 * All methods return {@link CompletableFuture} for non-blocking async operations.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public interface QuestService {
    
    /**
     * Gets all enabled quest categories ordered by display order.
     *
     * @return a future completing with the list of categories
     */
    @NotNull
    CompletableFuture<List<QuestCategory>> getCategories();
    
    /**
     * Gets all enabled quests in a specific category.
     *
     * @param categoryIdentifier the category identifier
     * @return a future completing with the list of quests
     */
    @NotNull
    CompletableFuture<List<Quest>> getQuestsByCategory(@NotNull String categoryIdentifier);
    
    /**
     * Gets a specific quest by its identifier.
     *
     * @param questIdentifier the quest identifier
     * @return a future completing with the quest if found
     */
    @NotNull
    CompletableFuture<Optional<Quest>> getQuest(@NotNull String questIdentifier);
    
    /**
     * Attempts to start a quest for a player.
     * <p>
     * This method validates:
     * <ul>
     *     <li>Quest exists and is enabled</li>
     *     <li>Player hasn't reached max active quests</li>
     *     <li>Quest is not already active</li>
     *     <li>Quest is not on cooldown</li>
     *     <li>All quest requirements are met</li>
     * </ul>
     * </p>
     *
     * @param playerId        the player's unique identifier
     * @param questIdentifier the quest identifier
     * @return a future completing with the start result
     */
    @NotNull
    CompletableFuture<QuestStartResult> startQuest(
            @NotNull UUID playerId,
            @NotNull String questIdentifier
    );
    
    /**
     * Abandons an active quest for a player.
     * <p>
     * This removes the quest from the player's active quests and
     * cleans up all associated progress data.
     * </p>
     *
     * @param playerId        the player's unique identifier
     * @param questIdentifier the quest identifier
     * @return a future completing with the abandon result
     */
    @NotNull
    CompletableFuture<QuestAbandonResult> abandonQuest(
            @NotNull UUID playerId,
            @NotNull String questIdentifier
    );
    
    /**
     * Gets all active quests for a player.
     *
     * @param playerId the player's unique identifier
     * @return a future completing with the list of active quests
     */
    @NotNull
    CompletableFuture<List<ActiveQuest>> getActiveQuests(@NotNull UUID playerId);
    
    /**
     * Gets the progress for a specific active quest.
     *
     * @param playerId        the player's unique identifier
     * @param questIdentifier the quest identifier
     * @return a future completing with the quest progress if found
     */
    @NotNull
    CompletableFuture<Optional<QuestProgress>> getProgress(
            @NotNull UUID playerId,
            @NotNull String questIdentifier
    );
    
    /**
     * Checks if a player can start a specific quest.
     * <p>
     * This performs all validation checks without actually starting the quest.
     * Useful for UI display to show why a quest cannot be started.
     * </p>
     *
     * @param playerId        the player's unique identifier
     * @param questIdentifier the quest identifier
     * @return a future completing with the validation result
     */
    @NotNull
    CompletableFuture<QuestStartResult> canStartQuest(
            @NotNull UUID playerId,
            @NotNull String questIdentifier
    );
    
    /**
     * Gets the number of active quests for a player.
     *
     * @param playerId the player's unique identifier
     * @return a future completing with the active quest count
     */
    @NotNull
    CompletableFuture<Integer> getActiveQuestCount(@NotNull UUID playerId);
    
    /**
     * Checks if a specific quest is active for a player.
     *
     * @param playerId        the player's unique identifier
     * @param questIdentifier the quest identifier
     * @return a future completing with true if the quest is active
     */
    @NotNull
    CompletableFuture<Boolean> isQuestActive(
            @NotNull UUID playerId,
            @NotNull String questIdentifier
    );
    
    /**
     * Invalidates all cached data for a player.
     * <p>
     * This should be called when player data is modified externally
     * or when a full refresh is needed.
     * </p>
     *
     * @param playerId the player's unique identifier
     */
    void invalidatePlayerCache(@NotNull UUID playerId);
    
    /**
     * Invalidates all cached quest definitions.
     * <p>
     * This should be called when quest configurations are reloaded.
     * </p>
     */
    void invalidateQuestCache();
    
    /**
     * Processes quest completion and automatically unlocks dependent quests.
     * <p>
     * This method should be called after a quest is completed to trigger
     * the progression system's automatic unlocking logic.
     * </p>
     *
     * @param playerId the player's unique identifier
     * @param questIdentifier the completed quest identifier
     * @return a future completing with the list of newly unlocked quests
     */
    @NotNull
    CompletableFuture<List<Quest>> processQuestCompletion(
            @NotNull UUID playerId,
            @NotNull String questIdentifier
    );
}
