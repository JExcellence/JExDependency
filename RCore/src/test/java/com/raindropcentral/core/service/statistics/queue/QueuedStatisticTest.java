package com.raindropcentral.core.service.statistics.queue;

import com.raindropcentral.rplatform.type.EStatisticType.StatisticDataType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests validation and helper methods on {@link QueuedStatistic}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class QueuedStatisticTest {

    @Test
    void builderProvidesSensibleDefaults() {
        final UUID playerUniqueId = UUID.randomUUID();
        final QueuedStatistic queuedStatistic = QueuedStatistic.builder()
            .playerUuid(playerUniqueId)
            .statisticKey("quests_completed")
            .value(4.0D)
            .dataType(StatisticDataType.NUMBER)
            .build();

        assertEquals(playerUniqueId, queuedStatistic.playerUuid());
        assertEquals(DeliveryPriority.NORMAL, queuedStatistic.priority());
        assertFalse(queuedStatistic.isDelta());
        assertEquals("RCore", queuedStatistic.sourcePlugin());
        assertTrue(queuedStatistic.collectionTimestamp() > 0);
    }

    @Test
    void withPriorityReturnsCopyWithUpdatedPriorityOnly() {
        final UUID playerUniqueId = UUID.randomUUID();
        final QueuedStatistic original = new QueuedStatistic(
            playerUniqueId,
            "total_kills",
            7.0D,
            StatisticDataType.NUMBER,
            123L,
            DeliveryPriority.NORMAL,
            true,
            "RDQ"
        );

        final QueuedStatistic updated = original.withPriority(DeliveryPriority.CRITICAL);

        assertEquals(DeliveryPriority.CRITICAL, updated.priority());
        assertEquals(original.playerUuid(), updated.playerUuid());
        assertEquals(original.statisticKey(), updated.statisticKey());
        assertEquals(original.value(), updated.value());
        assertEquals(original.dataType(), updated.dataType());
        assertEquals(original.collectionTimestamp(), updated.collectionTimestamp());
        assertEquals(original.isDelta(), updated.isDelta());
        assertEquals(original.sourcePlugin(), updated.sourcePlugin());
    }

    @Test
    void buildsDeduplicationKeyFromPlayerAndStatistic() {
        final UUID playerUniqueId = UUID.randomUUID();
        final QueuedStatistic queuedStatistic = new QueuedStatistic(
            playerUniqueId,
            "distance_walked",
            8.0D,
            StatisticDataType.NUMBER,
            12L,
            DeliveryPriority.HIGH,
            false,
            "RCore"
        );

        assertEquals(playerUniqueId + ":distance_walked", queuedStatistic.getDeduplicationKey());
    }

    @Test
    void validatesRequiredRecordFields() {
        final UUID playerUniqueId = UUID.randomUUID();

        assertThrows(
            IllegalArgumentException.class,
            () -> new QueuedStatistic(
                null,
                "key",
                1.0D,
                StatisticDataType.NUMBER,
                1L,
                DeliveryPriority.NORMAL,
                false,
                "RCore"
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new QueuedStatistic(
                playerUniqueId,
                " ",
                1.0D,
                StatisticDataType.NUMBER,
                1L,
                DeliveryPriority.NORMAL,
                false,
                "RCore"
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new QueuedStatistic(
                playerUniqueId,
                "key",
                null,
                StatisticDataType.NUMBER,
                1L,
                DeliveryPriority.NORMAL,
                false,
                "RCore"
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new QueuedStatistic(
                playerUniqueId,
                "key",
                1.0D,
                null,
                1L,
                DeliveryPriority.NORMAL,
                false,
                "RCore"
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new QueuedStatistic(
                playerUniqueId,
                "key",
                1.0D,
                StatisticDataType.NUMBER,
                1L,
                null,
                false,
                "RCore"
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new QueuedStatistic(
                playerUniqueId,
                "key",
                1.0D,
                StatisticDataType.NUMBER,
                1L,
                DeliveryPriority.NORMAL,
                false,
                " "
            )
        );
    }
}
