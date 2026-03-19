package com.raindropcentral.rdq.service.quest;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.quest.*;
import com.raindropcentral.rdq.database.repository.QuestCompletionHistoryRepository;
import com.raindropcentral.rdq.database.repository.QuestRepository;
import com.raindropcentral.rdq.cache.quest.PlayerQuestProgressCache;
import com.raindropcentral.rdq.event.quest.QuestCompleteEvent;
import com.raindropcentral.rdq.event.quest.TaskCompleteEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
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
 * Cache-based implementation of the quest progress tracker.
 * <p>
 * This service uses the {@link PlayerQuestProgressCache} for instant access to quest progress
 * without database queries. It batches progress updates for performance and processes them
 * periodically. All progress modifications happen in memory and are persisted via the cache
 * lifecycle (auto-save, player quit).
 * </p>
 *
 * <h3>Design Philosophy</h3>
 * <ul>
 *   <li>All progress reads from cache (instant access, no DB queries)</li>
 *   <li>All progress writes to cache (instant updates, marked dirty)</li>
 *   <li>Batch processing for performance (30 second intervals)</li>
 *   <li>Cache handles persistence (auto-save, player quit)</li>
 * </ul>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since TBD
 */
public class QuestProgressTrackerImpl implements QuestProgressTracker {
    
    private static final Logger LOGGER = Logger.getLogger(QuestProgressTrackerImpl.class.getName());
    
    private static final long BATCH_INTERVAL_TICKS = 20L * 30L; // 30 seconds
    
    private final RDQ plugin;
    private final QuestRepository questRepository;
    private final QuestCompletionHistoryRepository completionHistoryRepository;
    private final PlayerQuestProgressCache progressCache;
    
    /**
     * Pending progress updates: (playerId, questId, taskId) -> amount
     */
    private final Map<ProgressKey, AtomicInteger> pendingUpdates;
    
    private BukkitTask batchTask;
    private volatile boolean running;
    
    /**
     * Constructs a new quest progress tracker implementation.
     *
     * @param plugin the RDQ plugin instance
     */
    public QuestProgressTrackerImpl(@NotNull final RDQ plugin) {
        this.plugin = plugin;
        
        // Get repositories and cache from plugin
        this.questRepository = plugin.getQuestRepository();
        this.completionHistoryRepository = plugin.getQuestCompletionHistoryRepository();
        this.progressCache = plugin.getPlayerQuestProgressCache();
        
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
        batchTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin.getPlugin(),
                this::processBatch,
                BATCH_INTERVAL_TICKS,
                BATCH_INTERVAL_TICKS
        );
        
        LOGGER.info("Quest progress tracker started (cache-based)");
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
        
