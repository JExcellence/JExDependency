package com.raindropcentral.rdq.service.bounty;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import com.raindropcentral.rdq.database.repository.RBountyRepository;
import com.raindropcentral.rdq.service.BountyService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Premium version of bounty service with persistent database storage.
 * <p>
 * This implementation provides full functionality for the premium version by
 * delegating all operations to the {@link RDQ}. It assumes no
 * hardcoded limits, as those are typically handled by configuration or
 * permissions in a premium plugin.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public final class PremiumBountyService implements BountyService {

    private final RBountyRepository bountyRepository;

    /**
     * Constructs the premium bounty service.
     *
     */
    public PremiumBountyService(@NotNull RBountyRepository bountyRepository) {
        this.bountyRepository = bountyRepository;
    }

    @Override
    public @NotNull CompletableFuture<List<RBounty>> getAllBounties(int page, int pageSize) {
        return this.bountyRepository.findAllAsync(page, pageSize);
    }

    @Override
    public @NotNull CompletableFuture<Optional<RBounty>> getBountyByPlayer(@NotNull UUID playerUuid) {
        return this.bountyRepository.findByPlayerAsync(playerUuid);
    }

    @Override
    public @NotNull CompletableFuture<RBounty> createBounty(
            @NotNull RDQPlayer target,
            @NotNull Player commissioner,
            @NotNull Set<RewardItem> rewardItems,
            @NotNull Map<String, Double> rewardCurrencies
    ) {
        RBounty bounty = new RBounty(target, commissioner, rewardItems, rewardCurrencies);
        return this.bountyRepository.createAsync(bounty);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> deleteBounty(@NotNull Long bountyId) {
        return this.bountyRepository.deleteAsync(bountyId);
    }

    @Override
    public @NotNull CompletableFuture<RBounty> updateBounty(@NotNull RBounty bounty) {
        return this.bountyRepository.updateAsync(bounty);
    }

    @Override
    public boolean isPremium() {
        return true;
    }

    @Override
    public int getMaxBountiesPerPlayer() {
        // Premium version has no hardcoded limit. -1 signifies unlimited.
        return -1;
    }



    @Override
    public int getMaxRewardItems() {
        // Premium version has no hardcoded limit. -1 signifies unlimited.
        return -1;
    }

    @Override
    public boolean canCreateBounty(@NotNull Player player) {
        // In premium, this is typically controlled by permissions, not a hard limit.
        // For example: return player.hasPermission("rdq.bounty.create");
        return true;
    }

    @Override
    public @NotNull CompletableFuture<Integer> getTotalBountyCount() {
        return this.bountyRepository.findAllAsync(0, 1000).thenApply(List::size);
    }
}