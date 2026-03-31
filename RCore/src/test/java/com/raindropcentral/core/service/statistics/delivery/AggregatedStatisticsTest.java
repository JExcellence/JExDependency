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

package com.raindropcentral.core.service.statistics.delivery;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests construction helpers for {@link AggregatedStatistics}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class AggregatedStatisticsTest {

    @Test
    void emptyFactoryReturnsZeroedSnapshot() {
        final long before = System.currentTimeMillis();

        final AggregatedStatistics statistics = AggregatedStatistics.empty();

        final long after = System.currentTimeMillis();
        assertTrue(statistics.timestamp() >= before);
        assertTrue(statistics.timestamp() <= after);
        assertEquals(0, statistics.totalPlayersTracked());
        assertEquals(0.0D, statistics.averagePlaytimeMs(), 0.000_1D);
        assertEquals(0.0D, statistics.totalEconomyVolume(), 0.000_1D);
        assertEquals(0, statistics.totalQuestCompletions());
        assertTrue(statistics.customAggregates().isEmpty());
    }

    @Test
    void builderCopiesAndSealsCustomAggregates() {
        final AggregatedStatistics statistics = AggregatedStatistics.builder()
            .timestamp(100L)
            .totalPlayersTracked(5)
            .averagePlaytimeMs(10.5D)
            .totalEconomyVolume(200.0D)
            .totalQuestCompletions(3)
            .customAggregate("players_online_peak", 22)
            .customAggregates(Map.of("events", 4))
            .build();

        assertEquals(100L, statistics.timestamp());
        assertEquals(5, statistics.totalPlayersTracked());
        assertEquals(10.5D, statistics.averagePlaytimeMs(), 0.000_1D);
        assertEquals(200.0D, statistics.totalEconomyVolume(), 0.000_1D);
        assertEquals(3, statistics.totalQuestCompletions());
        assertEquals(22, statistics.customAggregates().get("players_online_peak"));
        assertEquals(4, statistics.customAggregates().get("events"));
        assertThrows(
            UnsupportedOperationException.class,
            () -> statistics.customAggregates().put("extra", 1)
        );
    }
}
