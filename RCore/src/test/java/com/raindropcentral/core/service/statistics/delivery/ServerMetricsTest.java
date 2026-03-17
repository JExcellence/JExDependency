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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests builder defaults and value propagation for {@link ServerMetrics}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class ServerMetricsTest {

    @Test
    void builderDefaultsToBaselineServerValues() {
        final ServerMetrics metrics = ServerMetrics.builder().build();

        assertEquals(20.0D, metrics.tps1m(), 0.000_1D);
        assertEquals(20.0D, metrics.tps5m(), 0.000_1D);
        assertEquals(20.0D, metrics.tps15m(), 0.000_1D);
        assertEquals(0L, metrics.heapUsed());
        assertEquals(0L, metrics.heapMax());
        assertEquals(0L, metrics.nonHeapUsed());
        assertEquals(0.0D, metrics.cpuUsage(), 0.000_1D);
        assertEquals(0, metrics.onlinePlayers());
        assertEquals(0, metrics.maxPlayers());
        assertEquals(0L, metrics.uptimeMs());
        assertEquals(0, metrics.worldCount());
        assertEquals(0, metrics.loadedChunks());
        assertEquals(0, metrics.entityCount());
        assertEquals(0, metrics.tileEntityCount());
    }

    @Test
    void builderMapsProvidedValues() {
        final ServerMetrics metrics = ServerMetrics.builder()
            .tps1m(19.5D)
            .tps5m(19.2D)
            .tps15m(18.8D)
            .heapUsed(100L)
            .heapMax(200L)
            .nonHeapUsed(30L)
            .cpuUsage(40.2D)
            .onlinePlayers(10)
            .maxPlayers(100)
            .uptimeMs(120_000L)
            .worldCount(3)
            .loadedChunks(400)
            .entityCount(500)
            .tileEntityCount(60)
            .build();

        assertEquals(19.5D, metrics.tps1m(), 0.000_1D);
        assertEquals(19.2D, metrics.tps5m(), 0.000_1D);
        assertEquals(18.8D, metrics.tps15m(), 0.000_1D);
        assertEquals(100L, metrics.heapUsed());
        assertEquals(200L, metrics.heapMax());
        assertEquals(30L, metrics.nonHeapUsed());
        assertEquals(40.2D, metrics.cpuUsage(), 0.000_1D);
        assertEquals(10, metrics.onlinePlayers());
        assertEquals(100, metrics.maxPlayers());
        assertEquals(120_000L, metrics.uptimeMs());
        assertEquals(3, metrics.worldCount());
        assertEquals(400, metrics.loadedChunks());
        assertEquals(500, metrics.entityCount());
        assertEquals(60, metrics.tileEntityCount());
    }
}
