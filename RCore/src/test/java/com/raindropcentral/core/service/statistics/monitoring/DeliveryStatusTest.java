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
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests lifecycle and health helpers in {@link DeliveryStatus}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class DeliveryStatusTest {

    @Test
    void notRunningFactoryCreatesBaselineStatus() {
        final DeliveryStatus status = DeliveryStatus.notRunning();

        assertFalse(status.running());
        assertFalse(status.paused());
        assertFalse(status.offlineMode());
        assertFalse(status.isHealthy());
        assertEquals(-1L, status.secondsSinceLastSuccess());
    }

    @Test
    void healthyStatusRequiresRunningUnpausedOnlineAndNoOverflow() {
        final DeliveryStatus healthy = DeliveryStatus.builder()
            .running(true)
            .paused(false)
            .offlineMode(false)
            .backpressureStatus(BackpressureLevel.WARNING)
            .build();

        final DeliveryStatus paused = DeliveryStatus.builder()
            .running(true)
            .paused(true)
            .offlineMode(false)
            .backpressureStatus(BackpressureLevel.NONE)
            .build();

        final DeliveryStatus offline = DeliveryStatus.builder()
            .running(true)
            .paused(false)
            .offlineMode(true)
            .backpressureStatus(BackpressureLevel.NONE)
            .build();

        final DeliveryStatus overflow = DeliveryStatus.builder()
            .running(true)
            .paused(false)
            .offlineMode(false)
            .backpressureStatus(BackpressureLevel.OVERFLOW)
            .build();

        assertTrue(healthy.isHealthy());
        assertFalse(paused.isHealthy());
        assertFalse(offline.isHealthy());
        assertFalse(overflow.isHealthy());
    }

    @Test
    void builderMapsStatusAndPendingCounts() {
        final Instant successTime = Instant.now().minusSeconds(5L);
        final DeliveryStatus status = DeliveryStatus.builder()
            .running(true)
            .paused(false)
            .lastSuccessTime(successTime)
            .pendingByPriority(Map.of(DeliveryPriority.CRITICAL, 2))
            .totalPending(2)
            .failedCount(1L)
            .retryCount(3L)
            .queueDepth(4)
            .backpressureStatus(BackpressureLevel.NONE)
            .offlineMode(false)
            .build();

        assertEquals(successTime, status.lastSuccessTime());
        assertEquals(2, status.pendingByPriority().get(DeliveryPriority.CRITICAL));
        assertEquals(2, status.totalPending());
        assertEquals(1L, status.failedCount());
        assertEquals(3L, status.retryCount());
        assertEquals(4, status.queueDepth());
        assertTrue(status.secondsSinceLastSuccess() >= 5L);
    }
}
