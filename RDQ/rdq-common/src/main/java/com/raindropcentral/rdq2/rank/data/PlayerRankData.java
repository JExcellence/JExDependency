package com.raindropcentral.rdq2.rank.data;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Data class representing a player's rank information.
 */
public record PlayerRankData(
    @NotNull UUID playerId,
    @NotNull List<RankPathData> activePaths
) {
    
    /**
     * Data class representing a rank path.
     */
    public record RankPathData(
        @NotNull String pathId,
        @NotNull String pathName,
        @NotNull String currentRank
    ) {}
}
