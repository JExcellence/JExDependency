package com.raindropcentral.rdq.bounty.dto;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Immutable record representing bounty hunter statistics.
 */
public record HunterStats(
    @NotNull UUID playerUuid,
    @NotNull String playerName,
    int bountiesClaimed,
    double totalRewardValue,
    double highestBountyValue,
    @NotNull Optional<LocalDateTime> lastClaimTime,
    int rank
) {
}