        // Add to pending updates for batch processing
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
        return CompletableFuture.runAsync(() -> {
            // Check if player cache is loaded
            if (!progressCache.isLoaded(playerId)) {
                LOGGER.warning("Cannot complete task - player cache not loaded: " + playerId);
                return;
            }
            
            // Find quest progress from cache
            PlayerQuestProgress questProgress = findQuestProgressByIdentifier(playerId, questIdentifier);
            if (questProgress == null) {
                LOGGER.warning("Cannot complete task - quest not active: " + questIdentifier + 
                    " for player " + playerId);
                return;
            }
            
            // Find task progress
            Optional<PlayerTaskProgress> taskProgressOpt = questProgress.getTaskProgress().stream()
                    .filter(tp -> tp.getTask().getIdentifier().equalsIgnoreCase(taskIdentifier))
                    .findFirst();
            
            if (taskProgressOpt.isEmpty()) {
                LOGGER.warning("Task not found: " + taskIdentifier);
                return;
            }
            
            PlayerTaskProgress taskProgress = taskProgressOpt.get();
            
            // Check if already completed
            if (taskProgress.isCompleted()) {
                return;
            }
            
            // Mark as completed
            taskProgress.setCompleted(true);
            taskProgress.setCompletedAt(Instant.now());
            taskProgress.setCurrentProgress(taskProgress.getRequiredProgress());
            
            // Update in cache
            progressCache.updateProgress(playerId, questProgress);
            
            // Fire task complete event
            fireTaskCompleteEvent(playerId, questProgress.getQuest(), taskIdentifier);
            
            // TODO: Distribute task rewards
            
            // Check if quest should be completed
            checkQuestCompletion(playerId, questProgress);
        });
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> completeQuest(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        return CompletableFuture.runAsync(() -> {
            // Check if player cache is loaded
            if (!progressCache.isLoaded(playerId)) {
                LOGGER.warning("Cannot complete quest - player cache not loaded: " + playerId);
                return;
            }
            
            // Find quest progress from cache
            PlayerQuestProgress questProgress = findQuestProgressByIdentifier(playerId, questIdentifier);
            if (questProgress == null) {
                LOGGER.warning("Cannot complete quest - not active: " + questIdentifier + 
                    " for player " + playerId);
                return;
            }
            
            Quest quest = questProgress.getQuest();
            
            // Mark as completed
            questProgress.markCompleted();
            
            // Update in cache (will be saved on quit/auto-save)
            progressCache.updateProgress(playerId, questProgress);
            
            // Record completion history
            recordCompletionHistory(playerId, quest).join();
            
            // Fire quest complete event
            fireQuestCompleteEvent(playerId, quest, questProgress.getStartedAt());
            
            // Process completion to unlock dependent quests
            plugin.getQuestService().processQuestCompletion(playerId, questIdentifier)
                    .thenAccept(unlockedQuests -> {
                        if (!unlockedQuests.isEmpty()) {
                            // Notify player about unlocked quests
                            Player player = Bukkit.getPlayer(playerId);
                            if (player != null && player.isOnline()) {
                                player.sendMessage("§aNew quests unlocked:");
                                for (Quest unlockedQuest : unlockedQuests) {
                                    player.sendMessage("§7  - §f" + unlockedQuest.getIdentifier());
                                }
                            }
                        }
                    });
            
            // TODO: Distribute quest rewards
            
            // Remove from cache after a delay (keep briefly for completion display)
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(5000); // 5 second delay
                    progressCache.removeProgress(playerId, quest.getId());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
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
        return CompletableFuture.supplyAsync(() -> {
            // Check if player cache is loaded
            if (!progressCache.isLoaded(playerId)) {
                return false;
            }
            
            // Find quest progress from cache
            PlayerQuestProgress questProgress = findQuestProgressByIdentifier(playerId, questIdentifier);
            if (questProgress == null) {
                return false;
            }
            
            // Check if task is completed
            return questProgress.getTaskProgress().stream()
                    .anyMatch(tp -> tp.getTask().getIdentifier().equalsIgnoreCase(taskIdentifier) &&
                            tp.isCompleted());
        });
    }
    
    @Override
    @NotNull
    public CompletableFuture<Integer> getTaskProgress(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier,
            @NotNull final String taskIdentifier
    ) {
        return CompletableFuture.supplyAsync(() -> {
            // Check if player cache is loaded
            if (!progressCache.isLoaded(playerId)) {
                return 0;
            }
            
            // Find quest progress from cache
            PlayerQuestProgress questProgress = findQuestProgressByIdentifier(playerId, questIdentifier);
            if (questProgress == null) {
                return 0;
            }
            
            // Get task progress - explicitly cast to Integer
            Long progress = questProgress.getTaskProgress().stream()
                    .filter(tp -> tp.getTask().getIdentifier().equalsIgnoreCase(taskIdentifier))
                    .findFirst()
                    .map(PlayerTaskProgress::getCurrentProgress)
                    .orElse(0L);
            return progress.intValue();
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
     * <p>
     * This method is called periodically (every 30 seconds) to apply batched progress
     * updates to the cache. All updates happen in memory for performance.
     * </p>
     */
    private void processBatch() {
        if (pendingUpdates.isEmpty()) {
            return;
        }
        
        // Take snapshot of pending updates and clear
        Map<ProgressKey, AtomicInteger> snapshot = new ConcurrentHashMap<>(pendingUpdates);
        pendingUpdates.clear();
        
        LOGGER.fine("Processing batch of " + snapshot.size() + " progress updates");
        
        // Process each update
        snapshot.forEach((key, amount) -> {
            try {
                processProgressUpdate(key, amount.get());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing progress update for " + key, e);
            }
        });
    }
    
    /**
     * Processes a single progress update from cache.
     *
     * @param key    the progress key
     * @param amount the amount to add
     */
    private void processProgressUpdate(
            @NotNull final ProgressKey key,
            final int amount
    ) {
        // Check if player cache is loaded
        if (!progressCache.isLoaded(key.playerId())) {
            LOGGER.fine("Skipping progress update - player cache not loaded: " + key.playerId());
            return;
        }
        
        // Find quest progress from cache
        PlayerQuestProgress questProgress = findQuestProgressByIdentifier(
            key.playerId(), 
            key.questIdentifier()
        );
        
        if (questProgress == null) {
            LOGGER.fine("Skipping progress update - quest not active: " + key.questIdentifier());
            return;
        }
        
        // Find task progress
        Optional<PlayerTaskProgress> taskProgressOpt = questProgress.getTaskProgress().stream()
                .filter(tp -> tp.getTask().getIdentifier().equalsIgnoreCase(key.taskIdentifier()))
                .findFirst();
        
        if (taskProgressOpt.isEmpty()) {
            LOGGER.warning("Task not found: " + key.taskIdentifier());
            return;
        }
        
        PlayerTaskProgress taskProgress = taskProgressOpt.get();
        
        // Skip if already completed
        if (taskProgress.isCompleted()) {
            return;
        }
        
        // Update progress
        long newProgress = Math.min(
                taskProgress.getCurrentProgress() + amount,
                taskProgress.getRequiredProgress()
        );
        
        taskProgress.setCurrentProgress(newProgress);
        
        // Check if task should be completed
        if (newProgress >= taskProgress.getRequiredProgress()) {
            taskProgress.setCompleted(true);
            taskProgress.setCompletedAt(Instant.now());
            
            // Fire task complete event
            fireTaskCompleteEvent(key.playerId(), questProgress.getQuest(), key.taskIdentifier());
            
            // TODO: Distribute task rewards
            
            // Check if quest should be completed
            checkQuestCompletion(key.playerId(), questProgress);
        }
        
        // Update in cache (marks as dirty)
        progressCache.updateProgress(key.playerId(), questProgress);
    }
    
    /**
     * Checks if a quest should be completed and completes it if so.
     *
     * @param playerId      the player's unique identifier
     * @param questProgress the quest progress
     */
    private void checkQuestCompletion(
            @NotNull final UUID playerId,
            @NotNull final PlayerQuestProgress questProgress
    ) {
        // Check if all tasks are completed
        if (questProgress.areAllTasksCompleted() && !questProgress.isCompleted()) {
            completeQuest(playerId, questProgress.getQuest().getIdentifier()).join();
        }
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
        return completionHistoryRepository.findLatestByPlayerAndQuest(playerId, quest.getId())
                .thenCompose(historyOpt -> {
                    QuestCompletionHistory history;
                    
                    if (historyOpt.isPresent()) {
                        // Update existing history
                        history = historyOpt.get();
                        history.setCompletionCount(history.getCompletionCount() + 1);
                        history.setCompletedAt(java.time.LocalDateTime.now());
                        // Note: Cooldown tracking would need to be added to the entity if needed
                    } else {
                        // Create new history - need to get RDQPlayer first
                        // For now, create a simple record
                        // TODO: Properly integrate with RDQPlayer repository
                        history = new QuestCompletionHistory(
                                null, // RDQPlayer - needs to be loaded
                                quest,
                                java.time.LocalDateTime.now(),
                                1, // First completion
                                0L // Time taken - would need to track start time
                        );
                    }
                    
                    return completionHistoryRepository.createAsync(history).thenApply(saved -> null);
                });
    }
    
    /**
     * Finds quest progress by quest identifier from cache.
     *
     * @param playerId        the player's UUID
     * @param questIdentifier the quest identifier
     * @return the quest progress, or null if not found
     */
    private PlayerQuestProgress findQuestProgressByIdentifier(
            @NotNull final UUID playerId,
            @NotNull final String questIdentifier
    ) {
        return progressCache.getProgress(playerId).stream()
                .filter(qp -> qp.getQuest().getIdentifier().equalsIgnoreCase(questIdentifier))
                .findFirst()
                .orElse(null);
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
        Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                TaskCompleteEvent event = new TaskCompleteEvent(player, quest, taskIdentifier);
                Bukkit.getPluginManager().callEvent(event);
            }
        });
    }
    
    /**
     * Fires a quest complete event on the main thread.
     *
     * @param playerId  the player's unique identifier
     * @param quest     the quest
     * @param startedAt when the quest was started
     */
    private void fireQuestCompleteEvent(
            @NotNull final UUID playerId,
            @NotNull final Quest quest,
            @NotNull final Instant startedAt
    ) {
        Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                Duration completionTime = Duration.between(startedAt, Instant.now());
                QuestCompleteEvent event = new QuestCompleteEvent(player, quest, completionTime);
                Bukkit.getPluginManager().callEvent(event);
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
