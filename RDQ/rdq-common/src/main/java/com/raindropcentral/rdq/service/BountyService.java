package com.raindropcentral.rdq.service;

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

    @NotNull CompletableFuture<List<RBounty>> getAllBounties(int page, int pageSize);

    @NotNull CompletableFuture<Optional<RBounty>> getBountyByPlayer(@NotNull UUID playerUuid);

    @NotNull CompletableFuture<RBounty> createBounty(
            @NotNull RDQPlayer target,
            @NotNull Player commissioner,
            @NotNull Set<RewardItem> rewardItems,
            @NotNull Map<String, Double> rewardCurrencies
    );

    @NotNull CompletableFuture<Boolean> deleteBounty(@NotNull Long bountyId);

    @NotNull CompletableFuture<RBounty> updateBounty(@NotNull RBounty bounty);

    boolean isPremium();

    int getMaxBountiesPerPlayer();

    int getMaxRewardItems();

    boolean canCreateBounty(@NotNull Player player);

    @NotNull CompletableFuture<Integer> getTotalBountyCount();
}