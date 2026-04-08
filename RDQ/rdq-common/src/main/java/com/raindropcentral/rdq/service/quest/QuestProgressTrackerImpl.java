
package com.raindropcentral.rdq.service.quest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.quest.Quest;
import com.raindropcentral.rdq.database.entity.quest.QuestCompletionHistory;
import com.raindropcentral.rdq.database.entity.quest.QuestTask;
import com.raindropcentral.rdq.database.entity.quest.QuestTaskProgress;
import com.raindropcentral.rdq.database.entity.quest.QuestUser;
import com.raindropcentral.rdq.database.repository.quest.QuestCompletionHistoryRepository;
import com.raindropcentral.rdq.database.repository.quest.QuestRepository;
import com.raindropcentral.rdq.database.repository.quest.QuestUserRepository;
import com.raindropcentral.rdq.event.quest.QuestCompleteEvent;
import com.raindropcentral.rdq.event.quest.TaskCompleteEvent;
import com.raindropcentral.rdq.model.quest.ActiveQuest;
import com.raindropcentral.rdq.model.quest.TaskProgress;
import com.raindropcentral.rplatform.scheduler.CancellableTaskHandle;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Implementation of the quest progress tracker.
 * <p>
 * This service batches progress updates for performance and processes them
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
    private final RewardDistributor rewardDistributor;
    private final com.raindropcentral.rdq.cache.quest.QuestCacheManager cacheManager;
    
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
        this.rewardDistributor = plugin.getRewardDistributor();
        this.cacheManager = plugin.getQuestCacheManager();
        
        this.pendingUpdates = new ConcurrentHashMap<>();
        this.running = false;
    }
    
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
    
    @Override
    @NotNull
    public CompletableFuture<Void> completeTask(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier,
            @NotNull final String taskIdentifier
    ) {
        // Use cache for instant access
        final Optional<QuestUser> questUserOpt = cacheManager.getPlayerQuest(playerId, questIdentifier);
        
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
        
        // Mark cache as dirty
        cacheManager.markDirty(playerId);
        
        // Save to database
        return questUserRepository.updateAsync(questUser)
                .thenCompose(saved -> {
                    // Fire task complete event
                    fireTaskCompleteEvent(playerId, quest, taskIdentifier);
                    
                    // TODO: Distribute task rewards
                    
                    // Check if quest should be completed (uses cache)
                    return checkQuestCompletion(playerId, questIdentifier);
                });
    }
    
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
                    
                    // Load quest with collections using entity graphs to avoid MultipleBagFetchException
                    return questRepository.findByIdentifierWithCollections(questIdentifier)
                            .thenCompose(questOpt -> {
                                if (questOpt.isEmpty()) {
                                    LOGGER.log(Level.WARNING, "Quest not found: " + questIdentifier);
                                    return CompletableFuture.completedFuture(null);
                                }
                                
                                final Quest quest = questOpt.get();
                                
                                // Calculate completion time
                                final Duration completionTime = Duration.between(questUser.getStartedAt(), Instant.now());
                                
                                // Distribute rewards first (before marking as completed)
                                // Handle case where reward distributor is not yet initialized
                                final CompletableFuture<com.raindropcentral.rdq.model.quest.RewardDistributionResult> rewardFuture;
                                if (rewardDistributor != null) {
                                    rewardFuture = rewardDistributor.distributeQuestRewards(playerId, quest);
                                } else {
                                    LOGGER.warning("Reward distributor not initialized - skipping reward distribution for quest " + 
                                            questIdentifier + " for player " + playerId);
                                    rewardFuture = CompletableFuture.completedFuture(null);
                                }
                                
                                return rewardFuture.thenCompose(rewardResult -> {
                                // Check if reward distribution failed completely
                                if (rewardResult != null && !rewardResult.allSuccessful()) {
                                    LOGGER.warning("Some rewards failed to distribute for quest " + 
                                            questIdentifier + " for player " + playerId + 
                                            ": " + rewardResult.getFailedRewards().size() + " failed");
                                    
                                    // If all rewards failed, don't mark quest as completed
                                    if (rewardResult.getSuccessfulRewards().isEmpty() && 
                                        !rewardResult.rewards().isEmpty()) {
                                        LOGGER.severe("All rewards failed to distribute for quest " + 
                                                questIdentifier + " for player " + playerId + 
                                                " - quest will not be marked as completed");
                                        return CompletableFuture.completedFuture(null);
                                    }
                                }
                                
                                // Mark as completed (only after successful reward distribution)
                                questUser.setCompleted(true);
                                questUser.setCompletedAt(Instant.now());
                                
                                // Remove from cache immediately to prevent OptimisticLockException
                                cacheManager.removePlayerQuest(playerId, questIdentifier);
                                
                                // Save to database
                                return questUserRepository.updateAsync(questUser)
                                        .thenCompose(saved -> {
                                            // Record completion history
                                            return recordCompletionHistory(playerId, quest)
                                                    .thenCompose(v -> {
                                                        // Fire quest complete event with reward result
                                                        fireQuestCompleteEvent(playerId, quest, completionTime, rewardResult);
                                                        
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
                            });
                });
    }
    
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
        // Use cache to check quest completion (instant access, no database query)
        final Optional<QuestUser> questUserOpt = cacheManager.getPlayerQuest(playerId, questIdentifier);
        
        if (questUserOpt.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        
        final QuestUser questUser = questUserOpt.get();
        
        // Check if all tasks are completed
        final boolean allTasksComplete = questUser.getTaskProgress().stream()
                .allMatch(QuestTaskProgress::isCompleted);
        
        if (allTasksComplete && !questUser.isCompleted()) {
            LOGGER.info("[QuestProgressTracker] All tasks completed for quest " + questIdentifier + 
                    " for player " + playerId + " - triggering auto-completion");
            return completeQuest(playerId, questIdentifier);
        }
        
        return CompletableFuture.completedFuture(null);
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
                    final boolean isUpdate;
                    
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
                        isUpdate = true;
                    } else {
                        // Create new history
                        final Duration cooldown = quest.isRepeatable() && quest.getCooldownSeconds() > 0 ?
                                Duration.ofSeconds(quest.getCooldownSeconds()) : Duration.ZERO;
                        
                        history = QuestCompletionHistory.create(
                                playerId,
                                quest.getIdentifier(),
                                cooldown
                        );
                        isUpdate = false;
                    }
                    
                    // Use update for existing, create for new
                    if (isUpdate) {
                        return completionHistoryRepository.updateAsync(history).thenApply(saved -> null);
                    } else {
                        return completionHistoryRepository.createAsync(history).thenApply(saved -> null);
                    }
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
     * @param playerId       the player's unique identifier
     * @param quest          the quest
     * @param completionTime the time taken to complete the quest
     * @param rewardResult   the reward distribution result (null if no rewards)
     */
    private void fireQuestCompleteEvent(
            @NotNull final UUID playerId,
            @NotNull final Quest quest,
            @NotNull final Duration completionTime,
            @org.jetbrains.annotations.Nullable final com.raindropcentral.rdq.model.quest.RewardDistributionResult rewardResult
    ) {
        plugin.getPlatform().getScheduler().runGlobal(() -> {
            final Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                plugin.getPlatform().getScheduler().runAtEntity(player, () -> {
                    final QuestCompleteEvent event = new QuestCompleteEvent(
                            player,
                            quest,
                            completionTime,
                            rewardResult
                    );
                    Bukkit.getPluginManager().callEvent(event);
                });
            }
        });
    }
    
    @Override
    @NotNull
    public CompletableFuture<List<String>> updateTaskProgress(
            @NotNull final UUID playerId,
            @NotNull final String taskType,
            @NotNull final Map<String, Object> criteria,
            final int amount
    ) {
        if (amount <= 0) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        final java.util.logging.Logger pluginLog = plugin.getPlugin().getLogger();
        pluginLog.info("[QuestProgress] ========== UPDATE TASK PROGRESS ==========");
        pluginLog.info("[QuestProgress] Player: " + playerId);
        pluginLog.info("[QuestProgress] Task Type: " + taskType);
        pluginLog.info("[QuestProgress] Criteria: " + criteria);
        pluginLog.info("[QuestProgress] Amount: " + amount);

        return CompletableFuture.supplyAsync(() -> {
            final List<String> completedTasks = new ArrayList<>();

            try {
                // Get a COPY of the quest list to avoid holding the lock during database operations
                final List<QuestUser> activeQuests = plugin.getQuestCacheManager()
                        .getPlayerQuests(playerId);  // This returns a copy, not direct reference

                pluginLog.info("[QuestProgress] Cached quests for player: " + activeQuests.size());

                if (activeQuests.isEmpty()) {
                    pluginLog.warning("[QuestProgress] No active quests found in cache!");
                    return completedTasks;
                }

                // No need to synchronize - we're working with a copy
                for (final QuestUser questUser : activeQuests) {
                    pluginLog.info("[QuestProgress] Processing quest: " + questUser.getQuest().getIdentifier());
                    
                    // Load tasks using entity graphs to avoid MultipleBagFetchException
                    final Quest questWithTasks = questRepository
                            .findByIdentifierWithCollections(questUser.getQuest().getIdentifier())
                            .join()
                            .orElse(null);

                    if (questWithTasks == null) {
                        pluginLog.warning("[QuestProgress] Quest not found in DB: "
                                + questUser.getQuest().getIdentifier());
                        continue;
                    }

                    pluginLog.info("[QuestProgress] Quest '" + questWithTasks.getIdentifier()
                            + "' has " + questWithTasks.getTasks().size() + " task definitions");
                    pluginLog.info("[QuestProgress] QuestUser has " + questUser.getTaskProgress().size() 
                            + " progress rows");

                    // Log all task progress rows
                        for (final com.raindropcentral.rdq.database.entity.quest.QuestTaskProgress tp : questUser.getTaskProgress()) {
                            pluginLog.info("[QuestProgress]   Progress row: taskId=" + tp.getTaskIdentifier()
                                    + " current=" + tp.getCurrentProgress()
                                    + " required=" + tp.getRequiredProgress()
                                    + " completed=" + tp.isCompleted());
                        }

                        for (final QuestTask task : questWithTasks.getTasks()) {
                            pluginLog.info("[QuestProgress] Checking task: " + task.getTaskIdentifier());
                            pluginLog.info("[QuestProgress]   Requirement data: " + task.getRequirementData());
                            
                            final boolean matches = matchesCriteria(task, criteria);
                            pluginLog.info("[QuestProgress]   Matches criteria: " + matches);

                            if (!matches) continue;

                            final Optional<com.raindropcentral.rdq.database.entity.quest.QuestTaskProgress> taskProgressOpt = 
                                    questUser.getTaskProgress()
                                    .stream()
                                    .filter(tp -> tp.getTaskIdentifier().equalsIgnoreCase(task.getTaskIdentifier()))
                                    .findFirst();

                        if (taskProgressOpt.isEmpty()) {
                            pluginLog.warning("[QuestProgress] No progress row found for task: " + task.getTaskIdentifier());
                            pluginLog.warning("[QuestProgress] Available progress rows: " + 
                                    questUser.getTaskProgress().stream()
                                            .map(com.raindropcentral.rdq.database.entity.quest.QuestTaskProgress::getTaskIdentifier)
                                            .collect(java.util.stream.Collectors.joining(", ")));
                            continue;
                        }

                        final com.raindropcentral.rdq.database.entity.quest.QuestTaskProgress taskProgress = taskProgressOpt.get();
                        if (taskProgress.isCompleted()) {
                            pluginLog.info("[QuestProgress] Task already completed: " + task.getTaskIdentifier());
                            continue;
                        }

                        final int newProgress = Math.min(
                                taskProgress.getCurrentProgress() + amount,
                                taskProgress.getRequiredProgress()
                        );
                        taskProgress.setCurrentProgress(newProgress);

                        pluginLog.info("[QuestProgress] Updated progress: " + newProgress
                                + "/" + taskProgress.getRequiredProgress());

                        if (newProgress >= taskProgress.getRequiredProgress()) {
                            taskProgress.setCompleted(true);
                            taskProgress.setCompletedAt(Instant.now());
                            completedTasks.add(task.getTaskIdentifier());
                            fireTaskCompleteEvent(playerId, questWithTasks, task.getTaskIdentifier());
                            pluginLog.info("[QuestProgress] TASK COMPLETED: " + task.getTaskIdentifier());
                            
                            // Check if all tasks are complete (we're working with the actual cached object now)
                            final boolean allTasksComplete = questUser.getTaskProgress().stream()
                                    .allMatch(com.raindropcentral.rdq.database.entity.quest.QuestTaskProgress::isCompleted);
                            
                            pluginLog.info("[QuestProgress] All tasks complete check: " + allTasksComplete);
                            pluginLog.info("[QuestProgress] Task completion status:");
                            for (final com.raindropcentral.rdq.database.entity.quest.QuestTaskProgress tp : questUser.getTaskProgress()) {
                                pluginLog.info("[QuestProgress]   - " + tp.getTaskIdentifier() 
                                        + ": completed=" + tp.isCompleted() 
                                        + " progress=" + tp.getCurrentProgress() + "/" + tp.getRequiredProgress());
                            }
                            
                            if (allTasksComplete) {
                                pluginLog.info("[QuestProgress] ========== QUEST COMPLETED! ==========");
                                pluginLog.info("[QuestProgress] Calling completeQuest for: " + questWithTasks.getIdentifier());
                                completeQuest(playerId, questWithTasks.getIdentifier()).join();
                                pluginLog.info("[QuestProgress] completeQuest call finished");
                            }
                        }

                        // Mark dirty in cache — will be persisted on quit/auto-save
                        plugin.getQuestCacheManager().markDirty(playerId);
                    }
                }
            } catch (Exception e) {
                pluginLog.log(Level.SEVERE,
                        "[QuestProgress] Error updating progress for player " + playerId, e);
            }

            pluginLog.info("[QuestProgress] ========== END UPDATE ==========");
            return completedTasks;
        });
    }
    
    @Override
    @NotNull
    public CompletableFuture<List<ActiveQuest>> getActiveQuestsWithTaskType(
            @NotNull final UUID playerId,
            @NotNull final String taskType
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get all active quests for the player
                final List<QuestUser> activeQuests = questUserRepository
                        .findActiveByPlayer(playerId)
                        .join();
                
                // Filter quests that have tasks of the specified type
                return activeQuests.stream()
                        .filter(questUser -> hasTaskOfType(questUser.getQuest(), taskType))
                        .map(this::convertToActiveQuest)
                        .collect(Collectors.toList());
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error getting active quests with task type " + taskType + 
                        " for player " + playerId, e);
                return Collections.emptyList();
            }
        });
    }
    
    @Override
    public boolean matchesCriteria(
            @NotNull final QuestTask task,
            @NotNull final Map<String, Object> criteria
    ) {
        // Parse requirement data to get task type and criteria
        final String requirementData = task.getRequirementData();
        if (requirementData == null || requirementData.isEmpty()) {
            plugin.getPlugin().getLogger().fine("[matchesCriteria] No requirement data for task: " + task.getTaskIdentifier());
            return false;
        }
        
        try {
            // Parse JSON to extract type and criteria fields
            final JsonObject json = JsonParser.parseString(requirementData).getAsJsonObject();
            
            plugin.getPlugin().getLogger().info("[matchesCriteria] Task: " + task.getTaskIdentifier());
            plugin.getPlugin().getLogger().info("[matchesCriteria]   JSON: " + json);
            plugin.getPlugin().getLogger().info("[matchesCriteria]   Criteria: " + criteria);
            
            if (!json.has("type")) {
                plugin.getPlugin().getLogger().warning("[matchesCriteria] No 'type' field in requirement data");
                return false;
            }
            
            final String taskTypeFromData = json.get("type").getAsString();
            plugin.getPlugin().getLogger().info("[matchesCriteria]   Task type from data: " + taskTypeFromData);
            
            // Check if task type matches the criteria's task type (if provided)
            if (criteria.containsKey("task_type")) {
                final String criteriaTaskType = criteria.get("task_type").toString();
                plugin.getPlugin().getLogger().info("[matchesCriteria]   Criteria task type: " + criteriaTaskType);
                if (!taskTypeFromData.equalsIgnoreCase(criteriaTaskType)) {
                    plugin.getPlugin().getLogger().info("[matchesCriteria]   Task type mismatch!");
                    return false;
                }
            }
            
            // Match specific criteria fields
            for (Map.Entry<String, Object> entry : criteria.entrySet()) {
                final String key = entry.getKey();
                final Object value = entry.getValue();
                
                plugin.getPlugin().getLogger().info("[matchesCriteria]   Checking criterion: " + key + " = " + value);
                
                // Skip task_type as it's already checked
                if ("task_type".equals(key)) {
                    continue;
                }
                
                // Skip context-only keys that are not stored in requirement data
                if ("world".equals(key)) {
                    plugin.getPlugin().getLogger().fine("[matchesCriteria]   Skipping 'world' key");
                    continue;
                }
                
                // Check if the task has this criteria field
                if (json.has(key)) {
                    final String taskValue = json.get(key).getAsString();
                    plugin.getPlugin().getLogger().info("[matchesCriteria]   Task value for '" + key + "': " + taskValue);
                    
                    // Compare values (case-insensitive for strings)
                    if (!taskValue.equalsIgnoreCase(value.toString())) {
                        plugin.getPlugin().getLogger().info("[matchesCriteria]   Value mismatch: " + taskValue + " != " + value);
                        return false;
                    }
                    plugin.getPlugin().getLogger().info("[matchesCriteria]   Value matches!");
                } else {
                    // If task doesn't have this criteria field, it doesn't match
                    plugin.getPlugin().getLogger().warning("[matchesCriteria]   Task missing criterion: " + key);
                    return false;
                }
            }
            
            // All criteria matched
            plugin.getPlugin().getLogger().info("[matchesCriteria] ✓ ALL CRITERIA MATCHED!");
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse requirement data for task " + 
                    task.getTaskIdentifier(), e);
            return false;
        }
    }
    
    /**
     * Checks if a quest has any tasks of the specified type.
     *
     * @param quest    the quest to check
     * @param taskType the task type to look for
     * @return true if the quest has at least one task of the specified type
     */
    private boolean hasTaskOfType(@NotNull final Quest quest, @NotNull final String taskType) {
        return quest.getTasks().stream()
                .anyMatch(task -> {
                    final String requirementData = task.getRequirementData();
                    if (requirementData == null || requirementData.isEmpty()) {
                        return false;
                    }
                    
                    try {
                        final JsonObject json = JsonParser.parseString(requirementData).getAsJsonObject();
                        return json.has("type") && json.get("type").getAsString().equalsIgnoreCase(taskType);
                    } catch (Exception e) {
                        return false;
                    }
                });
    }
    
    /**
     * Converts a QuestUser to an ActiveQuest model.
     *
     * @param questUser the quest user to convert
     * @return the active quest model
     */
    @NotNull
    private ActiveQuest convertToActiveQuest(@NotNull final QuestUser questUser) {
        final Quest quest = questUser.getQuest();
        
        // Convert task progress
        final List<TaskProgress> taskProgressList = questUser.getTaskProgress().stream()
                .map(tp -> {
                    // Find the task to get its identifier
                    final QuestTask task = quest.getTasks().stream()
                            .filter(t -> t.getTaskIdentifier().equalsIgnoreCase(tp.getTaskIdentifier()))
                            .findFirst()
                            .orElse(null);
                    
                    final int current = tp.getCurrentProgress();
                    final int required = tp.getRequiredProgress();
                    final double percentage = required > 0 ? (double) current / required : 0.0;
                    
                    return new TaskProgress(
                            tp.getId(),  // taskId (Long)
                            tp.getTaskIdentifier(),  // taskIdentifier (String)
                            task != null ? task.getIcon().getDisplayNameKey() : "unknown",  // taskName (String)
                            current,  // currentCount (int)
                            required,  // requiredCount (int)
                            tp.isCompleted(),  // isCompleted (boolean)
                            percentage  // progressPercentage (double)
                    );
                })
                .collect(Collectors.toList());
        
        // Calculate overall progress
        final double overallProgress = questUser.getTaskProgress().isEmpty() ? 0.0 :
                questUser.getTaskProgress().stream()
                        .mapToDouble(tp -> tp.isCompleted() ? 1.0 : 
                                (double) tp.getCurrentProgress() / tp.getRequiredProgress())
                        .average()
                        .orElse(0.0);
        
        // Calculate completed tasks
        final int completedTasks = (int) questUser.getTaskProgress().stream()
                .filter(tp -> tp.isCompleted())
                .count();
        final int totalTasks = questUser.getTaskProgress().size();
        
        // Convert overall progress to percentage (0-100)
        final double progressPercentage = overallProgress * 100.0;
        
        return new ActiveQuest(
                quest.getId(),  // questId (Long)
                quest.getIdentifier(),  // questIdentifier (String)
                quest.getIcon().getDisplayNameKey(),  // questName (String)
                questUser.getPlayerId(),  // playerId (UUID)
                questUser.getStartedAt(),  // startedAt (Instant)
                completedTasks,  // completedTasks (int)
                totalTasks,  // totalTasks (int)
                progressPercentage  // progressPercentage (double, 0-100)
        );
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

