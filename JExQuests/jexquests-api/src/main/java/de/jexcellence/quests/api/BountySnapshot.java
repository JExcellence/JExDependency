package de.jexcellence.quests.api;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable snapshot of an active bounty.
 *
 * @param id database row id (for disambiguation against duplicates)
 * @param targetUuid player hunted by the bounty
 * @param issuerUuid player who placed the bounty
 * @param currency currency identifier
 * @param amount payout to the claimer
 * @param placedAt when the bounty was placed
 */
public record BountySnapshot(
        long id,
        @NotNull UUID targetUuid,
        @NotNull UUID issuerUuid,
        @NotNull String currency,
        double amount,
        @NotNull Instant placedAt
) {
}
