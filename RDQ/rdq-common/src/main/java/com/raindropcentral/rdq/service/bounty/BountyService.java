package com.raindropcentral.rdq.service.bounty;

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
 * Service interface for bounty operations.
 * Implementations differ between free and premium versions.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public interface BountyService {

    /**
     * Retrieves a paginated list of bounties visible to the caller.
     *
     * @param page      the zero-based page index to request
     * @param pageSize  the maximum number of bounties to include per page
     * @return a future completing with the ordered bounty slice
     */
    @NotNull CompletableFuture<List<RBounty>> getAllBounties(int page, int pageSize);

    /**
     * Retrieves the active bounty that targets the provided player, if present.
     *
     * @param playerUuid the unique identifier of the player being targeted
     * @return a future completing with the bounty, or empty if none exists
     */
    @NotNull CompletableFuture<Optional<RBounty>> getBountyByPlayer(@NotNull UUID playerUuid);

    /**
     * Creates and persists a bounty for the supplied target on behalf of the commissioner.
     *
     * @param target            the player receiving the bounty
     * @param commissioner      the player funding and issuing the bounty
     * @param rewardItems       the reward items to award upon bounty completion
     * @param rewardCurrencies  the currency rewards keyed by currency identifier
     * @return a future completing with the newly persisted bounty entity
     */
    @NotNull CompletableFuture<RBounty> createBounty(
            @NotNull RDQPlayer target,
            @NotNull Player commissioner,
            @NotNull Set<RewardItem> rewardItems,
            @NotNull Map<String, Double> rewardCurrencies
    );

    /**
     * Deletes the bounty identified by the provided identifier.
     *
     * @param bountyId the unique identifier of the bounty to remove
     * @return a future completing with {@code true} if the bounty was deleted
     */
    @NotNull CompletableFuture<Boolean> deleteBounty(@NotNull Long bountyId);

    /**
     * Persists the provided bounty changes.
     *
     * @param bounty the modified bounty entity to persist
     * @return a future completing with the updated bounty state
     */
    @NotNull CompletableFuture<RBounty> updateBounty(@NotNull RBounty bounty);

    /**
     * Indicates whether the implementation operates with premium-only features.
     *
     * @return {@code true} for premium-capable implementations
     */
    boolean isPremium();

    /**
     * Provides the maximum number of bounties a player may have simultaneously.
     *
     * @return the per-player bounty limit
     */
    int getMaxBountiesPerPlayer();

    /**
     * Provides the maximum number of reward items that may be associated with a bounty.
     *
     * @return the reward item limit for bounty creation
     */
    int getMaxRewardItems();

    /**
     * Determines if the provided player can create a bounty under the current constraints.
     *
     * @param player the player attempting to create a bounty
     * @return {@code true} if the player may issue a bounty
     */
    boolean canCreateBounty(@NotNull Player player);

    /**
     * Calculates the total number of active bounties available.
     *
     * @return a future completing with the total bounty count
     */
    @NotNull CompletableFuture<Integer> getTotalBountyCount();
}
