package com.raindropcentral.rdt.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests null-safe comparison helpers on {@link ChunkType}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class ChunkTypeTest {

    @Test
    void instanceComparisonMatchesOnlyIdenticalType() {
        assertTrue(ChunkType.NEXUS.equalsType(ChunkType.NEXUS));
        assertFalse(ChunkType.NEXUS.equalsType(ChunkType.FARM));
        assertFalse(ChunkType.NEXUS.equalsType(null));
    }

    @Test
    void staticComparisonIsNullSafe() {
        assertTrue(ChunkType.equalsType(ChunkType.BANK, ChunkType.BANK));
        assertFalse(ChunkType.equalsType(ChunkType.BANK, ChunkType.DEFAULT));
        assertTrue(ChunkType.equalsType(null, null));
        assertFalse(ChunkType.equalsType(ChunkType.CLAIM_PENDING, null));
    }
}
