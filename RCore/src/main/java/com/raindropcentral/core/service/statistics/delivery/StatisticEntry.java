package com.raindropcentral.core.service.statistics.delivery;

import com.raindropcentral.rplatform.type.EStatisticType.StatisticDataType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Represents a single statistic entry within a batch payload for API transmission.
 * This is the wire format for individual statistics sent to the backend.
 *
 * @param playerUuid          the UUID of the player this statistic belongs to
 * @param statisticKey        the unique key identifying the statistic type
 * @param value               the statistic value
 * @param dataType            the data type of the value (NUMBER, STRING, BOOLEAN, TIMESTAMP)
 * @param collectionTimestamp the timestamp when this statistic was collected
 * @param isDelta             true if this value represents a change since last delivery
 * @param sourcePlugin        the plugin that generated this statistic
 *
 * @author JExcellence
 * @since 1.0.0
 */
public record StatisticEntry(
    @NotNull UUID playerUuid,
    @NotNull String statisticKey,
    @NotNull Object value,
    @NotNull StatisticDataType dataType,
    long collectionTimestamp,
    boolean isDelta,
    @NotNull String sourcePlugin
) {

    /**
     * Creates a StatisticEntry from a QueuedStatistic.
     *
     * @param queued the queued statistic to convert
     * @return a new StatisticEntry
     */
    public static StatisticEntry fromQueued(
        final @NotNull com.raindropcentral.core.service.statistics.queue.QueuedStatistic queued
    ) {
        return new StatisticEntry(
            queued.playerUuid(),
            queued.statisticKey(),
            queued.value(),
            queued.dataType(),
            queued.collectionTimestamp(),
            queued.isDelta(),
            queued.sourcePlugin()
        );
    }
}
