package com.raindropcentral.rdq.bounty.dto;

import com.raindropcentral.rdq.bounty.type.ClaimMode;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable record containing information about a bounty claim.
 */
public record ClaimInfo(
    @NotNull UUID hunterUuid,
    @NotNull String hunterName,
    @NotNull LocalDateTime claimedAt,
    @NotNull ClaimMode claimMode
) {
}
