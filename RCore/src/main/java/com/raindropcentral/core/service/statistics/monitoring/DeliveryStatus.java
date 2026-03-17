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

package com.raindropcentral.core.service.statistics.monitoring;

import com.raindropcentral.core.service.statistics.queue.BackpressureLevel;
import com.raindropcentral.core.service.statistics.queue.DeliveryPriority;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Map;

/**
 * Current status of the statistics delivery system.
 *
 * @param running            whether the service is running
 * @param paused             whether delivery is paused
 * @param lastSuccessTime    timestamp of last successful delivery
 * @param lastFailureTime    timestamp of last failed delivery
 * @param pendingByPriority  count of pending statistics by priority
 * @param totalPending       total pending statistics count
 * @param failedCount        count of failed deliveries in current session
 * @param retryCount         count of retries in current session
 * @param queueDepth         current queue depth
 * @param backpressureStatus current backpressure level
 * @param offlineMode        whether operating in offline mode
 *
 * @author JExcellence
 * @since 1.0.0
 */
public record DeliveryStatus(
    boolean running,
    boolean paused,
    @Nullable Instant lastSuccessTime,
    @Nullable Instant lastFailureTime,
    @NotNull Map<DeliveryPriority, Integer> pendingByPriority,
    int totalPending,
    long failedCount,
    long retryCount,
    int queueDepth,
    @NotNull BackpressureLevel backpressureStatus,
    boolean offlineMode
) {

    /**
     * Creates a status indicating the service is not running.
     */
    public static DeliveryStatus notRunning() {
        return new DeliveryStatus(
            false, false, null, null,
            Map.of(), 0, 0, 0, 0,
            BackpressureLevel.NONE, false
        );
    }

    /**
     * Creates a builder for constructing status.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Checks if the service is healthy (running, not paused, not in offline mode).
     */
    public boolean isHealthy() {
        return running && !paused && !offlineMode &&
               backpressureStatus != BackpressureLevel.OVERFLOW;
    }

    /**
     * Gets the time since last successful delivery in seconds.
     */
    public long secondsSinceLastSuccess() {
        if (lastSuccessTime == null) {
            return -1;
        }
        return Instant.now().getEpochSecond() - lastSuccessTime.getEpochSecond();
    }

    /**
     * Builder for DeliveryStatus.
     */
    public static class Builder {
        private boolean running = false;
        private boolean paused = false;
        private Instant lastSuccessTime;
        private Instant lastFailureTime;
        private Map<DeliveryPriority, Integer> pendingByPriority = Map.of();
        private int totalPending = 0;
        private long failedCount = 0;
        private long retryCount = 0;
        private int queueDepth = 0;
        private BackpressureLevel backpressureStatus = BackpressureLevel.NONE;
        private boolean offlineMode = false;

        /**
         * Executes running.
         */
        public Builder running(boolean running) { this.running = running; return this; }
        /**
         * Executes paused.
         */
        public Builder paused(boolean paused) { this.paused = paused; return this; }
        /**
         * Executes lastSuccessTime.
         */
        public Builder lastSuccessTime(Instant time) { this.lastSuccessTime = time; return this; }
        /**
         * Executes method.
         */
        /**
         * Executes this member.
         */
        /**
         * Executes lastFailureTime.
         */
        public Builder lastFailureTime(Instant time) { this.lastFailureTime = time; return this; }
        /**
         * Executes pendingByPriority.
         */
        public Builder pendingByPriority(Map<DeliveryPriority, Integer> map) { this.pendingByPriority = map; return this; }
        /**
         * Executes totalPending.
         */
        public Builder totalPending(int count) { this.totalPending = count; return this; }
        /**
         * Executes failedCount.
         */
        public Builder failedCount(long count) { this.failedCount = count; return this; }
        /**
         * Executes retryCount.
         */
        public Builder retryCount(long count) { this.retryCount = count; return this; }
        /**
         * Executes queueDepth.
         */
        public Builder queueDepth(int depth) { this.queueDepth = depth; return this; }
        /**
         * Executes backpressureStatus.
         */
        public Builder backpressureStatus(BackpressureLevel level) { this.backpressureStatus = level; return this; }
        /**
         * Executes offlineMode.
         */
        public Builder offlineMode(boolean offline) { this.offlineMode = offline; return this; }

        /**
         * Executes build.
         */
        public DeliveryStatus build() {
            return new DeliveryStatus(
                running, paused, lastSuccessTime, lastFailureTime,
                pendingByPriority, totalPending, failedCount, retryCount,
                queueDepth, backpressureStatus, offlineMode
            );
        }
    }
}
