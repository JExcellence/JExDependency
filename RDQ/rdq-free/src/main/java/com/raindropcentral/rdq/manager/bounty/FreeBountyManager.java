package com.raindropcentral.rdq.manager.bounty;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Free version bounty manager with in-memory storage and limited features.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public final class FreeBountyManager implements BountyManager {

    private static final int MAX_BOUNTIES_FREE = 1;
    private static final int MAX_REWARD_ITEMS_FREE = 3;

    private final Map<Long, RBounty> bounties = new ConcurrentHashMap<>();
    private final Map<UUID, RBounty> bountyByPlayer = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong();

    /**
     * Retrieves the list of bounties stored in memory for the free edition.
     *
     * @param page     the requested page index; ignored in the free implementation
     * @param pageSize the requested number of entries per page; ignored in the free implementation
     * @return a completed future containing all known bounties
     */
    @Override
    public @NotNull CompletableFuture<List<RBounty>> getAllBounties(final int page, final int pageSize) {
        return CompletableFuture.completedFuture(new ArrayList<>(this.bounties.values()));
    }

    /**
     * Locates a bounty by the owning player's unique identifier.
     *
     * @param playerUuid the UUID of the player whose bounty should be retrieved
     * @return a completed future with the bounty if one is present for the player
     */
    @Override
    public @NotNull CompletableFuture<Optional<RBounty>> getBountyByPlayer(final @NotNull UUID playerUuid) {
        return CompletableFuture.completedFuture(Optional.ofNullable(this.bountyByPlayer.get(playerUuid)));
    }

    /**
     * Attempts to create a new bounty targeting the supplied player.
     *
     * @param target           the player that would be targeted by the bounty
     * @param commissioner     the player requesting the bounty
     * @param rewardItems      the items to be offered for the bounty completion
     * @param rewardCurrencies the currency rewards associated with the bounty
     * @return a failed future indicating the operation is not supported in the free edition
     */
    @Override
    public @NotNull CompletableFuture<RBounty> createBounty(
            final @NotNull RDQPlayer target,
            final @NotNull Player commissioner,
            final @NotNull Set<RewardItem> rewardItems,
            final @NotNull Map<String, Double> rewardCurrencies
    ) {
        return CompletableFuture.failedFuture(
                new UnsupportedOperationException("Bounty creation requires premium version")
        );
    }

    /**
     * Attempts to delete the specified bounty.
     *
     * @param bountyId the identifier of the bounty to delete
     * @return a completed future indicating deletion is not supported in the free edition
     */
    @Override
    public @NotNull CompletableFuture<Boolean> deleteBounty(final @NotNull Long bountyId) {
        return CompletableFuture.completedFuture(false);
    }

    /**
     * Attempts to apply updates to an existing bounty.
     *
     * @param bounty the bounty containing desired updates
     * @return a failed future indicating updates are not supported in the free edition
     */
    @Override
    public @NotNull CompletableFuture<RBounty> updateBounty(final @NotNull RBounty bounty) {
        return CompletableFuture.failedFuture(
                new UnsupportedOperationException("Bounty updates require premium version")
        );
    }

    /**
     * Provides the maximum number of bounties a player may create in the free edition.
     *
     * @return the allowed number of bounties per player
     */
    @Override
    public int getMaxBountiesPerPlayer() {
        return MAX_BOUNTIES_FREE;
    }

    /**
     * Provides the maximum number of reward items a bounty may include in the free edition.
     *
     * @return the allowed number of reward items
     */
    @Override
    public int getMaxRewardItems() {
        return MAX_REWARD_ITEMS_FREE;
    }

    /**
     * Indicates whether the provided player may create a bounty.
     *
     * @param player the player attempting to create a bounty
     * @return {@code false} because bounty creation is reserved for the premium edition
     */
    @Override
    public boolean canCreateBounty(final @NotNull Player player) {
        return false;
    }

    /**
     * Retrieves the number of bounties currently tracked by the free manager.
     *
     * @return a completed future containing the total bounty count
     */
    @Override
    public @NotNull CompletableFuture<Integer> getTotalBountyCount() {
        return CompletableFuture.completedFuture(this.bounties.size());
    }
}