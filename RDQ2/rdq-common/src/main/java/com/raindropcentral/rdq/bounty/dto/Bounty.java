package com.raindropcentral.rdq.bounty.dto;

import com.raindropcentral.rdq.bounty.type.BountyStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable record representing a bounty data transfer object.
 */
public record Bounty(
    @Nullable Long id,
    @NotNull UUID targetUuid,
    @NotNull String targetName,
    @NotNull UUID commissionerUuid,
    @NotNull String commissionerName,
    @NotNull Set<RewardItem> rewardItems,
    @NotNull Map<String, Double> rewardCurrencies,
    double totalEstimatedValue,
    @NotNull LocalDateTime createdAt,
    @Nullable LocalDateTime expiresAt,
    @NotNull BountyStatus status,
    @NotNull Optional<ClaimInfo> claimInfo
) {
    /**
     * Checks if the bounty is currently active and not expired.
     *
     * @return true if the bounty is active and not expired
     */
    public boolean isActive() {
        return status == BountyStatus.ACTIVE && 
               (expiresAt == null || LocalDateTime.now().isBefore(expiresAt));
    }
    
    /**
     * Checks if the bounty has expired.
     *
     * @return true if the bounty has an expiration time that has passed
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
