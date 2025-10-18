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

    @Override
    public @NotNull CompletableFuture<List<RBounty>> getAllBounties(final int page, final int pageSize) {
        return CompletableFuture.completedFuture(new ArrayList<>(this.bounties.values()));
    }

    @Override
    public @NotNull CompletableFuture<Optional<RBounty>> getBountyByPlayer(final @NotNull UUID playerUuid) {
        return CompletableFuture.completedFuture(Optional.ofNullable(this.bountyByPlayer.get(playerUuid)));
    }

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

    @Override
    public @NotNull CompletableFuture<Boolean> deleteBounty(final @NotNull Long bountyId) {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public @NotNull CompletableFuture<RBounty> updateBounty(final @NotNull RBounty bounty) {
        return CompletableFuture.failedFuture(
                new UnsupportedOperationException("Bounty updates require premium version")
        );
    }

    @Override
    public int getMaxBountiesPerPlayer() {
        return MAX_BOUNTIES_FREE;
    }

    @Override
    public int getMaxRewardItems() {
        return MAX_REWARD_ITEMS_FREE;
    }

    @Override
    public boolean canCreateBounty(final @NotNull Player player) {
        return false;
    }

    @Override
    public @NotNull CompletableFuture<Integer> getTotalBountyCount() {
        return CompletableFuture.completedFuture(this.bounties.size());
    }
}