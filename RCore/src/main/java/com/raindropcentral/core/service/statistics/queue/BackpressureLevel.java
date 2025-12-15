package com.raindropcentral.core.service.statistics.queue;

/**
 * Backpressure levels indicating queue saturation state.
 * Used to throttle collection when queues approach capacity.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public enum BackpressureLevel {

    /**
     * No backpressure - normal operation.
     * All priorities collected at full rate.
     */
    NONE(1.0),

    /**
     * Warning level - queue approaching capacity.
     * LOW and BULK priorities throttled to 50%.
     */
    WARNING(0.5),

    /**
     * Critical level - queue near capacity.
     * LOW and BULK priorities throttled to 25%.
     */
    CRITICAL(0.25),

    /**
     * Overflow level - queue at capacity.
     * LOW and BULK priorities paused entirely.
     */
    OVERFLOW(0.0);

    private final double collectionMultiplier;

    BackpressureLevel(final double collectionMultiplier) {
        this.collectionMultiplier = collectionMultiplier;
    }

    /**
     * Gets the collection rate multiplier for throttleable priorities.
     *
     * @return multiplier between 0.0 and 1.0
     */
    public double getCollectionMultiplier() {
        return collectionMultiplier;
    }

    /**
     * Checks if this level indicates active backpressure.
     *
     * @return true if backpressure is active
     */
    public boolean isActive() {
        return this != NONE;
    }

    /**
     * Checks if collection should be paused for throttleable priorities.
     *
     * @return true if collection should be paused
     */
    public boolean shouldPauseThrottleable() {
        return this == OVERFLOW;
    }
}
