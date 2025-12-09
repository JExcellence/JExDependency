package com.raindropcentral.rdq.bounty.claim;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Represents the result of a bounty claim operation.
 * Contains information about winners and their reward proportions.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public record ClaimResult(
        @NotNull Map<UUID, Double> winners
) {
    public ClaimResult {
        winners = Collections.unmodifiableMap(winners);
    }

    /**
     * Creates an empty claim result (no winners).
     */
    public static ClaimResult empty() {
        return new ClaimResult(Collections.emptyMap());
    }

    /**
     * Creates a claim result with a single winner receiving 100% of the bounty.
     */
    public static ClaimResult singleWinner(@NotNull UUID winner) {
        return new ClaimResult(Map.of(winner, 1.0));
    }

    /**
     * Checks if there are any winners.
     */
    public boolean hasWinners() {
        return !winners.isEmpty();
    }

    /**
     * Gets the number of winners.
     */
    public int getWinnerCount() {
        return winners.size();
    }
}
