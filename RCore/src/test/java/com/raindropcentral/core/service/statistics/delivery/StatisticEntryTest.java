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
