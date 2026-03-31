package com.raindropcentral.rdq.quest.handler;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.cache.quest.PlayerQuestProgressCache;
import com.raindropcentral.rdq.config.quest.TaskHandlersSection;
import com.raindropcentral.rdq.cache.quest.QuestCacheManager;
import com.raindropcentral.rdq.service.quest.QuestProgressTracker;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Manager for registering and unregistering quest task handlers.
 * <p>
 * This class is responsible for:
 * <ul>
 *   <li>Creating task handler instances</li>
 *   <li>Registering event listeners for enabled handlers</li>
 *   <li>Unregistering handlers on plugin disable</li>
 *   <li>Reading configuration for enabled/disabled handlers</li>
 * </ul>
 * </p>
 * <p>
 * Task handlers can be enabled or disabled via the quest-system.yml configuration
 * under the {@code task-handlers} section.
 * </p>
 *
 * @author RaindropCentral
 * @version 1.0.0
 */
public class TaskHandlerManager {
    
    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    
    /**
     * Performance metrics logging interval in ticks (5 minutes)
     */
    private static final long METRICS_LOG_INTERVAL_TICKS = 20L * 60L * 5L;
    
    private final RDQ plugin;
    private final QuestProgressTracker progressTracker;
    private final QuestCacheManager cacheManager;
    private final PlayerQuestProgressCache progressCache;
    private final Map<String, BaseTaskHandler> handlers;
    private final TaskHandlersSection config;
    
    /**
     * Task for periodic performance metrics logging
     */
    private BukkitTask metricsTask;
    
    /**
     * Constructs a new task handler manager.
     *
     * @param plugin           the RDQ plugin instance
     * @param progressTracker  the quest progress tracker
     * @param cacheManager     the quest cache manager
     * @param progressCache    the player quest progress cache
     * @param config           the task handlers configuration section
     */
    public TaskHandlerManager(
            @NotNull final RDQ plugin,
            @NotNull final QuestProgressTracker progressTracker,
            @NotNull final QuestCacheManager cacheManager,
            @NotNull final PlayerQuestProgressCache progressCache,
            @Nullable final TaskHandlersSection config
    ) {
        this.plugin = plugin;
        this.progressTracker = progressTracker;
        this.cacheManager = cacheManager;
        this.progressCache = progressCache;
        this.handlers = new HashMap<>();
        this.config = config;
    }
    
    /**
     * Registers all task handlers.
     * <p>
     * This method creates instances of all available task handlers and registers
     * them as event listeners if they are enabled in the configuration.
     * </p>
     * <p>
     * After registration, starts a periodic task to log performance metrics.
     * </p>
     */
    public void registerHandlers() {
        LOGGER.info("Registering quest task handlers...");
        
        // Register core task handlers
        registerHandler(new KillMobsTaskHandler(progressTracker, cacheManager, progressCache));
        registerHandler(new CollectItemsTaskHandler(progressTracker, cacheManager, progressCache));
        registerHandler(new CraftItemsTaskHandler(progressTracker, cacheManager, progressCache));
        
        // Register additional task handlers
        registerHandler(new BreakBlocksTaskHandler(progressTracker, cacheManager, progressCache));
        registerHandler(new PlaceBlocksTaskHandler(progressTracker, cacheManager, progressCache));
        registerHandler(new ReachLocationTaskHandler(progressTracker, cacheManager, progressCache));
        registerHandler(new TradeWithVillagerTaskHandler(progressTracker, cacheManager, progressCache));
        registerHandler(new EnchantItemTaskHandler(progressTracker, cacheManager, progressCache));
        registerHandler(new BreedAnimalsTaskHandler(progressTracker, cacheManager, progressCache));
        registerHandler(new GainExperienceTaskHandler(progressTracker, cacheManager, progressCache));
        registerHandler(new FishItemsTaskHandler(progressTracker, cacheManager, progressCache));
        
        LOGGER.info("Registered " + handlers.size() + " quest task handlers");
        
        // Start performance metrics logging
        startPerformanceMetricsLogging();
    }
    
    /**
     * Registers a single task handler.
     * <p>
     * This method checks if the handler is enabled in the configuration before
     * registering it as an event listener.
     * </p>
     *
     * @param handler the task handler to register
     */
    private void registerHandler(@NotNull final BaseTaskHandler handler) {
        final String taskType = handler.getTaskType();
        
        // Check if handler is enabled in configuration
        if (!isHandlerEnabled(taskType)) {
            LOGGER.info("Task handler " + taskType + " is disabled in configuration");
            return;
        }
        
        // Register as event listener
        handlers.put(taskType, handler);
        plugin.getPlugin().getServer().getPluginManager().registerEvents(handler, plugin.getPlugin());
        
        LOGGER.info("Registered task handler: " + taskType);
    }
    
    /**
     * Unregisters all task handlers.
     * <p>
     * This method should be called when the plugin is being disabled to clean up
     * event listeners and prevent memory leaks.
     * </p>
     * <p>
     * Also stops the performance metrics logging task.
     * </p>
     */
    public void unregisterHandlers() {
        LOGGER.info("Unregistering quest task handlers...");
        
        // Stop performance metrics logging
        stopPerformanceMetricsLogging();
        
        // Log final metrics before unregistering
        logAllPerformanceMetrics();
        
        for (final BaseTaskHandler handler : handlers.values()) {
            HandlerList.unregisterAll(handler);
        }
        
        handlers.clear();
        LOGGER.info("Unregistered all quest task handlers");
    }
    
