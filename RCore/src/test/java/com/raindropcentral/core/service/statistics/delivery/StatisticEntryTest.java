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

import com.raindropcentral.core.service.statistics.queue.QueuedStatistic;
import com.raindropcentral.rplatform.type.EStatisticType.StatisticDataType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests queued-to-wire conversion in {@link StatisticEntry}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class StatisticEntryTest {

    @Test
    void convertsQueuedStatisticWithoutLosingFields() {
        final UUID playerUniqueId = UUID.randomUUID();
        final QueuedStatistic queuedStatistic = QueuedStatistic.builder()
            .playerUuid(playerUniqueId)
            .statisticKey("total_kills")
            .value(9.0D)
            .dataType(StatisticDataType.NUMBER)
            .collectionTimestamp(321L)
            .isDelta(true)
            .sourcePlugin("RDQ")
            .build();

        final StatisticEntry entry = StatisticEntry.fromQueued(queuedStatistic);

        assertEquals(queuedStatistic.playerUuid(), entry.playerUuid());
        assertEquals(queuedStatistic.statisticKey(), entry.statisticKey());
        assertEquals(queuedStatistic.value(), entry.value());
        assertEquals(queuedStatistic.dataType(), entry.dataType());
        assertEquals(queuedStatistic.collectionTimestamp(), entry.collectionTimestamp());
        assertEquals(queuedStatistic.isDelta(), entry.isDelta());
        assertEquals(queuedStatistic.sourcePlugin(), entry.sourcePlugin());
    }
}
