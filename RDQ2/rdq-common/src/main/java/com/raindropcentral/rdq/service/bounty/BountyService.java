package com.raindropcentral.rdq.service.bounty;

import com.raindropcentral.rdq.database.entity.bounty.BountyHunterStats;
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

public interface BountyService {

    @NotNull CompletableFuture<List<RBounty>> getAllBounties(int page, int pageSize);

    @NotNull CompletableFuture<Optional<RBounty>> getBountyByPlayer(@NotNull UUID playerUuid);

    @NotNull CompletableFuture<List<RBounty>> getBountiesByCommissioner(@NotNull UUID commissionerUuid);

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

    @NotNull CompletableFuture<Optional<BountyHunterStats>> getHunterStats(@NotNull UUID playerUuid);

    @NotNull CompletableFuture<List<BountyHunterStats>> getTopHunters(int limit, @NotNull String orderBy);

    @NotNull CompletableFuture<BountyHunterStats> recordBountyClaim(
            @NotNull UUID hunterUuid,
            double rewardValue
    );

    @NotNull CompletableFuture<Integer> getHunterRank(@NotNull UUID playerUuid);
}
