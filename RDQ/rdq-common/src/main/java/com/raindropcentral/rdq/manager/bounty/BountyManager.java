package com.raindropcentral.rdq.manager.bounty;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Manager interface for bounty operations.
 * <p>
 * Implementations differ between free and premium versions:
 * <ul>
 * <li>Free: In-memory storage, view-only, limited features</li>
 * <li>Premium: Database persistence, full CRUD, unlimited features</li>
 * </ul>
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public interface BountyManager {

    /**
     * Retrieves a paginated collection of bounties for display.
     *
     * @param page     zero-based page index to request
     * @param pageSize amount of bounties to include per page
     * @return future that completes with the ordered list of bounties for the requested page
     */
    @NotNull CompletableFuture<List<RBounty>> getAllBounties(int page, int pageSize);

    /**
     * Looks up the active bounty associated with a specific player.
     *
     * @param playerUuid unique identifier of the player whose bounty is being retrieved
     * @return future that completes with the optional bounty result
     */
    @NotNull CompletableFuture<Optional<RBounty>> getBountyByPlayer(@NotNull UUID playerUuid);

    /**
     * Creates and persists a new bounty targeting the supplied player.
     *
     * @param target            player entity that the bounty will target
     * @param commissioner      online player commissioning the bounty
     * @param rewardItems       collection of reward items to award upon completion
     * @param rewardCurrencies  mapping of currency identifiers to payout amounts
     * @return future that completes with the newly created bounty instance
     */
    @NotNull CompletableFuture<RBounty> createBounty(
            @NotNull RDQPlayer target,
            @NotNull Player commissioner,
            @NotNull Set<RewardItem> rewardItems,
            @NotNull Map<String, Double> rewardCurrencies
    );

    /**
     * Removes a bounty from the underlying store.
     *
     * @param bountyId identifier of the bounty to delete
     * @return future that completes with {@code true} if the bounty was removed
     */
    @NotNull CompletableFuture<Boolean> deleteBounty(@NotNull Long bountyId);

    /**
     * Updates an existing bounty record.
     *
     * @param bounty bounty entity containing the desired changes
     * @return future that completes with the persisted bounty data
     */
    @NotNull CompletableFuture<RBounty> updateBounty(@NotNull RBounty bounty);

    /**
     * Retrieves the maximum amount of active bounties a player may have.
     *
     * @return configured per-player bounty limit
     */
    int getMaxBountiesPerPlayer();

    /**
     * Retrieves the maximum number of reward items a single bounty can grant.
     *
     * @return configured reward item ceiling
     */
    int getMaxRewardItems();

    /**
     * Determines whether the supplied player is eligible to create a bounty.
     *
     * @param player online player requesting bounty creation
     * @return {@code true} when bounty creation is permitted
     */
    boolean canCreateBounty(@NotNull Player player);

    /**
     * Retrieves the total number of bounties across all pages.
     *
     * @return future that completes with the total bounty count
     */
    @NotNull CompletableFuture<Integer> getTotalBountyCount();
}