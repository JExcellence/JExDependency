package com.raindropcentral.rdq.service.quest;

import org.jetbrains.annotations.NotNull;

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
}
