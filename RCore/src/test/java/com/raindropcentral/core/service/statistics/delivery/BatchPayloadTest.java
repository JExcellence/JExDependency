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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests builder and copy-helper behavior on {@link BatchPayload}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class BatchPayloadTest {

    @Test
    void builderRequiresServerIdentifier() {
        assertThrows(IllegalStateException.class, () -> BatchPayload.builder().build());
    }

    @Test
    void builderGeneratesBatchIdentifierAndEntryCount() {
        final StatisticEntry entry = new StatisticEntry(
            UUID.randomUUID(),
            "total_kills",
            "3.0",
            StatisticDataType.NUMBER,
            10L,
            false,
            "RCore"
        );

        final BatchPayload payload = BatchPayload.builder()
            .serverUuid(UUID.randomUUID())
            .entries(List.of(entry))
            .build();

        assertNotNull(payload.batchId());
        assertTrue(payload.batchId().startsWith("batch-"));
        assertEquals(1, payload.entryCount());
        assertEquals(List.of(entry), payload.entries());
    }

    @Test
    void withChecksumAndWithSignatureReturnUpdatedCopies() {
        final BatchPayload payload = BatchPayload.builder()
            .serverUuid("server-1")
            .batchId("batch-a")
            .build();

        final BatchPayload checksummed = payload.withChecksum("checksum-1");
        final BatchPayload signed = checksummed.withSignature("signature-1");

        assertEquals("checksum-1", checksummed.checksum());
        assertEquals("signature-1", signed.signature());
        assertEquals("checksum-1", signed.checksum());
        assertEquals(payload.batchId(), signed.batchId());
    }
}
