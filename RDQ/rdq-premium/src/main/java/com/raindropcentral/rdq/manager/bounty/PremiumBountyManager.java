package com.raindropcentral.rdq.manager.bounty;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import com.raindropcentral.rdq.database.repository.RBountyRepository;
import com.raindropcentral.rdq.database.repository.RDQPlayerRepository;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Premium version bounty manager with full database integration.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public final class PremiumBountyManager implements BountyManager {

    private static final int MAX_BOUNTIES_PREMIUM = -1;
    private static final int MAX_REWARD_ITEMS_PREMIUM = -1;

    private final RBountyRepository bountyRepository;
    private final RDQPlayerRepository playerRepository;

    public PremiumBountyManager(
            final @NotNull RBountyRepository bountyRepository,
            final @NotNull RDQPlayerRepository playerRepository
    ) {
        this.bountyRepository = bountyRepository;
        this.playerRepository = playerRepository;
    }

    @Override
    public @NotNull CompletableFuture<List<RBounty>> getAllBounties(final int page, final int pageSize) {
        return this.bountyRepository.findAllAsync(page, pageSize);
    }

    @Override
    public @NotNull CompletableFuture<Optional<RBounty>> getBountyByPlayer(final @NotNull UUID playerUuid) {
        return this.bountyRepository
                .findByAttributesAsync(Map.of("player.uniqueId", playerUuid))
                .thenApply(Optional::ofNullable);
    }

    @Override
    public @NotNull CompletableFuture<RBounty> createBounty(
            final @NotNull RDQPlayer target,
            final @NotNull Player commissioner,
            final @NotNull Set<RewardItem> rewardItems,
            final @NotNull Map<String, Double> rewardCurrencies
    ) {
        final RBounty bounty = new RBounty();
        bounty.setPlayer(target);
        bounty.setCommissioner(commissioner.getUniqueId());
        bounty.setRewardItems(rewardItems);

        return this.bountyRepository.createAsync(bounty);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> deleteBounty(final @NotNull Long bountyId) {
        return this.bountyRepository.deleteAsync(bountyId);
    }

    @Override
    public @NotNull CompletableFuture<RBounty> updateBounty(final @NotNull RBounty bounty) {
        return this.bountyRepository.updateAsync(bounty);
    }

    @Override
    public int getMaxBountiesPerPlayer() {
        return MAX_BOUNTIES_PREMIUM;
    }

    @Override
    public int getMaxRewardItems() {
        return MAX_REWARD_ITEMS_PREMIUM;
    }

    @Override
    public boolean canCreateBounty(final @NotNull Player player) {
        return true;
    }

    @Override
    public @NotNull CompletableFuture<Integer> getTotalBountyCount() {
        return this.bountyRepository
                .findAllAsync(1, Integer.MAX_VALUE)
                .thenApply(List::size);
    }
}