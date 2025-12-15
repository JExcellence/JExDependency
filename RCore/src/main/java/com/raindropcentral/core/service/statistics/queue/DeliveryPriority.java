package com.raindropcentral.core.service.statistics.queue;

/**
 * Priority levels for statistics delivery determining processing urgency.
 * Higher priority statistics are processed before lower priority ones.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public enum DeliveryPriority {

    /**
     * Critical priority - process within 2 seconds.
     * Used for system-critical statistics that must be delivered immediately.
     */
    CRITICAL(0, 2_000L),

    /**
     * High priority - process within 10 seconds.
     * Used for player disconnect snapshots and significant events.
     */
    HIGH(1, 10_000L),

    /**
     * Normal priority - process within the configured delivery interval.
     * Used for regular periodic statistics collection.
     */
    NORMAL(2, 300_000L),

    /**
     * Low priority - process during periods of low server activity.
     * Used for non-urgent statistics that can wait.
     */
    LOW(3, 600_000L),

    /**
     * Bulk priority - process when queue depth permits.
     * Used for historical data, offline queue recovery, and batch imports.
     */
    BULK(4, 3_600_000L);

    private final int order;
    private final long maxDelayMs;

    DeliveryPriority(final int order, final long maxDelayMs) {
        this.order = order;
        this.maxDelayMs = maxDelayMs;
    }

    /**
     * Gets the priority order (lower = higher priority).
     *
     * @return the priority order
     */
    public int getOrder() {
        return order;
    }

    /**
     * Gets the maximum delay in milliseconds before this priority must be processed.
     *
     * @return the maximum delay in milliseconds
     */
    public long getMaxDelayMs() {
        return maxDelayMs;
    }

    /**
     * Checks if this priority is higher than another.
     *
     * @param other the other priority to compare
     * @return true if this priority is higher (lower order number)
     */
    public boolean isHigherThan(final DeliveryPriority other) {
        return this.order < other.order;
    }

    /**
     * Checks if this priority should be throttled during backpressure.
     * LOW and BULK priorities are throttled first.
     *
     * @return true if this priority can be throttled
     */
    public boolean isThrottleable() {
        return this == LOW || this == BULK;
    }
}
