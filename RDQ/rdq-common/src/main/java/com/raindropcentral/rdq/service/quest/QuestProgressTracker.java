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


package com.raindropcentral.rdq.service.quest;

import com.raindropcentral.rdq.database.entity.quest.QuestTask;
import com.raindropcentral.rdq.model.quest.ActiveQuest;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for tracking and updating quest progress.
 * <p>
 * This service handles:
 * <ul>
 *     <li>Task progress updates</li>
 *     <li>Task completion</li>
 *     <li>Quest completion</li>
 *     <li>Reward distribution</li>
 *     <li>Batch progress processing</li>
 *     <li>Task handler integration</li>
 * </ul>
 * </p>
 * <p>
 * Progress updates are batched for performance and processed periodically.
 * All methods return {@link CompletableFuture} for non-blocking async operations.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public interface QuestProgressTracker {
    
    /**
     * Updates progress for a specific quest task.
     * <p>
     * This method batches updates and processes them periodically for performance.
     * If the task reaches its required progress, it will be automatically completed.
     * </p>
     *
     * @param playerId        the player's unique identifier
     * @param questIdentifier the quest identifier
     * @param taskIdentifier  the task identifier
     * @param amount          the amount to add to current progress
     * @return a future completing when the update is queued
     */
    @NotNull
    CompletableFuture<Void> updateProgress(
            @NotNull UUID playerId,
            @NotNull String questIdentifier,
            @NotNull String taskIdentifier,
            int amount
    );
    
    /**
     * Marks a specific task as completed.
     * <p>
     * This distributes task rewards and checks if the quest should be completed.
     * </p>
     *
     * @param playerId        the player's unique identifier
     * @param questIdentifier the quest identifier
     * @param taskIdentifier  the task identifier
     * @return a future completing when the task is marked complete
     */
    @NotNull
    CompletableFuture<Void> completeTask(
            @NotNull UUID playerId,
            @NotNull String questIdentifier,
            @NotNull String taskIdentifier
    );
    
    /**
     * Marks a quest as completed.
     * <p>
     * This distributes quest rewards, records completion history,
     * and handles cooldown/repeat logic.
     * </p>
     *
     * @param playerId        the player's unique identifier
     * @param questIdentifier the quest identifier
     * @return a future completing when the quest is marked complete
     */
    @NotNull
    CompletableFuture<Void> completeQuest(
            @NotNull UUID playerId,
            @NotNull String questIdentifier
    );
    
    /**
     * Checks if a specific task is completed.
     *
     * @param playerId        the player's unique identifier
     * @param questIdentifier the quest identifier
     * @param taskIdentifier  the task identifier
     * @return a future completing with true if the task is completed
     */
    @NotNull
    CompletableFuture<Boolean> isTaskComplete(
            @NotNull UUID playerId,
            @NotNull String questIdentifier,
            @NotNull String taskIdentifier
    );
    
    /**
     * Gets the current progress for a specific task.
     *
     * @param playerId        the player's unique identifier
     * @param questIdentifier the quest identifier
     * @param taskIdentifier  the task identifier
     * @return a future completing with the current progress value
     */
    @NotNull
    CompletableFuture<Integer> getTaskProgress(
            @NotNull UUID playerId,
            @NotNull String questIdentifier,
            @NotNull String taskIdentifier
    );
    
    /**
     * Forces immediate processing of all pending progress updates.
     * <p>
     * This should be called before server shutdown or when immediate
     * consistency is required.
     * </p>
     *
     * @return a future completing when all updates are processed
     */
    @NotNull
    CompletableFuture<Void> flushPendingUpdates();
    
    /**
     * Starts the batch processing scheduler.
     * <p>
     * This should be called during plugin initialization.
     * </p>
     */
    void start();
    
    /**
     * Stops the batch processing scheduler and flushes pending updates.
     * <p>
     * This should be called during plugin shutdown.
     * </p>
     *
     * @return a future completing when shutdown is complete
     */
    @NotNull
    CompletableFuture<Void> shutdown();
    
    /**
     * Updates task progress for a player with criteria matching.
     * <p>
     * This method is designed for task handlers to update progress based on
     * game events. It will:
     * <ul>
     *   <li>Find all active quests for the player</li>
     *   <li>Filter tasks by the specified task type</li>
     *   <li>Match tasks against the provided criteria</li>
     *   <li>Update progress for matching tasks</li>
     *   <li>Fire TaskCompleteEvent when tasks complete</li>
     *   <li>Fire QuestCompleteEvent when quests complete</li>
     * </ul>
     * </p>
     *
     * @param playerId the player's unique identifier
     * @param taskType the type of task (e.g., "KILL_MOBS", "COLLECT_ITEMS")
     * @param criteria the criteria to match against task requirements
     * @param amount   the amount to add to progress
     * @return a future completing with the list of completed task identifiers
     */
    @NotNull
    CompletableFuture<List<String>> updateTaskProgress(
            @NotNull UUID playerId,
            @NotNull String taskType,
            @NotNull Map<String, Object> criteria,
            int amount
    );
    
    /**
     * Gets all active quests for a player that have tasks of a specific type.
     * <p>
     * This method is used by task handlers to efficiently find quests that
     * might be affected by a game event.
     * </p>
     *
     * @param playerId the player's unique identifier
     * @param taskType the type of task to filter by
     * @return a future completing with the list of active quests
     */
    @NotNull
    CompletableFuture<List<ActiveQuest>> getActiveQuestsWithTaskType(
            @NotNull UUID playerId,
            @NotNull String taskType
    );
    
    /**
     * Checks if a task matches the given criteria.
     * <p>
     * This method parses the task's requirement data and compares it against
     * the provided criteria. A task matches if:
     * <ul>
     *   <li>The task type matches</li>
     *   <li>All required criteria fields are present and match</li>
     * </ul>
     * </p>
     * <p>
     * Common criteria keys include:
     * <ul>
     *   <li>"entity_type" - For mob kills (e.g., "ZOMBIE")</li>
     *   <li>"material" - For item collection/crafting (e.g., "DIAMOND")</li>
     *   <li>"block_type" - For block breaking/placing (e.g., "STONE")</li>
     *   <li>"world" - For location-based tasks</li>
     * </ul>
     * </p>
     *
     * @param task     the quest task to check
     * @param criteria the criteria to match against
     * @return true if the task matches the criteria
     */
    boolean matchesCriteria(@NotNull QuestTask task, @NotNull Map<String, Object> criteria);
}
