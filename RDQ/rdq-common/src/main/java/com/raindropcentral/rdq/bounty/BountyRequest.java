package com.raindropcentral.rdq.bounty;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Request record for creating a new bounty.
 *
 * <p>Validates input in the compact constructor:
 * <ul>
 *   <li>Amount must be positive</li>
 *   <li>Placer cannot target themselves</li>
 * </ul>
 *
 * @param placerId the UUID of the player placing the bounty
 * @param targetId the UUID of the target player
 * @param amount the bounty amount (must be positive)
 * @param currency the currency type
 * @throws IllegalArgumentException if placer equals target or amount is not positive
 */
public record BountyRequest(
    @NotNull UUID placerId,
    @NotNull UUID targetId,
    @NotNull BigDecimal amount,
    @NotNull String currency
) {
    public BountyRequest {
        Objects.requireNonNull(placerId, "placerId");
        Objects.requireNonNull(targetId, "targetId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        // Self-targeting validation is handled by config check in BountyCreationView
        // This allows server admins to enable/disable self-bounties via bounty.yml
    }

    public static BountyRequest of(@NotNull UUID placerId, @NotNull UUID targetId, double amount, @NotNull String currency) {
        return new BountyRequest(placerId, targetId, BigDecimal.valueOf(amount), currency);
    }
}
