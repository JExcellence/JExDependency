package com.raindropcentral.rdq.api;

import com.raindropcentral.rdq.bounty.Bounty;
import com.raindropcentral.rdq.bounty.BountyRequest;
import com.raindropcentral.rdq.bounty.ClaimResult;
import com.raindropcentral.rdq.bounty.HunterStats;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for bounty hunting operations.
 *
 * <p>This sealed interface defines the contract for bounty management, including
 * bounty creation, claiming, and statistics tracking. Implementations are provided
 * by {@link FreeBountyService} (basic features) and {@link PremiumBountyService}
 * (advanced distribution modes).
 *
 * <p>All methods return {@link CompletableFuture} for async execution.
 * Economy operations integrate with Vault for currency handling.
 *
 * @see FreeBountyService
 * @see PremiumBountyService
 * @see Bounty
 * @see BountyRequest
 */
public sealed interface BountyService permits FreeBountyService, PremiumBountyService {

    /**
     * Creates a new bounty on a target player.
     *
     * <p>This method validates the request, withdraws funds from the placer's
     * account, and creates the bounty. Self-targeting is rejected.
     *
     * @param request the bounty creation request containing placer, target, and amount
     * @return a future containing the created bounty
     * @throws com.raindropcentral.rdq.shared.error.RDQException if validation fails or funds insufficient
     */
    CompletableFuture<Bounty> createBounty(@NotNull BountyRequest request);

    /**
     * Claims all active bounties on a target player.
     *
     * <p>Called when a hunter kills a target. Distributes rewards according to
     * the configured distribution mode and updates hunter statistics.
     *
     * @param hunterId the UUID of the hunter claiming the bounty
     * @param targetId the UUID of the target who was killed
     * @return a future containing the claim result with total reward
     */
    CompletableFuture<ClaimResult> claimBounty(@NotNull UUID hunterId, @NotNull UUID targetId);

    /**
     * Retrieves all currently active bounties.
     *
     * @return a future containing the list of active bounties
     */
    CompletableFuture<List<Bounty>> getActiveBounties();

    /**
     * Retrieves all active bounties placed on a specific player.
     *
     * @param targetId the UUID of the target player
     * @return a future containing bounties on the target
     */
    CompletableFuture<List<Bounty>> getBountiesOnPlayer(@NotNull UUID targetId);

    /**
     * Retrieves all bounties placed by a specific player.
     *
     * @param placerId the UUID of the placer
     * @return a future containing bounties placed by the player
     */
    CompletableFuture<List<Bounty>> getBountiesPlacedBy(@NotNull UUID placerId);

    /**
     * Retrieves hunter statistics for a player.
     *
     * @param playerId the UUID of the player
     * @return a future containing the player's hunter stats, or empty if none
     */
    CompletableFuture<Optional<HunterStats>> getHunterStats(@NotNull UUID playerId);

    /**
     * Retrieves the top hunters leaderboard.
     *
     * @param limit the maximum number of entries to return
     * @return a future containing the top hunters sorted by bounties claimed
     */
    CompletableFuture<List<HunterStats>> getLeaderboard(int limit);

    /**
     * Cancels a bounty placed by a player.
     *
     * <p>Only the original placer can cancel their bounty. A partial refund
     * may be issued based on configuration.
     *
     * @param placerId the UUID of the placer requesting cancellation
     * @param bountyId the ID of the bounty to cancel
     * @return a future containing true if cancelled successfully
     */
    CompletableFuture<Boolean> cancelBounty(@NotNull UUID placerId, long bountyId);

    /**
     * Expires and cleans up old bounties.
     *
     * <p>Called periodically to mark expired bounties and issue refunds
     * to placers based on the configured refund percentage.
     *
     * @return a future that completes when expiration processing is done
     */
    CompletableFuture<Void> expireOldBounties();
}
