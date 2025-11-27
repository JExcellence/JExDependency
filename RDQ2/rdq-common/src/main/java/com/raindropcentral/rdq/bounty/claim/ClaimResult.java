package com.raindropcentral.rdq.bounty.claim;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Represents the result of a bounty claim determination.
 * Contains the winner(s) and their proportional shares of the bounty.
 */
public record ClaimResult(
    @NotNull Map<UUID, Double> shares
) {
    /**
     * Creates a new ClaimResult with defensive copy of shares.
     */
    public ClaimResult {
        shares = Map.copyOf(shares);
    }
    
    /**
     * Checks if there are any winners.
     *
     * @return true if there is at least one winner
     */
    public boolean hasWinners() {
        return !shares.isEmpty();
    }
    
    /**
     * Gets the set of winner UUIDs.
     *
     * @return an immutable set of winner UUIDs
     */
    @NotNull
    public Set<UUID> getWinners() {
        return shares.keySet();
    }
    
    /**
     * Gets the share proportion for a specific player.
     *
     * @param playerUuid the UUID of the player
     * @return the proportion (0.0 to 1.0), or 0.0 if not a winner
     */
    public double getShare(@NotNull UUID playerUuid) {
        return shares.getOrDefault(playerUuid, 0.0);
    }
    
    /**
     * Checks if this is a single-winner result.
     *
     * @return true if there is exactly one winner
     */
    public boolean isSingleWinner() {
        return shares.size() == 1;
    }
    
    /**
     * Gets the single winner UUID if there is exactly one winner.
     *
     * @return the winner UUID, or null if there are zero or multiple winners
     */
    public UUID getSingleWinner() {
        if (isSingleWinner()) {
            return shares.keySet().iterator().next();
        }
        return null;
    }
    
    /**
     * Creates an empty ClaimResult with no winners.
     *
     * @return an empty ClaimResult
     */
    @NotNull
    public static ClaimResult empty() {
        return new ClaimResult(Collections.emptyMap());
    }
    
    /**
     * Creates a ClaimResult with a single winner receiving 100% of the bounty.
     *
     * @param winnerUuid the UUID of the winner
     * @return a ClaimResult with a single winner
     */
    @NotNull
    public static ClaimResult singleWinner(@NotNull UUID winnerUuid) {
        return new ClaimResult(Map.of(winnerUuid, 1.0));
    }
}
