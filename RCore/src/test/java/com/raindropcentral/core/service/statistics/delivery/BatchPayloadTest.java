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
            3.0D,
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
