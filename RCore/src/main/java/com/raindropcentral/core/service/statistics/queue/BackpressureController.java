package com.raindropcentral.core.service.statistics.queue;

import com.raindropcentral.core.service.statistics.config.StatisticsDeliveryConfig;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Controls backpressure based on queue size to prevent memory exhaustion.
 * Throttles low-priority collection when queues approach capacity.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class BackpressureController {

    private static final Logger LOGGER = CentralLogger.getLogger(BackpressureController.class);

    private final int warningThreshold;
    private final int criticalThreshold;
    private final int maxCapacity;

    private final AtomicReference<BackpressureLevel> currentLevel;
    private volatile int lastQueueSize;

    /**
     * Creates a backpressure controller with default thresholds.
     */
    public BackpressureController() {
        this(5000, 7500, 10000);
    }

    /**
     * Creates a backpressure controller with custom thresholds.
     *
     * @param warningThreshold  queue size to trigger WARNING level
     * @param criticalThreshold queue size to trigger CRITICAL level
     * @param maxCapacity       queue size to trigger OVERFLOW level
     */
    public BackpressureController(
        final int warningThreshold,
        final int criticalThreshold,
        final int maxCapacity
    ) {
        this.warningThreshold = warningThreshold;
        this.criticalThreshold = criticalThreshold;
        this.maxCapacity = maxCapacity;
        this.currentLevel = new AtomicReference<>(BackpressureLevel.NONE);
        this.lastQueueSize = 0;
    }

    /**
     * Creates a backpressure controller from configuration.
     *
     * @param config the statistics delivery configuration
     */
    public BackpressureController(final @NotNull StatisticsDeliveryConfig config) {
        this(
            config.getBackpressureWarningThreshold(),
            config.getBackpressureCriticalThreshold(),
            config.getMaxQueueSize()
        );
    }

    /**
     * Evaluates and updates the backpressure level based on queue size.
     *
     * @param queueSize the current queue size
     * @return the new backpressure level
     */
    public BackpressureLevel evaluate(final int queueSize) {
        BackpressureLevel newLevel;

        if (queueSize >= maxCapacity) {
            newLevel = BackpressureLevel.OVERFLOW;
        } else if (queueSize >= criticalThreshold) {
            newLevel = BackpressureLevel.CRITICAL;
        } else if (queueSize >= warningThreshold) {
            newLevel = BackpressureLevel.WARNING;
        } else {
            newLevel = BackpressureLevel.NONE;
        }

        BackpressureLevel oldLevel = currentLevel.getAndSet(newLevel);
        this.lastQueueSize = queueSize;

        // Log level transitions
        if (oldLevel != newLevel) {
            logLevelTransition(oldLevel, newLevel, queueSize);
        }

        return newLevel;
    }

    /**
     * Called when queue size changes to re-evaluate backpressure.
     *
     * @param newSize the new queue size
     */
    public void onQueueSizeChanged(final int newSize) {
        evaluate(newSize);
    }

    /**
     * Checks if collection should proceed for the given priority.
     *
     * @param priority the delivery priority to check
     * @return true if collection should proceed
     */
    public boolean shouldCollect(final @NotNull DeliveryPriority priority) {
        BackpressureLevel level = currentLevel.get();

        // High priority always collected
        if (!priority.isThrottleable()) {
            return true;
        }

        // Throttleable priorities may be paused
        if (level.shouldPauseThrottleable()) {
            return false;
        }

        // Apply probabilistic throttling based on multiplier
        double multiplier = level.getCollectionMultiplier();
        if (multiplier >= 1.0) {
            return true;
        }

        return Math.random() < multiplier;
    }

    /**
     * Gets the collection rate multiplier for throttleable priorities.
     *
     * @return multiplier between 0.0 and 1.0
     */
    public double getCollectionRateMultiplier() {
        return currentLevel.get().getCollectionMultiplier();
    }

    /**
     * Gets the current backpressure level.
     *
     * @return the current level
     */
    public BackpressureLevel getCurrentLevel() {
        return currentLevel.get();
    }

    /**
     * Checks if backpressure is currently active.
     *
     * @return true if backpressure is active
     */
    public boolean isBackpressureActive() {
        return currentLevel.get().isActive();
    }

    /**
     * Gets the last known queue size.
     *
     * @return the last queue size
     */
    public int getLastQueueSize() {
        return lastQueueSize;
    }

    /**
     * Gets the warning threshold.
     *
     * @return the warning threshold
     */
    public int getWarningThreshold() {
        return warningThreshold;
    }

    /**
     * Gets the critical threshold.
     *
     * @return the critical threshold
     */
    public int getCriticalThreshold() {
        return criticalThreshold;
    }

    /**
     * Gets the maximum capacity.
     *
     * @return the maximum capacity
     */
    public int getMaxCapacity() {
        return maxCapacity;
    }

    private void logLevelTransition(
        final BackpressureLevel oldLevel,
        final BackpressureLevel newLevel,
        final int queueSize
    ) {
        if (newLevel.ordinal() > oldLevel.ordinal()) {
            // Escalating
            switch (newLevel) {
                case WARNING -> LOGGER.warning(
                    "Backpressure WARNING: Queue size " + queueSize + " exceeded warning threshold " + warningThreshold +
                    ". LOW/BULK collection throttled to 50%."
                );
                case CRITICAL -> LOGGER.warning(
                    "Backpressure CRITICAL: Queue size " + queueSize + " exceeded critical threshold " + criticalThreshold +
                    ". LOW/BULK collection throttled to 25%."
                );
                case OVERFLOW -> LOGGER.severe(
                    "Backpressure OVERFLOW: Queue size " + queueSize + " reached max capacity " + maxCapacity +
                    ". LOW/BULK collection PAUSED. Consider increasing delivery rate or reducing collection."
                );
                default -> {}
            }
        } else {
            // De-escalating
            LOGGER.info(
                "Backpressure reduced from " + oldLevel + " to " + newLevel +
                ". Queue size: " + queueSize
            );
        }
    }
}