    /**
     * Checks if a task handler is enabled in the configuration.
     * <p>
     * Task handlers can be disabled by setting {@code task-handlers.<TYPE>.enabled}
     * to {@code false} in the quest-system.yml configuration file.
     * </p>
     *
     * @param taskType the task type identifier
     * @return true if the handler is enabled (default: true)
     */
    private boolean isHandlerEnabled(@NotNull final String taskType) {
        if (config == null) {
            // No configuration loaded, enable all handlers by default
            return true;
        }
        
        return config.isHandlerEnabled(taskType);
    }
    
    /**
     * Gets a registered task handler by type.
     *
     * @param taskType the task type identifier
     * @return the task handler, or null if not registered
     */
    @NotNull
    public BaseTaskHandler getHandler(@NotNull final String taskType) {
        return handlers.get(taskType);
    }
    
    /**
     * Gets all registered task handlers.
     *
     * @return a map of task type to handler
     */
    @NotNull
    public Map<String, BaseTaskHandler> getHandlers() {
        return new HashMap<>(handlers);
    }
    
    /**
     * Reloads task handlers based on updated configuration.
     * <p>
     * This method unregisters all current handlers and re-registers them
     * based on the new configuration. This allows handlers to be enabled
     * or disabled without restarting the server.
     * </p>
     *
     * @param newConfig the updated task handlers configuration
     */
    public void reload(@Nullable final TaskHandlersSection newConfig) {
        LOGGER.info("Reloading quest task handlers with new configuration...");
        
        // Unregister all current handlers
        unregisterHandlers();
        
        // Register handlers with new configuration
        registerHandlers();
        
        LOGGER.info("Quest task handlers reloaded successfully");
    }
    
    /**
     * Starts periodic performance metrics logging.
     * <p>
     * Logs performance metrics for all task handlers every 5 minutes.
     * This helps monitor quest system performance and identify bottlenecks.
     * </p>
     */
    private void startPerformanceMetricsLogging() {
        if (metricsTask != null) {
            metricsTask.cancel();
        }
        
        metricsTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin.getPlugin(),
                this::logAllPerformanceMetrics,
                METRICS_LOG_INTERVAL_TICKS,
                METRICS_LOG_INTERVAL_TICKS
        );
        
        LOGGER.info("Started performance metrics logging (interval: 5 minutes)");
    }
    
    /**
     * Stops periodic performance metrics logging.
     */
    private void stopPerformanceMetricsLogging() {
        if (metricsTask != null) {
            metricsTask.cancel();
            metricsTask = null;
            LOGGER.info("Stopped performance metrics logging");
        }
    }
    
    /**
     * Logs performance metrics for all registered task handlers.
     * <p>
     * This method is called periodically to monitor task handler performance.
     * It logs metrics such as:
     * <ul>
     *   <li>Events processed vs skipped</li>
     *   <li>Average processing time</li>
     *   <li>Maximum processing time</li>
     *   <li>Skip rate percentage</li>
     * </ul>
     * </p>
     */
    private void logAllPerformanceMetrics() {
        if (handlers.isEmpty()) {
            return;
        }
        
        LOGGER.info("=== Quest Task Handler Performance Metrics ===");
        
        long totalProcessed = 0;
        long totalSkipped = 0;
        double maxAvgTime = 0.0;
        double maxMaxTime = 0.0;
        
        for (final BaseTaskHandler handler : handlers.values()) {
            handler.logPerformanceMetrics();
            
            final Map<String, Object> metrics = handler.getPerformanceMetrics();
            totalProcessed += (long) metrics.get("events_processed");
            totalSkipped += (long) metrics.get("events_skipped");
            
            final double avgTime = Double.parseDouble(metrics.get("avg_processing_time_ms").toString());
            final double maxTime = Double.parseDouble(metrics.get("max_processing_time_ms").toString());
            
            if (avgTime > maxAvgTime) {
                maxAvgTime = avgTime;
            }
            if (maxTime > maxMaxTime) {
                maxMaxTime = maxTime;
            }
        }
        
        final long totalEvents = totalProcessed + totalSkipped;
        final double skipRate = totalEvents > 0 ? (totalSkipped / (double) totalEvents) * 100.0 : 0.0;
        
        LOGGER.info(String.format(
                "[Summary] Total: processed=%d, skipped=%d (%.1f%%), max_avg=%.3fms, max_peak=%.3fms",
                totalProcessed,
                totalSkipped,
                skipRate,
                maxAvgTime,
                maxMaxTime
        ));
        
        LOGGER.info("==============================================");
        
        // Reset metrics after logging (for next interval)
        for (final BaseTaskHandler handler : handlers.values()) {
            handler.resetPerformanceMetrics();
        }
    }
    
    /**
     * Gets performance metrics for all task handlers.
     * <p>
     * Returns a map of task type to performance metrics.
     * </p>
     *
     * @return a map of task type to metrics
     */
    @NotNull
    public Map<String, Map<String, Object>> getAllPerformanceMetrics() {
        final Map<String, Map<String, Object>> allMetrics = new HashMap<>();
        
        for (final Map.Entry<String, BaseTaskHandler> entry : handlers.entrySet()) {
            allMetrics.put(entry.getKey(), entry.getValue().getPerformanceMetrics());
        }
        
        return allMetrics;
    }
}
