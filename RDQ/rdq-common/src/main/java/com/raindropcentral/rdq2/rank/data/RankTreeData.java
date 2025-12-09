package com.raindropcentral.rdq2.rank.data;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Data class representing a rank tree.
 */
public record RankTreeData(
    @NotNull String id,
    @NotNull String name,
    @NotNull List<RankData> ranks
) {
    
    /**
     * Data class representing a rank.
     */
    public record RankData(
        @NotNull String id,
        @NotNull String name,
        int tier
    ) {}
}
