package com.raindropcentral.rdq.quest.handler;

import com.raindropcentral.rdq.cache.quest.PlayerQuestProgressCache;
import com.raindropcentral.rdq.cache.quest.QuestCacheManager;
import com.raindropcentral.rdq.database.entity.quest.QuestTask;
import com.raindropcentral.rdq.database.entity.quest.QuestUser;
import com.raindropcentral.rdq.service.quest.QuestProgressTracker;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for all quest task handlers.
 * <p>
 * Task handlers listen to Bukkit events and update quest progress when players
 * perform relevant actions. This base class provides common functionality for:
 * <ul>
 *   <li>Eligibility checking (creative mode, disabled worlds)</li>
 *   <li>Progress update logic with cache integration</li>
 *   <li>Criteria matching logic</li>
 *   <li>Error handling and logging</li>
 * </ul>
 * <p>
 * Subclasses must implement:
 * <ul>
 *   <li>{@link #getTaskType()} - The task type this handler processes</li>
 *   <li>{@link #shouldProcess(Event, Player)} - Whether to process the event</li>
 *   <li>{@link #extractCriteria(Event)} - Extract task criteria from the event</li>
 * </ul>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public abstract class BaseTaskHandler implements Listener {
    
    protected final Logger logger;
    protected final QuestProgressTracker progressTracker;
    protected final QuestCacheManager cacheManager;
    protected final PlayerQuestProgressCache progressCache;
    
    /**
     * Performance metrics: total events processed.
     */
    private final AtomicLong eventsProcessed = new AtomicLong(0);
    
    /**
     * Performance metrics: total events skipped (early exit).
     */
    private final AtomicLong eventsSkipped = new AtomicLong(0);
    
    /**
     * Performance metrics: total processing time in nanoseconds.
     */
    private final AtomicLong totalProcessingTimeNanos = new AtomicLong(0);
    
    /**
     * Performance metrics: maximum processing time in nanoseconds.
     */
    private final AtomicLong maxProcessingTimeNanos = new AtomicLong(0);
    
    /**
     * Constructs a new base task handler.
     *
     * @param progressTracker the quest progress tracker
     * @param cacheManager    the quest cache manager
     * @param progressCache   the player quest progress cache
     */
    protected BaseTaskHandler(
            @NotNull final QuestProgressTracker progressTracker,
            @NotNull final QuestCacheManager cacheManager,
            @NotNull final PlayerQuestProgressCache progressCache
    ) {
        this.logger = CentralLogger.getLoggerByName("RDQ");
        this.progressTracker = progressTracker;
        this.cacheManager = cacheManager;
        this.progressCache = progressCache;
    }
    
    /**
     * Gets the task type this handler processes.
     * <p>
     * This should match the task type defined in quest configurations
     * (e.g., "KILL_MOBS", "COLLECT_ITEMS", "CRAFT_ITEMS").
     *
     * @return the task type identifier
     */
    @NotNull
    protected abstract String getTaskType();
    
    /**
     * Checks if the event should be processed for the given player.
     * <p>
     * This method should perform event-specific validation, such as:
     * <ul>
     *   <li>Checking if the entity is the correct type</li>
     *   <li>Checking if the item matches criteria</li>
     *   <li>Checking if the action is valid</li>
     * </ul>
     *
     * @param event  the Bukkit event
     * @param player the player who triggered the event
     * @return true if the event should be processed
     */
    protected abstract boolean shouldProcess(@NotNull Event event, @NotNull Player player);
    
    /**
     * Extracts task criteria from the event.
     * <p>
     * The criteria map should contain key-value pairs that can be matched
     * against task requirements. Common keys include:
     * <ul>
     *   <li>"entity_type" - For mob kills</li>
     *   <li>"material" - For item collection/crafting</li>
     *   <li>"block_type" - For block breaking/placing</li>
     *   <li>"world" - For location-based tasks</li>
     * </ul>
     *
     * @param event the Bukkit event
     * @return a map of criteria key-value pairs
     */
    @NotNull
    protected abstract Map<String, Object> extractCriteria(@NotNull Event event);
    
    /**
     * Checks if a player is eligible for quest progress.
     * <p>
     * Players are ineligible if:
     * <ul>
     *   <li>They are in creative mode (for survival-only tasks)</li>
     *   <li>They are in a disabled world</li>
     *   <li>They have no active quests</li>
     * </ul>
     * <p>
     * This method implements early exit optimization by checking if the player
     * has any active quests before processing the event. This significantly
     * improves performance when many players have no active quests.
     *
     * @param player the player to check
     * @return true if the player is eligible
     */
    protected boolean isEligible(@NotNull final Player player) {
        
        // Spectators and creative mode players don't get quest progress
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
            return false;
        }
        
        // Check if player has any active quests in cache
        final boolean cacheLoaded = cacheManager.isLoaded(player.getUniqueId());
        
        if (!cacheLoaded) {
            return false;
        }
        
        final List<QuestUser> activeQuests = cacheManager.getPlayerQuests(player.getUniqueId());
        
        if (activeQuests.isEmpty()) {
            return false;
        }
        
        // Log quest details
        for (QuestUser questUser : activeQuests) {
            org.bukkit.Bukkit.getLogger().info("[" + getTaskType() + "]   Quest: " + questUser.getQuest().getIdentifier() + 
                    " (tasks: " + questUser.getTaskProgress().size() + ")");
        }

        // TODO: Check disabled worlds from configuration
        return true;
    }
    
    /**
     * Updates progress for all matching active quests.
     * <p>
     * This method:
     * <ol>
     *   <li>Gets all active quests for the player from cache</li>
     *   <li>Filters quests by task type</li>
     *   <li>Matches tasks against criteria</li>
     *   <li>Updates progress via the tracker</li>
     * </ol>
     *
     * @param player   the player
     * @param criteria the task criteria to match
     * @param amount   the amount to add to progress
     */
    protected void updateProgress(
            @NotNull final Player player,
            @NotNull final Map<String, Object> criteria,
            final int amount
    ) {
        if (amount <= 0) {
            return;
        }
        
        try {
            // Add task type to criteria
            final Map<String, Object> fullCriteria = new HashMap<>(criteria);
            fullCriteria.put("task_type", getTaskType());
            
            // Use the new updateTaskProgress method
            progressTracker.updateTaskProgress(
                    player.getUniqueId(),
                    getTaskType(),
                    fullCriteria,
                    amount
            ).thenAccept(completedTasks -> {
                if (!completedTasks.isEmpty()) {
                    logger.fine("Completed tasks for " + player.getName() + ": " + 
                            String.join(", ", completedTasks));
                }
            }).exceptionally(ex -> {
                logger.log(Level.WARNING, "Failed to update progress for " + player.getName(), ex);
                return null;
            });
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error updating quest progress for " + player.getName(), e);
        }
    }
    
    /**
     * Checks if a task matches the given criteria.
     * <p>
     * This method delegates to the QuestProgressTracker for consistent
     * criteria matching logic across all task handlers.
     *
     * @param task     the quest task
     * @param criteria the criteria to match
     * @return true if the task matches the criteria
     */
    protected boolean matchesCriteria(
            @NotNull final QuestTask task,
            @NotNull final Map<String, Object> criteria
    ) {
        return progressTracker.matchesCriteria(task, criteria);
    }
    
    /**
     * Handles an event and updates quest progress if applicable.
     * <p>
     * This is a convenience method that combines eligibility checking,
     * event processing, criteria extraction, and progress updating.
     * <p>
     * This method includes performance tracking and early exit optimizations:
     * <ul>
     *   <li>Tracks processing time for performance metrics</li>
     *   <li>Skips processing if player has no active quests</li>
     *   <li>Uses cached data instead of database queries</li>
     * </ul>
     *
     * @param event  the Bukkit event
     * @param player the player who triggered the event
     */
    protected void handleEvent(@NotNull final Event event, @NotNull final Player player) {
        final long startTime = System.nanoTime();
        
        try {
            // Early exit: Check eligibility (includes active quest check)
            if (!isEligible(player)) {
                eventsSkipped.incrementAndGet();
                return;
            }
            
            // Check if event should be processed
            if (!shouldProcess(event, player)) {
                eventsSkipped.incrementAndGet();
                return;
            }

            
            // Extract criteria
            final Map<String, Object> criteria = extractCriteria(event);
            
            // Update progress
            updateProgress(player, criteria, 1);
            
            // Track successful processing
            eventsProcessed.incrementAndGet();
            
        } finally {
            // Track processing time
            final long processingTime = System.nanoTime() - startTime;
            totalProcessingTimeNanos.addAndGet(processingTime);
            
            // Update max processing time
            long currentMax;
            do {
                currentMax = maxProcessingTimeNanos.get();
                if (processingTime <= currentMax) {
                    break;
                }
            } while (!maxProcessingTimeNanos.compareAndSet(currentMax, processingTime));
        }
    }
    
    /**
     * Handles an event with a custom amount and updates quest progress if applicable.
     * <p>
     * This method includes performance tracking and early exit optimizations.
     *
     * @param event  the Bukkit event
     * @param player the player who triggered the event
     * @param amount the amount to add to progress
     */
    protected void handleEvent(
            @NotNull final Event event,
            @NotNull final Player player,
            final int amount
    ) {
        final long startTime = System.nanoTime();
        
        try {
            // Early exit: Check eligibility (includes active quest check)
            if (!isEligible(player)) {
                eventsSkipped.incrementAndGet();
                return;
            }
            
            // Check if event should be processed
            if (!shouldProcess(event, player)) {
                eventsSkipped.incrementAndGet();
                return;
            }
            
            // Extract criteria
            final Map<String, Object> criteria = extractCriteria(event);
            
            // Update progress
            updateProgress(player, criteria, amount);
            
            // Track successful processing
            eventsProcessed.incrementAndGet();
            
        } finally {
            // Track processing time
            final long processingTime = System.nanoTime() - startTime;
            totalProcessingTimeNanos.addAndGet(processingTime);
            
            // Update max processing time
            long currentMax;
            do {
                currentMax = maxProcessingTimeNanos.get();
                if (processingTime <= currentMax) {
                    break;
                }
            } while (!maxProcessingTimeNanos.compareAndSet(currentMax, processingTime));
        }
    }
    
    /**
     * Gets performance metrics for this task handler.
     * <p>
     * Returns a map containing:
     * <ul>
     *   <li>events_processed - Total events successfully processed</li>
     *   <li>events_skipped - Total events skipped (early exit)</li>
     *   <li>avg_processing_time_ms - Average processing time in milliseconds</li>
     *   <li>max_processing_time_ms - Maximum processing time in milliseconds</li>
     *   <li>total_events - Total events received</li>
     * </ul>
     *
     * @return a map of performance metrics
     */
    @NotNull
    public Map<String, Object> getPerformanceMetrics() {
        final Map<String, Object> metrics = new HashMap<>();
        
        final long processed = eventsProcessed.get();
        final long skipped = eventsSkipped.get();
        final long totalEvents = processed + skipped;
        final long totalTimeNanos = totalProcessingTimeNanos.get();
        final long maxTimeNanos = maxProcessingTimeNanos.get();
        
        metrics.put("task_type", getTaskType());
        metrics.put("events_processed", processed);
        metrics.put("events_skipped", skipped);
        metrics.put("total_events", totalEvents);
        
        if (processed > 0) {
            final double avgTimeMs = (totalTimeNanos / (double) processed) / 1_000_000.0;
            metrics.put("avg_processing_time_ms", String.format(Locale.US, "%.3f", avgTimeMs));
        } else {
            metrics.put("avg_processing_time_ms", "0.000");
        }
        
        final double maxTimeMs = maxTimeNanos / 1_000_000.0;
        metrics.put("max_processing_time_ms", String.format(Locale.US, "%.3f", maxTimeMs));
        
        if (totalEvents > 0) {
            final double skipRate = (skipped / (double) totalEvents) * 100.0;
            metrics.put("skip_rate_percent", String.format(Locale.US, "%.1f", skipRate));
        } else {
            metrics.put("skip_rate_percent", "0.0");
        }
        
        return metrics;
    }
    
    /**
     * Resets performance metrics.
     * <p>
     * This is useful for periodic metric logging without accumulating
     * metrics over the entire server lifetime.
     */
    public void resetPerformanceMetrics() {
        eventsProcessed.set(0);
        eventsSkipped.set(0);
        totalProcessingTimeNanos.set(0);
        maxProcessingTimeNanos.set(0);
    }
    
    /**
     * Logs performance metrics at INFO level.
     * <p>
     * This method should be called periodically (e.g., every 5 minutes)
     * to monitor task handler performance.
     */
    public void logPerformanceMetrics() {
        final Map<String, Object> metrics = getPerformanceMetrics();
        
        final long totalEvents = (long) metrics.get("total_events");
        if (totalEvents == 0) {
            return; // Don't log if no events processed
        }
        
        logger.info(String.format(
                "[Performance] %s: processed=%d, skipped=%d (%.1f%%), avg=%.3fms, max=%.3fms",
                metrics.get("task_type"),
                metrics.get("events_processed"),
                metrics.get("events_skipped"),
                Double.parseDouble(metrics.get("skip_rate_percent").toString()),
                Double.parseDouble(metrics.get("avg_processing_time_ms").toString()),
                Double.parseDouble(metrics.get("max_processing_time_ms").toString())
        ));
    }
}


