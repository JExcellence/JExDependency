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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests queue-state helper behavior in {@link QueueStatistics}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class QueueStatisticsTest {

    @Test
    void emptyFactoryReturnsNoBackpressureSnapshot() {
        final QueueStatistics statistics = QueueStatistics.empty();

        assertTrue(statistics.isEmpty());
        assertFalse(statistics.isUnderPressure());
        assertEquals(0, statistics.totalSize());
        assertEquals(0, statistics.sizeFor(DeliveryPriority.CRITICAL));
        assertEquals(BackpressureLevel.NONE, statistics.backpressureLevel());
    }

    @Test
    void builderMapsStateAndLookupByPriority() {
        final QueueStatistics statistics = QueueStatistics.builder()
            .sizeByPriority(Map.of(
                DeliveryPriority.CRITICAL, 2,
                DeliveryPriority.LOW, 3
            ))
            .totalSize(5)
            .oldestEntryAgeMs(1500L)
            .backpressureLevel(BackpressureLevel.WARNING)
            .capacityUsed(80.0D)
            .persistedCount(1)
            .build();

        assertFalse(statistics.isEmpty());
        assertTrue(statistics.isUnderPressure());
        assertEquals(2, statistics.sizeFor(DeliveryPriority.CRITICAL));
        assertEquals(3, statistics.sizeFor(DeliveryPriority.LOW));
        assertEquals(0, statistics.sizeFor(DeliveryPriority.BULK));
        assertEquals(5, statistics.totalSize());
        assertEquals(1500L, statistics.oldestEntryAgeMs());
        assertEquals(BackpressureLevel.WARNING, statistics.backpressureLevel());
        assertEquals(80.0D, statistics.capacityUsed(), 0.000_1D);
        assertEquals(1, statistics.persistedCount());
    }
}
