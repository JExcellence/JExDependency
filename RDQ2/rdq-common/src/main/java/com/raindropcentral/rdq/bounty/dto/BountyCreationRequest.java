package com.raindropcentral.rdq.bounty.dto;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Immutable record representing a request to create a new bounty.
 */
public record BountyCreationRequest(
    @NotNull UUID targetUuid,
    @NotNull UUID commissionerUuid,
    @NotNull Set<RewardItem> rewardItems,
    @NotNull Map<String, Double> rewardCurrencies,
    @NotNull Optional<LocalDateTime> customExpiration
) {
    /**
     * Compact constructor with validation and defensive copying.
     */
    public BountyCreationRequest {
        Objects.requireNonNull(targetUuid, "target cannot be null");
        Objects.requireNonNull(commissionerUuid, "commissioner cannot be null");
        Objects.requireNonNull(rewardItems, "rewardItems cannot be null");
        Objects.requireNonNull(rewardCurrencies, "rewardCurrencies cannot be null");
        Objects.requireNonNull(customExpiration, "customExpiration cannot be null");
        
        // Defensive copies to ensure immutability
        rewardItems = Set.copyOf(rewardItems);
        rewardCurrencies = Map.copyOf(rewardCurrencies);
    }
}
