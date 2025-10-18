package com.raindropcentral.rdq.service.bounty;

import com.raindropcentral.rdq.database.entity.bounty.RBounty;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.reward.RewardItem;
import com.raindropcentral.rdq.database.repository.RBountyRepository;
import com.raindropcentral.rdq.database.repository.RDQPlayerRepository;
import com.raindropcentral.rdq.service.BountyService;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Premium version of bounty service with full database integration.
 * <p>
 * This implementation provides complete functionality:
 * <ul>
 * <li>Full CRUD operations on bounties</li>
 * <li>Unlimited bounties per player</li>
 * <li>Unlimited reward items</li>
 * <li>Database persistence</li>
 * <li>Async operations</li>
 * </ul>
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public final class PremiumBountyService implements BountyService {

    private static final int MAX_BOUNTIES_PREMIUM = -1;
    private static final int MAX_REWARD_ITEMS_PREMIUM = -1;

    private final RBountyRepository bountyRepository;
    private final RDQPlayerRepository playerRepository;

    public PremiumBountyService(
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
        final RBounty bounty = new RBounty(target, commissioner);
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
    public boolean isPremium() {
        return true;
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