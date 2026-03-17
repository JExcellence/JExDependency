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
