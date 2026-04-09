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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import com.raindropcentral.core.service.statistics.queue.QueuedStatistic;
import com.raindropcentral.rplatform.type.EStatisticType.StatisticDataType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests JSON serialization of {@link StatisticEntry} to ensure values are always strings.
 *
 * @author JExcellence
 * @since 2.0.0
 */
class StatisticEntrySerializationTest {

    @Test
    void serializesMapValueAsJsonString() {
        final UUID playerUuid = UUID.randomUUID();
        final Map<String, Object> complexValue = Map.of("key", "value", "count", 42);
        
        final QueuedStatistic queued = QueuedStatistic.builder()
            .playerUuid(playerUuid)
            .statisticKey("complex_stat")
            .value(complexValue)
            .dataType(StatisticDataType.STRING)
            .collectionTimestamp(System.currentTimeMillis())
            .sourcePlugin("Test")
            .build();

        final StatisticEntry entry = StatisticEntry.fromQueued(queued);
        
        // Value should be a JSON string
        assertTrue(entry.value().startsWith("{"));
        assertTrue(entry.value().contains("\"key\""));
        assertTrue(entry.value().contains("\"value\""));
        
        // Serialize with Gson
        final Gson gson = new GsonBuilder()
            .registerTypeAdapter(StatisticEntry.class, (JsonSerializer<StatisticEntry>) (src, typeOfSrc, context) -> {
                com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
                obj.addProperty("playerUuid", src.playerUuid().toString());
                obj.addProperty("statisticKey", src.statisticKey());
                obj.addProperty("value", src.value());
                obj.addProperty("dataType", src.dataType().name());
                obj.addProperty("collectionTimestamp", src.collectionTimestamp());
                obj.addProperty("isDelta", src.isDelta());
                obj.addProperty("sourcePlugin", src.sourcePlugin());
                return obj;
            })
            .create();
        
        final String json = gson.toJson(entry);
        
        // JSON should contain escaped quotes, not nested objects
        assertTrue(json.contains("\"value\":\""));
        assertFalse(json.contains("\"value\":{"));
    }
}
