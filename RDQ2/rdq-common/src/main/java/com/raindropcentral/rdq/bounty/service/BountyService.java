package com.raindropcentral.rdq.bounty.service;

import com.raindropcentral.rdq.bounty.dto.Bounty;
import com.raindropcentral.rdq.bounty.dto.BountyCreationRequest;
import com.raindropcentral.rdq.bounty.dto.HunterStats;
import com.raindropcentral.rdq.bounty.exception.BountyException;
import com.raindropcentral.rdq.bounty.type.HunterSortOrder;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Sealed interface for bounty service operations.
 * This interface defines all bounty-related operations with async database access.
 * Only PremiumBountyService and FreeBountyService may implement this interface.
 */
public sealed interface BountyService permits PremiumBountyService, FreeBountyService {
    
    // ========== Query Operations ==========
    
    /**
     * Retrieves all bounties with pagination support.
     *
     * @param page the page number (0-indexed)
     * @param pageSize the number of bounties per page
     * @return a future containing the list of bounties for the requested page
     */
    @NotNull
    CompletableFuture<List<Bounty>> getAllBounties(int page, int pageSize);
    
    /**
     * Retrieves a bounty by target UUID.
     *
     * @param targetUuid the UUID of the target player
     * @return a future containing an optional bounty
     */
    @NotNull
    CompletableFuture<Optional<Bounty>> getBountyByTarget(@NotNull UUID targetUuid);
    
    /**
     * Retrieves all bounties created by a specific commissioner.
     *
     * @param commissionerUuid the UUID of the commissioner
     * @return a future containing the list of bounties
     */
    @NotNull
    CompletableFuture<List<Bounty>> getBountiesByCommissioner(@NotNull UUID commissionerUuid);
    
    /**
     * Gets the total count of active bounties.
     *
     * @return a future containing the total count
     */
    @NotNull
    CompletableFuture<Integer> getTotalBountyCount();
    
    // ========== Mutation Operations ==========
    
    /**
     * Creates a new bounty.
     *
     * @param request the bounty creation request
     * @return a future containing the created bounty
     * @throws BountyException if creation fails
     */
    @NotNull
    CompletableFuture<Bounty> createBounty(@NotNull BountyCreationRequest request) throws BountyException;
    
    /**
     * Deletes a bounty by ID.
     *
     * @param bountyId the ID of the bounty to delete
     * @return a future containing true if deletion was successful
     */
    @NotNull
    CompletableFuture<Boolean> deleteBounty(@NotNull Long bountyId);
    
    /**
     * Claims a bounty for a hunter.
     *
     * @param bountyId the ID of the bounty to claim
     * @param hunterUuid the UUID of the hunter claiming the bounty
     * @return a future containing the claimed bounty
     * @throws BountyException if claiming fails
     */
    @NotNull
    CompletableFuture<Bounty> claimBounty(@NotNull Long bountyId, @NotNull UUID hunterUuid) throws BountyException;
    
    /**
     * Marks a bounty as expired.
     *
     * @param bountyId the ID of the bounty to expire
     * @return a future that completes when the operation is done
     */
    @NotNull
    CompletableFuture<Void> expireBounty(@NotNull Long bountyId);
    
    // ========== Hunter Statistics ==========
    
    /**
     * Retrieves hunter statistics for a player.
     *
     * @param playerUuid the UUID of the player
     * @return a future containing optional hunter statistics
     */
    @NotNull
    CompletableFuture<Optional<HunterStats>> getHunterStats(@NotNull UUID playerUuid);
    
    /**
     * Retrieves the top hunters based on sort order.
     *
     * @param limit the maximum number of hunters to retrieve
     * @param sortOrder the sort order for ranking
     * @return a future containing the list of top hunters
     */
    @NotNull
    CompletableFuture<List<HunterStats>> getTopHunters(int limit, @NotNull HunterSortOrder sortOrder);
    
    /**
     * Gets the rank of a hunter.
     *
     * @param playerUuid the UUID of the player
     * @return a future containing the player's rank (1-indexed)
     */
    @NotNull
    CompletableFuture<Integer> getHunterRank(@NotNull UUID playerUuid);
    
    // ========== Edition Capabilities ==========
    
    /**
     * Checks if this is the premium edition service.
     *
     * @return true if premium, false if free
     */
    boolean isPremium();
    
    /**
     * Gets the maximum number of active bounties per player.
     *
     * @return the maximum bounty limit
     */
    int getMaxBountiesPerPlayer();
    
    /**
     * Gets the maximum number of reward items per bounty.
     *
     * @return the maximum reward item limit
     */
    int getMaxRewardItems();
    
    /**
     * Checks if a player can create a bounty.
     *
     * @param player the player to check
     * @return true if the player can create a bounty
     */
    boolean canCreateBounty(@NotNull Player player);
}
