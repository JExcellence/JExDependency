package com.raindropcentral.rdt.utils;

import org.jspecify.annotations.Nullable;

/**
 * Semantic type assigned to a claimed town chunk.
 *
 * <p>Provides helper comparison methods so call sites can perform null-safe enum checks without
 * duplicating equality boilerplate.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.2
 */
public enum ChunkType {
    DEFAULT,
    NEXUS,
    BANK,
    FARM,
    CLAIM_PENDING,
    CHUNK_BLOCK;

    /**
     * Returns whether this chunk type matches the provided type.
     *
     * @param chunkType type to compare against
     * @return {@code true} when equal
     */
    public boolean equalsType(final @Nullable ChunkType chunkType) {
        return this == chunkType;
    }

    /**
     * Null-safe comparison helper for two chunk types.
     *
     * @param first first type
     * @param second second type
     * @return {@code true} when equal
     */
    public static boolean equalsType(
            final @Nullable ChunkType first,
            final @Nullable ChunkType second
    ) {
        return first == second;
    }
}
