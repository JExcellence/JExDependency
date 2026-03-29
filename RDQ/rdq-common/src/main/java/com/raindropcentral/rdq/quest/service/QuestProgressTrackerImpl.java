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

package com.raindropcentral.rdq.quest.service;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.quest.*;
import com.raindropcentral.rdq.database.repository.quest.QuestCompletionHistoryRepository;
import com.raindropcentral.rdq.database.repository.quest.QuestRepository;
import com.raindropcentral.rdq.database.repository.quest.QuestUserRepository;
import com.raindropcentral.rdq.quest.event.QuestCompleteEvent;
import com.raindropcentral.rdq.quest.event.TaskCompleteEvent;
import com.raindropcentral.rplatform.scheduler.CancellableTaskHandle;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the quest progress tracker.
 *
 * <p>This service batches progress updates for performance and processes them
 * periodically. It handles task completion, quest completion, and reward
 * distribution.
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class QuestProgressTrackerImpl implements QuestProgressTracker {
    
    private static final Logger LOGGER = Logger.getLogger(QuestProgressTrackerImpl.class.getName());
    
    private static final long BATCH_INTERVAL_TICKS = 20L * 30L; // 30 seconds
    
    private final RDQ plugin;
    private final QuestRepository questRepository;
    private final QuestUserRepository questUserRepository;
    private final QuestCompletionHistoryRepository completionHistoryRepository;
    
    /**
     * Pending progress updates: (playerId, questId, taskId) -> amount.
     */
    private final Map<ProgressKey, AtomicInteger> pendingUpdates;
    
    private CancellableTaskHandle batchTask;
    private volatile boolean running;
    
    /**
     * Constructs a new quest progress tracker implementation.
     *
     * @param plugin the RDQ plugin instance
     */
    public QuestProgressTrackerImpl(@NotNull final RDQ plugin) {
        this.plugin = plugin;
        
        // Get repositories from plugin
        this.questRepository = plugin.getQuestRepository();
        this.questUserRepository = plugin.getQuestUserRepository();
        this.completionHistoryRepository = plugin.getQuestCompletionHistoryRepository();
        
        this.pendingUpdates = new ConcurrentHashMap<>();
        this.running = false;
    }
    
    /**
     * Executes start.
     */
    @Override
    public void start() {
        if (running) {
            LOGGER.warning("Quest progress tracker is already running");
            return;
        }
        
        running = true;
        
        // Start batch processing task
        batchTask = plugin.getPlatform().getScheduler().runRepeatingAsync(
                this::processBatch,
                BATCH_INTERVAL_TICKS,
                BATCH_INTERVAL_TICKS
        );
        
        LOGGER.info("Quest progress tracker started");
    }
    
    /**
     * Executes shutdown.
     */
    @Override
    @NotNull
    public CompletableFuture<Void> shutdown() {
        if (!running) {
            return CompletableFuture.completedFuture(null);
        }
        
        running = false;
        
        // Cancel batch task
        if (batchTask != null) {
            batchTask.cancel();
            batchTask = null;
        }
        
        // Flush pending updates
        return flushPendingUpdates()
                .thenRun(() -> LOGGER.info("Quest progress tracker shutdown complete"));
    }
    
    /**
     * Executes updateProgress.
     */
    @Override
    @NotNull
    public CompletableFuture<Void> updateProgress(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier,
            @NotNull final String taskIdentifier,
            final int amount
    ) {
        if (amount <= 0) {
            return CompletableFuture.completedFuture(null);
        }
        
        // Add to pending updates
        final ProgressKey key = new ProgressKey(playerId, questIdentifier, taskIdentifier);
        pendingUpdates.computeIfAbsent(key, k -> new AtomicInteger(0))
                .addAndGet(amount);
        
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Executes completeTask.
     */
    @Override
    @NotNull
    public CompletableFuture<Void> completeTask(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier,
            @NotNull final String taskIdentifier
    ) {
        return questUserRepository.findActiveByPlayerAndQuest(playerId, questIdentifier)
                .thenCompose(questUserOpt -> {
                    if (questUserOpt.isEmpty()) {
                        LOGGER.log(Level.WARNING, "Cannot complete task - quest not active: " + 
                                questIdentifier + " for player " + playerId);
                        return CompletableFuture.completedFuture(null);
                    }
                    
                    final QuestUser questUser = questUserOpt.get();
                    final Quest quest = questUser.getQuest();
                    
                    // Find the task progress
                    final Optional<QuestTaskProgress> taskProgressOpt = questUser.getTaskProgress()
                            .stream()
                            .filter(tp -> tp.getTaskIdentifier().equalsIgnoreCase(taskIdentifier))
                            .findFirst();
                    
                    if (taskProgressOpt.isEmpty()) {
                        LOGGER.log(Level.WARNING, "Task not found: " + taskIdentifier);
                        return CompletableFuture.completedFuture(null);
                    }
                    
                    final QuestTaskProgress taskProgress = taskProgressOpt.get();
                    
                    // Check if already completed
                    if (taskProgress.isCompleted()) {
                        return CompletableFuture.completedFuture(null);
                    }
                    
                    // Mark as completed
                    taskProgress.setCompleted(true);
                    taskProgress.setCompletedAt(Instant.now());
                    taskProgress.setCurrentProgress(taskProgress.getRequiredProgress());
                    
                    // Save to database
                    return questUserRepository.updateAsync(questUser)
                            .thenCompose(saved -> {
                                // Fire task complete event
                                fireTaskCompleteEvent(playerId, quest, taskIdentifier);
                                
                                // TODO: Distribute task rewards
                                
                                // Check if quest should be completed
                                return checkQuestCompletion(playerId, questIdentifier);
                            });
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error completing task for player " + playerId, ex);
                    return null;
                });
    }
    
    /**
     * Executes completeQuest.
     */
    @Override
    @NotNull
    public CompletableFuture<Void> completeQuest(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        return questUserRepository.findActiveByPlayerAndQuest(playerId, questIdentifier)
                .thenCompose(questUserOpt -> {
                    if (questUserOpt.isEmpty()) {
                        LOGGER.log(Level.WARNING, "Cannot complete quest - not active: " + 
                                questIdentifier + " for player " + playerId);
                        return CompletableFuture.completedFuture(null);
                    }
                    
                    final QuestUser questUser = questUserOpt.get();
                    final Quest quest = questUser.getQuest();
                    
                    // Mark as completed
                    questUser.setCompleted(true);
                    questUser.setCompletedAt(Instant.now());
                    
                    // Save to database
                    return questUserRepository.updateAsync(questUser)
                            .thenCompose(saved -> {
                                // Record completion history
                                return recordCompletionHistory(playerId, quest)
                                        .thenCompose(v -> {
                                            // Fire quest complete event
                                            fireQuestCompleteEvent(playerId, quest);
                                            
                                            // TODO: Distribute quest rewards
                                            
                                            // Delete the quest user entity after a delay
                                            // (keep it briefly for completion display)
                                            return CompletableFuture.runAsync(() -> {
                                                try {
                                                    Thread.sleep(5000); // 5 second delay
                                                    questUserRepository.deleteAsync(questUser.getId()).join();
                                                } catch (InterruptedException e) {
                                                    Thread.currentThread().interrupt();
                                                }
                                            });
                                        });
                            });
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error completing quest for player " + playerId, ex);
                    return null;
                });
    }
    
    /**
     * Returns whether taskComplete.
     */
    @Override
    @NotNull
    public CompletableFuture<Boolean> isTaskComplete(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier,
            @NotNull final String taskIdentifier
    ) {
        return questUserRepository.findActiveByPlayerAndQuest(playerId, questIdentifier)
                .thenApply(questUserOpt -> {
                    if (questUserOpt.isEmpty()) {
                        return false;
                    }
                    
                    return questUserOpt.get().getTaskProgress().stream()
                            .anyMatch(tp -> tp.getTaskIdentifier().equalsIgnoreCase(taskIdentifier) &&
                                    tp.isCompleted());
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error checking task completion for player " + playerId, ex);
                    return false;
                });
    }
    
    /**
     * Gets taskProgress.
     */
    @Override
    @NotNull
    public CompletableFuture<Integer> getTaskProgress(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier,
            @NotNull final String taskIdentifier
    ) {
        return questUserRepository.findActiveByPlayerAndQuest(playerId, questIdentifier)
                .thenApply(questUserOpt -> {
                    if (questUserOpt.isEmpty()) {
                        return 0;
                    }
                    
                    return questUserOpt.get().getTaskProgress().stream()
                            .filter(tp -> tp.getTaskIdentifier().equalsIgnoreCase(taskIdentifier))
                            .findFirst()
                            .map(QuestTaskProgress::getCurrentProgress)
                            .orElse(0);
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error getting task progress for player " + playerId, ex);
                    return 0;
                });
    }
    
    /**
     * Executes flushPendingUpdates.
     */
    @Override
    @NotNull
    public CompletableFuture<Void> flushPendingUpdates() {
        if (pendingUpdates.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        
        LOGGER.info("Flushing " + pendingUpdates.size() + " pending progress updates");
        
        return CompletableFuture.runAsync(this::processBatch);
    }
    
    /**
     * Processes all pending progress updates in a batch.
     */
    private void processBatch() {
        if (pendingUpdates.isEmpty()) {
            return;
        }
        
        // Take snapshot of pending updates and clear
        final Map<ProgressKey, AtomicInteger> snapshot = new ConcurrentHashMap<>(pendingUpdates);
        pendingUpdates.clear();
        
        LOGGER.fine("Processing batch of " + snapshot.size() + " progress updates");
        
        // Process each update
        snapshot.forEach((key, amount) -> {
            try {
                processProgressUpdate(key, amount.get()).join();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing progress update for " + key, e);
            }
        });
    }
    
    /**
     * Processes a single progress update.
     *
     * @param key    the progress key
     * @param amount the amount to add
     * @return a future completing when the update is processed
     */
    @NotNull
    private CompletableFuture<Void> processProgressUpdate(
            @NotNull final ProgressKey key,
            final int amount
    ) {
        return questUserRepository.findActiveByPlayerAndQuest(
                key.playerId(),
                key.questIdentifier()
        ).thenCompose(questUserOpt -> {
            if (questUserOpt.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            
            final QuestUser questUser = questUserOpt.get();
            
            // Find the task progress
            final Optional<QuestTaskProgress> taskProgressOpt = questUser.getTaskProgress()
                    .stream()
                    .filter(tp -> tp.getTaskIdentifier().equalsIgnoreCase(key.taskIdentifier()))
                    .findFirst();
            
            if (taskProgressOpt.isEmpty()) {
                LOGGER.log(Level.WARNING, "Task not found: " + key.taskIdentifier());
                return CompletableFuture.completedFuture(null);
            }
            
            final QuestTaskProgress taskProgress = taskProgressOpt.get();
            
            // Skip if already completed
            if (taskProgress.isCompleted()) {
                return CompletableFuture.completedFuture(null);
            }
            
            // Update progress
            final int newProgress = Math.min(
                    taskProgress.getCurrentProgress() + amount,
                    taskProgress.getRequiredProgress()
            );
            
            taskProgress.setCurrentProgress(newProgress);
            
            // Check if task should be completed
            if (newProgress >= taskProgress.getRequiredProgress()) {
                taskProgress.setCompleted(true);
                taskProgress.setCompletedAt(Instant.now());
                
                // Save and complete task
                return questUserRepository.updateAsync(questUser)
                        .thenCompose(saved -> {
                            // Fire task complete event
                            fireTaskCompleteEvent(key.playerId(), questUser.getQuest(), key.taskIdentifier());
                            
                            // TODO: Distribute task rewards
                            
                            // Check if quest should be completed
                            return checkQuestCompletion(key.playerId(), key.questIdentifier());
                        });
            } else {
                // Just save the progress update
                return questUserRepository.updateAsync(questUser).thenApply(saved -> null);
            }
        });
    }
    
    /**
     * Checks if a quest should be completed and completes it if so.
     *
     * @param playerId        the player's unique identifier
     * @param questIdentifier the quest identifier
     * @return a future completing when the check is done
     */
    @NotNull
    private CompletableFuture<Void> checkQuestCompletion(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        return questUserRepository.findActiveByPlayerAndQuest(playerId, questIdentifier)
                .thenCompose(questUserOpt -> {
                    if (questUserOpt.isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    }
                    
                    final QuestUser questUser = questUserOpt.get();
                    
                    // Check if all tasks are completed
                    final boolean allTasksComplete = questUser.getTaskProgress().stream()
                            .allMatch(QuestTaskProgress::isCompleted);
                    
                    if (allTasksComplete && !questUser.isCompleted()) {
                        return completeQuest(playerId, questIdentifier);
                    }
                    
                    return CompletableFuture.completedFuture(null);
                });
    }
    
    /**
     * Records quest completion in history.
     *
     * @param playerId the player's unique identifier
     * @param quest    the completed quest
     * @return a future completing when the history is recorded
     */
    @NotNull
    private CompletableFuture<Void> recordCompletionHistory(
            @NotNull final UUID playerId,
            @NotNull final Quest quest
    ) {
        return completionHistoryRepository.findByPlayerAndQuest(playerId, quest.getIdentifier())
                .thenCompose(historyOpt -> {
                    final QuestCompletionHistory history;
                    
                    if (historyOpt.isPresent()) {
                        // Update existing history
                        history = historyOpt.get();
                        history.setCompletionCount(history.getCompletionCount() + 1);
                        history.setCompletedAt(Instant.now());
                        
                        // Update cooldown if repeatable
                        if (quest.isRepeatable() && quest.getCooldownSeconds() > 0) {
                            final Duration cooldown = Duration.ofSeconds(quest.getCooldownSeconds());
                            history.setNextAvailableAt(Instant.now().plus(cooldown));
                        }
                    } else {
                        // Create new history
                        final Duration cooldown = quest.isRepeatable() && quest.getCooldownSeconds() > 0 ?
                                Duration.ofSeconds(quest.getCooldownSeconds()) : null;
                        
                        history = QuestCompletionHistory.create(
                                playerId,
                                quest.getIdentifier(),
                                cooldown
                        );
                    }
                    
                    return completionHistoryRepository.createAsync(history).thenApply(saved -> null);
                });
    }
    
    /**
     * Fires a task complete event on the main thread.
     *
     * @param playerId       the player's unique identifier
     * @param quest          the quest
     * @param taskIdentifier the task identifier
     */
    private void fireTaskCompleteEvent(
            @NotNull final UUID playerId,
            @NotNull final Quest quest,
            @NotNull final String taskIdentifier
    ) {
        plugin.getPlatform().getScheduler().runGlobal(() -> {
            final Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                plugin.getPlatform().getScheduler().runAtEntity(player, () -> {
                    final TaskCompleteEvent event = new TaskCompleteEvent(player, quest, taskIdentifier);
                    Bukkit.getPluginManager().callEvent(event);
                });
            }
        });
    }
    
    /**
     * Fires a quest complete event on the main thread.
     *
     * @param playerId the player's unique identifier
     * @param quest    the quest
     */
    private void fireQuestCompleteEvent(
            @NotNull final UUID playerId,
            @NotNull final Quest quest
    ) {
        plugin.getPlatform().getScheduler().runGlobal(() -> {
            final Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                plugin.getPlatform().getScheduler().runAtEntity(player, () -> {
                    final QuestCompleteEvent event = new QuestCompleteEvent(
                            player,
                            quest,
                            Duration.between(Instant.now(), Instant.now()) // TODO: Calculate actual time
                    );
                    Bukkit.getPluginManager().callEvent(event);
                });
            }
        });
    }
    
    /**
     * Record class for progress update keys.
     *
     * @param playerId        the player's unique identifier
     * @param questIdentifier the quest identifier
     * @param taskIdentifier  the task identifier
     */
    private record ProgressKey(
            @NotNull UUID playerId,
            @NotNull String questIdentifier,
            @NotNull String taskIdentifier
    ) {
    }
}
