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

package com.raindropcentral.core.service.statistics.queue;

import com.raindropcentral.core.service.statistics.config.StatisticsDeliveryConfig;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Manages multi-tier priority queues for statistics delivery.
 * Integrates backpressure control and disk persistence.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class StatisticsQueueManager {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RCore");

    private final Map<DeliveryPriority, ConcurrentLinkedQueue<QueuedStatistic>> queues;
    private final BackpressureController backpressureController;
    private final QueuePersistenceManager persistenceManager;
    private final AtomicInteger totalQueueSize;
    private final int maxQueueSize;

    private ScheduledFuture<?> persistenceTask;

    /**
     * Creates a new queue manager.
     *
     * @param plugin the plugin instance
     * @param config the statistics delivery configuration
     */
    public StatisticsQueueManager(
        final @NotNull Plugin plugin,
        final @NotNull StatisticsDeliveryConfig config
    ) {
        this.queues = new ConcurrentHashMap<>();
        this.backpressureController = new BackpressureController(config);
        this.persistenceManager = new QueuePersistenceManager(plugin);
        this.totalQueueSize = new AtomicInteger(0);
        this.maxQueueSize = config.getMaxQueueSize();

        // Initialize queues for all priorities
        for (DeliveryPriority priority : DeliveryPriority.values()) {
            queues.put(priority, new ConcurrentLinkedQueue<>());
        }
    }

    /**
     * Creates a queue manager with custom components (for testing).
     */
    public StatisticsQueueManager(
        final @NotNull BackpressureController backpressureController,
        final @NotNull QueuePersistenceManager persistenceManager,
        final int maxQueueSize
    ) {
        this.queues = new ConcurrentHashMap<>();
        this.backpressureController = backpressureController;
        this.persistenceManager = persistenceManager;
        this.totalQueueSize = new AtomicInteger(0);
        this.maxQueueSize = maxQueueSize;

        for (DeliveryPriority priority : DeliveryPriority.values()) {
            queues.put(priority, new ConcurrentLinkedQueue<>());
        }
    }

    /**
     * Initializes the queue manager, loading persisted data.
     */
    public void initialize() {
        LOGGER.info("Initializing statistics queue manager...");

        // Validate and load persisted queues
        persistenceManager.validateAndRepair();
        Map<DeliveryPriority, List<QueuedStatistic>> loaded = persistenceManager.load();

        int loadedCount = 0;
        for (var entry : loaded.entrySet()) {
            ConcurrentLinkedQueue<QueuedStatistic> queue = queues.get(entry.getKey());
            queue.addAll(entry.getValue());
            loadedCount += entry.getValue().size();
        }

        totalQueueSize.set(loadedCount);
        backpressureController.onQueueSizeChanged(loadedCount);

        LOGGER.info("Queue manager initialized with " + loadedCount + " persisted statistics");
    }

    /**
     * Starts periodic persistence.
     *
     * @param executor the executor for scheduling
     * @param intervalSeconds the persistence interval in seconds
     */
    public void startPersistence(
        final @NotNull ScheduledExecutorService executor,
        final int intervalSeconds
    ) {
        persistenceTask = executor.scheduleAtFixedRate(
            this::persistToDisk,
            intervalSeconds,
            intervalSeconds,
            TimeUnit.SECONDS
        );
        LOGGER.info("Queue persistence scheduled every " + intervalSeconds + " seconds");
    }

    /**
     * Stops periodic persistence.
     */
    public void stopPersistence() {
        if (persistenceTask != null) {
            persistenceTask.cancel(false);
            persistenceTask = null;
        }
    }

    /**
     * Shuts down the queue manager, persisting all data.
     */
    public void shutdown() {
        stopPersistence();
        persistToDisk();
        LOGGER.info("Queue manager shut down");
    }

    /**
     * Enqueues a single statistic.
     *
     * @param statistic the statistic to enqueue
     * @return true if enqueued, false if rejected due to capacity
     */
    public boolean enqueue(final @NotNull QueuedStatistic statistic) {
        // Check capacity
        if (totalQueueSize.get() >= maxQueueSize) {
            if (!discardLowPriority()) {
                LOGGER.warning("Queue at capacity, rejecting statistic: " + statistic.statisticKey());
                return false;
            }
        }

        ConcurrentLinkedQueue<QueuedStatistic> queue = queues.get(statistic.priority());
        queue.offer(statistic);

        int newSize = totalQueueSize.incrementAndGet();
        backpressureController.onQueueSizeChanged(newSize);

        // Append to WAL for durability
        persistenceManager.appendToLog(statistic);

        return true;
    }

    /**
     * Enqueues a batch of statistics.
     *
     * @param statistics the statistics to enqueue
     * @return the number of statistics successfully enqueued
     */
    public int enqueueBatch(final @NotNull Collection<QueuedStatistic> statistics) {
        int enqueued = 0;
        for (QueuedStatistic stat : statistics) {
            if (enqueue(stat)) {
                enqueued++;
            }
        }
        return enqueued;
    }

    /**
     * Dequeues statistics of a specific priority.
     *
     * @param priority the priority to dequeue
     * @param maxCount maximum number to dequeue
     * @return the dequeued statistics
     */
    public List<QueuedStatistic> dequeue(
        final @NotNull DeliveryPriority priority,
        final int maxCount
    ) {
        ConcurrentLinkedQueue<QueuedStatistic> queue = queues.get(priority);
        List<QueuedStatistic> result = new ArrayList<>(Math.min(maxCount, queue.size()));

        for (int i = 0; i < maxCount; i++) {
            QueuedStatistic stat = queue.poll();
            if (stat == null) break;
            result.add(stat);
        }

        if (!result.isEmpty()) {
            int newSize = totalQueueSize.addAndGet(-result.size());
            backpressureController.onQueueSizeChanged(newSize);
        }

        return result;
    }

    /**
     * Dequeues all statistics for a specific player.
     *
     * @param playerUuid the player UUID
     * @param maxCount maximum number to dequeue
     * @return the dequeued statistics
     */
    public List<QueuedStatistic> dequeueByPlayer(
        final @NotNull UUID playerUuid,
        final int maxCount
    ) {
        List<QueuedStatistic> result = new ArrayList<>();

        for (DeliveryPriority priority : DeliveryPriority.values()) {
            ConcurrentLinkedQueue<QueuedStatistic> queue = queues.get(priority);
            Iterator<QueuedStatistic> iterator = queue.iterator();

            while (iterator.hasNext() && result.size() < maxCount) {
                QueuedStatistic stat = iterator.next();
                if (stat.playerUuid().equals(playerUuid)) {
                    iterator.remove();
                    result.add(stat);
                }
            }
        }

        if (!result.isEmpty()) {
            int newSize = totalQueueSize.addAndGet(-result.size());
            backpressureController.onQueueSizeChanged(newSize);
        }

        return result;
    }

    /**
     * Dequeues statistics across all priorities, respecting priority order.
     *
     * @param maxCount maximum number to dequeue
     * @return the dequeued statistics
     */
    public List<QueuedStatistic> dequeueByPriority(final int maxCount) {
        List<QueuedStatistic> result = new ArrayList<>();

        for (DeliveryPriority priority : DeliveryPriority.values()) {
            if (result.size() >= maxCount) break;

            int remaining = maxCount - result.size();
            result.addAll(dequeue(priority, remaining));
        }

        return result;
    }

    /**
     * Persists all queues to disk.
     */
    public void persistToDisk() {
        persistenceManager.persist(queues);
    }

    /**
     * Loads queues from disk.
     */
    public void loadFromDisk() {
        Map<DeliveryPriority, List<QueuedStatistic>> loaded = persistenceManager.load();

        // Clear existing queues
        for (ConcurrentLinkedQueue<QueuedStatistic> queue : queues.values()) {
            queue.clear();
        }

        // Load persisted data
        int loadedCount = 0;
        for (var entry : loaded.entrySet()) {
            queues.get(entry.getKey()).addAll(entry.getValue());
            loadedCount += entry.getValue().size();
        }

        totalQueueSize.set(loadedCount);
        backpressureController.onQueueSizeChanged(loadedCount);
    }

    /**
     * Validates and repairs queue integrity.
     */
    public void validateAndRepair() {
        persistenceManager.validateAndRepair();
    }

    /**
     * Discards low-priority entries to make room.
     *
     * @return true if entries were discarded
     */
    private boolean discardLowPriority() {
        // Try BULK first, then LOW
        for (DeliveryPriority priority : new DeliveryPriority[]{DeliveryPriority.BULK, DeliveryPriority.LOW}) {
            ConcurrentLinkedQueue<QueuedStatistic> queue = queues.get(priority);
            if (!queue.isEmpty()) {
                QueuedStatistic discarded = queue.poll();
                if (discarded != null) {
                    totalQueueSize.decrementAndGet();
                    LOGGER.warning("Discarded " + priority + " priority statistic due to capacity: " +
                        discarded.statisticKey());
                    return true;
                }
            }
        }
        return false;
    }

    // ==================== Status Methods ====================

    /**
     * Returns whether backpressureActive.
     */
    public boolean isBackpressureActive() {
        return backpressureController.isBackpressureActive();
    }

    public BackpressureLevel getBackpressureLevel() {
        return backpressureController.getCurrentLevel();
    }

    /**
     * Gets queueSize.
     */
    public int getQueueSize(final @NotNull DeliveryPriority priority) {
        return queues.get(priority).size();
    }

    /**
     * Gets totalQueueSize.
     */
    public int getTotalQueueSize() {
        return totalQueueSize.get();
    }

    /**
     * Gets backpressureController.
     */
    public BackpressureController getBackpressureController() {
        return backpressureController;
    }

    /**
     * Gets queue statistics for monitoring.
     *
     * @return the queue statistics
     */
    public QueueStatistics getStatistics() {
        Map<DeliveryPriority, Integer> sizeByPriority = new EnumMap<>(DeliveryPriority.class);
        long oldestTimestamp = Long.MAX_VALUE;

        for (DeliveryPriority priority : DeliveryPriority.values()) {
            ConcurrentLinkedQueue<QueuedStatistic> queue = queues.get(priority);
            sizeByPriority.put(priority, queue.size());

            QueuedStatistic oldest = queue.peek();
            if (oldest != null && oldest.collectionTimestamp() < oldestTimestamp) {
                oldestTimestamp = oldest.collectionTimestamp();
            }
        }

        long oldestAgeMs = oldestTimestamp == Long.MAX_VALUE ? 0 :
            System.currentTimeMillis() - oldestTimestamp;

        return new QueueStatistics(
            sizeByPriority,
            totalQueueSize.get(),
            oldestAgeMs,
            backpressureController.getCurrentLevel()
        );
    }

    /**
     * Queue statistics record.
     */
    public record QueueStatistics(
        Map<DeliveryPriority, Integer> sizeByPriority,
        int totalSize,
        long oldestEntryAgeMs,
        BackpressureLevel backpressureLevel
    ) {}

    // ==================== Offline Mode ====================

    private volatile boolean offlineMode = false;

    /**
     * Sets offline mode, which increases queue capacity for buffering.
     *
     * @param offline true to enable offline mode
     */
    public void setOfflineMode(final boolean offline) {
        this.offlineMode = offline;
        if (offline) {
            LOGGER.info("Queue manager entering offline mode - increased buffering capacity");
        } else {
            LOGGER.info("Queue manager exiting offline mode");
        }
    }

    /**
     * Checks if offline mode is active.
     *
     * @return true if in offline mode
     */
    public boolean isOfflineMode() {
        return offlineMode;
    }
}
