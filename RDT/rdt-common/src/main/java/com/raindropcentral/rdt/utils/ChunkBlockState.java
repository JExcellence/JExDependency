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

package com.raindropcentral.rdt.utils;

import com.raindropcentral.rdt.database.entity.RChunk;
import com.raindropcentral.rdt.database.entity.RTown;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

/**
 * Resolves derived runtime state for placed chunk blocks.
 *
 * <p>Inactive chunk blocks are those whose stored block Y offset from the current nexus block
 * Y is outside the configured min/max distance window. Inactive chunk blocks are treated as
 * {@link ChunkType#DEFAULT} for display and gameplay-state purposes.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class ChunkBlockState {

    private ChunkBlockState() {
    }

    /**
     * Returns whether the chunk has a persisted placed chunk-block location.
     *
     * @param chunk chunk to inspect
     * @return {@code true} when world and XYZ are all present
     */
    public static boolean hasPlacedChunkBlock(final @NotNull RChunk chunk) {
        return chunk.getChunkBlockWorld() != null
                && chunk.getChunkBlockX() != null
                && chunk.getChunkBlockY() != null
                && chunk.getChunkBlockZ() != null;
    }

    /**
     * Returns whether the given chunk block is inactive relative to the town nexus Y distance window.
     *
     * @param town town owning the chunk
     * @param chunk chunk to evaluate
     * @param minYOffset minimum allowed Y offset from nexus (inclusive)
     * @param maxYOffset maximum allowed Y offset from nexus (inclusive)
     * @return {@code true} when the chunk block is inactive
     */
    public static boolean isInactive(
            final @NotNull RTown town,
            final @NotNull RChunk chunk,
            final int minYOffset,
            final int maxYOffset
    ) {
        if (!hasPlacedChunkBlock(chunk)) {
            return false;
        }
        if (ChunkType.equalsType(chunk.getType(), ChunkType.CLAIM_PENDING)
                || ChunkType.equalsType(chunk.getType(), ChunkType.NEXUS)) {
            return false;
        }

        final @Nullable Location nexusLocation = town.getNexusLocation();
        if (nexusLocation == null || nexusLocation.getWorld() == null) {
            return false;
        }

        if (!nexusLocation.getWorld().getName().equals(chunk.getChunkBlockWorld())) {
            return true;
        }

        final Integer chunkBlockY = chunk.getChunkBlockY();
        if (chunkBlockY == null) {
            return false;
        }
        final int yDistance = chunkBlockY - nexusLocation.getBlockY();
        return yDistance < minYOffset || yDistance > maxYOffset;
    }

    /**
     * Resolves the effective chunk type, falling back to {@link ChunkType#DEFAULT} when inactive.
     *
     * @param town town owning the chunk
     * @param chunk chunk to evaluate
     * @param minYOffset minimum allowed Y offset from nexus (inclusive)
     * @param maxYOffset maximum allowed Y offset from nexus (inclusive)
     * @return effective chunk type
     */
    public static @NotNull ChunkType resolveEffectiveType(
            final @NotNull RTown town,
            final @NotNull RChunk chunk,
            final int minYOffset,
            final int maxYOffset
    ) {
        if (isInactive(town, chunk, minYOffset, maxYOffset)) {
            return ChunkType.DEFAULT;
        }
        return chunk.getType();
    }

    /**
     * Computes Y distance from nexus to a block Y in the same world.
     *
     * @param nexusLocation nexus location
     * @param blockY target block Y
     * @return {@code blockY - nexusY}
     */
    public static int resolveYDistanceFromNexus(
            final @NotNull Location nexusLocation,
            final int blockY
    ) {
        return blockY - nexusLocation.getBlockY();
    }
}
