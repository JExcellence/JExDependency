package com.raindropcentral.rdq.bounty;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Objects;

public record ClaimResult(
    @NotNull Bounty bounty,
    @NotNull BigDecimal reward,
    @NotNull DistributionMode mode
) {
    public ClaimResult {
        Objects.requireNonNull(bounty, "bounty");
        Objects.requireNonNull(reward, "reward");
        Objects.requireNonNull(mode, "mode");
    }
}
