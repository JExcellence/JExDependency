package com.raindropcentral.core.service.statistics.monitoring;

import com.raindropcentral.core.service.statistics.queue.BackpressureLevel;
import com.raindropcentral.core.service.statistics.queue.DeliveryPriority;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Statistics about the delivery queue state.
 *
 * @param sizeByPriority    queue size for each priority level
 * @param totalSize         total queue size across all priorities
 * @param oldestEntryAgeMs  age of oldest entry in milliseconds
 * @param backpressureLevel current backpressure level
 * @param capacityUsed      percentage of capacity used (0-100)
 * @param persistedCount    count of entries persisted to disk
 *
 * @author JExcellence
 * @since 1.0.0
 */
public record QueueStatistics(
    @NotNull Map<DeliveryPriority, Integer> sizeByPriority,
    int totalSize,
    long oldestEntryAgeMs,
    @NotNull BackpressureLevel backpressureLevel,
    double capacityUsed,
    int persistedCount
) {

    /**
     * Creates empty queue statistics.
     */
    public static QueueStatistics empty() {
        return new QueueStatistics(
            Map.of(
                DeliveryPriority.CRITICAL, 0,
                DeliveryPriority.HIGH, 0,
                DeliveryPriority.NORMAL, 0,
                DeliveryPriority.LOW, 0,
                DeliveryPriority.BULK, 0
            ),
            0, 0, BackpressureLevel.NONE, 0.0, 0
        );
    }

    /**
     * Creates a builder for constructing statistics.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if the queue is empty.
     */
    public boolean isEmpty() {
        return totalSize == 0;
    }

    /**
     * Checks if the queue is under pressure.
     */
    public boolean isUnderPressure() {
        return backpressureLevel != BackpressureLevel.NONE;
    }

    /**
     * Gets the size for a specific priority.
     */
    public int sizeFor(final DeliveryPriority priority) {
        return sizeByPriority.getOrDefault(priority, 0);
    }

    /**
     * Builder for QueueStatistics.
     */
    public static class Builder {
        private Map<DeliveryPriority, Integer> sizeByPriority = Map.of();
        private int totalSize = 0;
        private long oldestEntryAgeMs = 0;
        private BackpressureLevel backpressureLevel = BackpressureLevel.NONE;
        private double capacityUsed = 0.0;
        private int persistedCount = 0;

        /**
         * Executes sizeByPriority.
         */
        public Builder sizeByPriority(Map<DeliveryPriority, Integer> map) { this.sizeByPriority = map; return this; }
        /**
         * Executes totalSize.
         */
        public Builder totalSize(int size) { this.totalSize = size; return this; }
        /**
         * Executes oldestEntryAgeMs.
         */
        public Builder oldestEntryAgeMs(long age) { this.oldestEntryAgeMs = age; return this; }
        /**
         * Executes backpressureLevel.
         */
        public Builder backpressureLevel(BackpressureLevel level) { this.backpressureLevel = level; return this; }
        /**
         * Executes capacityUsed.
         */
        public Builder capacityUsed(double used) { this.capacityUsed = used; return this; }
        /**
         * Executes persistedCount.
         */
        public Builder persistedCount(int count) { this.persistedCount = count; return this; }

        /**
         * Executes build.
         */
        public QueueStatistics build() {
            return new QueueStatistics(
                sizeByPriority, totalSize, oldestEntryAgeMs,
                backpressureLevel, capacityUsed, persistedCount
            );
        }
    }
}
