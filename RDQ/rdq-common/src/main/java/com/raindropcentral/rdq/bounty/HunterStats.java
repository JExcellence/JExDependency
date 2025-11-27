package com.raindropcentral.rdq.bounty;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record HunterStats(
    @NotNull UUID playerId,
    @NotNull String playerName,
    int bountiesPlaced,
    int bountiesClaimed,
    int deaths,
    @NotNull BigDecimal totalEarned,
    @NotNull BigDecimal totalSpent
) {
    public HunterStats {
        Objects.requireNonNull(playerId, "playerId");
        Objects.requireNonNull(playerName, "playerName");
        Objects.requireNonNull(totalEarned, "totalEarned");
        Objects.requireNonNull(totalSpent, "totalSpent");
    }

    public double getKDRatio() {
        return deaths == 0 ? bountiesClaimed : (double) bountiesClaimed / deaths;
    }

    public BigDecimal getNetProfit() {
        return totalEarned.subtract(totalSpent);
    }

    public static HunterStats empty(@NotNull UUID playerId, @NotNull String playerName) {
        return new HunterStats(playerId, playerName, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
