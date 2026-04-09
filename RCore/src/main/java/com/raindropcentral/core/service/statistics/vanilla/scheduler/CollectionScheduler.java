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

package com.raindropcentral.core.service.statistics.vanilla.scheduler;

import com.raindropcentral.core.service.statistics.vanilla.StatisticCategory;
import com.raindropcentral.core.service.statistics.vanilla.cache.StatisticCacheManager;
import com.raindropcentral.core.service.statistics.vanilla.config.VanillaStatisticConfig;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Schedules periodic collection tasks for vanilla statistics.
 *
 * <p>This scheduler manages:
 * <ul>
 *   <li>Main collection task at configured frequency</li>
 *   <li>Category-specific tasks if different frequencies are configured</li>
 *   <li>Cache persistence task every 5 minutes</li>
 * </ul>
 *
 * <p>The scheduler uses a {@link ScheduledExecutorService} for reliable periodic execution
 * and integrates with {@link TPSThrottler} for performance-aware collection.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * CollectionScheduler scheduler = new CollectionScheduler(
 *     config, cacheManager, throttler
 * );
 * 
 * // Set collection callback
 * scheduler.setCollectionCallback(() -> {
 *     // Perform collection
 * });
 * 
 * // Start scheduling
 * scheduler.start();
 * 
 * // Stop scheduling
 * scheduler.shutdown();
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class CollectionScheduler {

    private static final Logger LOGGER = Logger.getLogger(CollectionScheduler.class.getName());
    private static final int CACHE_PERSISTENCE_INTERVAL_MINUTES = 5;

    private final VanillaStatisticConfig config;
    private final StatisticCacheManager cacheManager;
    private final TPSThrottler throttler;

    private final ScheduledExecutorService scheduler;
    private final Map<StatisticCategory, ScheduledFuture<?>> categoryTasks;
    
    private ScheduledFuture<?> mainCollectionTask;
    private ScheduledFuture<?> cachePersistenceTask;
    
    private Runnable collectionCallback;
    private final Map<StatisticCategory, Runnable> categoryCallbacks;

    private volatile boolean running;

    /**
     * Creates a new collection scheduler.
     *
     * @param config       the vanilla statistic configuration
     * @param cacheManager the cache manager for persistence scheduling
     * @param throttler    the TPS throttler for performance-aware collection
     */
    public CollectionScheduler(
        final @NotNull VanillaStatisticConfig config,
        final @NotNull StatisticCacheManager cacheManager,
        final @NotNull TPSThrottler throttler
    ) {
        this.config = config;
        this.cacheManager = cacheManager;
        this.throttler = throttler;
        this.scheduler = Executors.newScheduledThreadPool(
            2,
            r -> {
                Thread thread = new Thread(r, "VanillaStats-Scheduler");
                thread.setDaemon(true);
                return thread;
            }
        );
        this.categoryTasks = new EnumMap<>(StatisticCategory.class);
        this.categoryCallbacks = new EnumMap<>(StatisticCategory.class);
        this.running = false;
    }

    /**
     * Sets the callback to execute for main collection tasks.
     *
     * <p>This callback is invoked at the configured collection frequency,
     * subject to TPS throttling. It should perform the actual statistic
     * collection for all enabled categories.
     *
     * @param callback the collection callback
     */
    public void setCollectionCallback(final @NotNull Runnable callback) {
        this.collectionCallback = callback;
    }

    /**
     * Sets the callback to execute for a specific category collection.
     *
     * <p>This callback is only used if the category has a different frequency
     * configured than the main collection frequency.
     *
     * @param category the statistic category
     * @param callback the category-specific collection callback
     */
    public void setCategoryCallback(
        final @NotNull StatisticCategory category,
        final @NotNull Runnable callback
    ) {
        this.categoryCallbacks.put(category, callback);
    }

    /**
     * Starts all scheduled tasks.
     *
     * <p>This method schedules:
     * <ul>
     *   <li>Main collection task at configured frequency</li>
     *   <li>Category-specific tasks for categories with different frequencies</li>
     *   <li>Cache persistence task every 5 minutes</li>
     * </ul>
     *
     * <p>If already running, this method does nothing.
     */
    public void start() {
        if (running) {
            LOGGER.warning("Collection scheduler is already running");
            return;
        }

        if (collectionCallback == null) {
            throw new IllegalStateException("Collection callback must be set before starting");
        }

        running = true;

        // Schedule main collection task
        scheduleMainCollection();

        // Schedule category-specific tasks
        scheduleCategoryCollections();

        // Schedule cache persistence
        scheduleCachePersistence();
    }

    /**
     * Schedules the main collection task at the configured frequency.
     */
    private void scheduleMainCollection() {
        int frequencySeconds = config.getCollectionFrequencySeconds();
        
        mainCollectionTask = scheduler.scheduleAtFixedRate(
            this::executeMainCollection,
            frequencySeconds,
            frequencySeconds,
            TimeUnit.SECONDS
        );

        LOGGER.fine("Scheduled main collection task every " + frequencySeconds + " seconds");
    }

    /**
     * Schedules category-specific collection tasks for categories with different frequencies.
     */
    private void scheduleCategoryCollections() {
        int mainFrequency = config.getCollectionFrequencySeconds();

        for (StatisticCategory category : StatisticCategory.values()) {
            if (!config.isCategoryEnabled(category)) {
                continue;
            }

            int categoryFrequency = config.getCategoryFrequency(category);
            
            // Only schedule separate task if frequency differs from main
            if (categoryFrequency != mainFrequency) {
                Runnable callback = categoryCallbacks.get(category);
                if (callback != null) {
                    ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(
                        () -> executeCategoryCollection(category, callback),
                        categoryFrequency,
                        categoryFrequency,
                        TimeUnit.SECONDS
                    );
                    
                    categoryTasks.put(category, task);
                    
                    LOGGER.fine("Scheduled " + category + " collection task every " + 
                               categoryFrequency + " seconds");
                }
            }
        }
    }

    /**
     * Schedules the cache persistence task every 5 minutes.
     */
    private void scheduleCachePersistence() {
        cachePersistenceTask = scheduler.scheduleAtFixedRate(
            this::executeCachePersistence,
            CACHE_PERSISTENCE_INTERVAL_MINUTES,
            CACHE_PERSISTENCE_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );

        LOGGER.fine("Scheduled cache persistence task every " + 
                   CACHE_PERSISTENCE_INTERVAL_MINUTES + " minutes");
    }

    /**
     * Executes the main collection task with TPS throttling.
     */
    private void executeMainCollection() {
        if (!running) {
            return;
        }

        // Check TPS throttling
        if (!throttler.shouldCollect()) {
            LOGGER.fine("Skipping main collection due to TPS throttling");
            return;
        }

        try {
            collectionCallback.run();
        } catch (Exception e) {
            LOGGER.severe("Error during main collection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Executes a category-specific collection task with TPS throttling.
     *
     * @param category the category being collected
     * @param callback the callback to execute
     */
    private void executeCategoryCollection(
        final @NotNull StatisticCategory category,
        final @NotNull Runnable callback
    ) {
        if (!running) {
            return;
        }

        // Check TPS throttling
        if (!throttler.shouldCollect()) {
            LOGGER.fine("Skipping " + category + " collection due to TPS throttling");
            return;
        }

        try {
            callback.run();
        } catch (Exception e) {
            LOGGER.severe("Error during " + category + " collection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Executes the cache persistence task.
     */
    private void executeCachePersistence() {
        if (!running) {
            return;
        }

        try {
            cacheManager.persistCache();
        } catch (Exception e) {
            LOGGER.severe("Error during cache persistence: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Shuts down the scheduler and cancels all scheduled tasks.
     *
     * <p>This method attempts a graceful shutdown, waiting up to 5 seconds
     * for running tasks to complete. If tasks don't complete in time,
     * a forced shutdown is performed.
     *
     * <p>After shutdown, the scheduler cannot be restarted.
     */
    public void shutdown() {
        if (!running) {
            return;
        }

        running = false;

        // Cancel all tasks
        if (mainCollectionTask != null) {
            mainCollectionTask.cancel(false);
        }

        for (ScheduledFuture<?> task : categoryTasks.values()) {
            task.cancel(false);
        }

        if (cachePersistenceTask != null) {
            cachePersistenceTask.cancel(false);
        }

        // Shutdown executor
        scheduler.shutdown();
        
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                LOGGER.warning("Forced shutdown of collection scheduler");
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Checks if the scheduler is currently running.
     *
     * @return true if the scheduler is running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Stops the scheduler.
     * Alias for shutdown() for backward compatibility.
     */
    public void stop() {
        shutdown();
    }

    /**
     * Gets the number of active scheduled tasks.
     *
     * @return the number of active tasks
     */
    public int getActiveTaskCount() {
        int count = 0;
        
        if (mainCollectionTask != null && !mainCollectionTask.isDone()) {
            count++;
        }
        
        if (cachePersistenceTask != null && !cachePersistenceTask.isDone()) {
            count++;
        }
        
        count += (int) categoryTasks.values().stream()
            .filter(task -> !task.isDone())
            .count();
        
        return count;
    }

    /**
     * Gets the TPS throttler used by this scheduler.
     *
     * @return the TPS throttler
     */
    public @NotNull TPSThrottler getThrottler() {
        return throttler;
    }
}
