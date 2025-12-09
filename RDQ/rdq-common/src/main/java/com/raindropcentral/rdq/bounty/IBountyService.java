package com.raindropcentral.rdq.bounty;

import com.raindropcentral.rdq.database.entity.bounty.Bounty;
import com.raindropcentral.rdq.database.entity.bounty.BountyHunter;
import com.raindropcentral.rdq.database.entity.bounty.BountyReward;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface IBountyService {

    CompletableFuture<List<Bounty>> findAll(int page, int pageSize);

    CompletableFuture<Bounty> findPlayerBounty(@NotNull UUID uniqueId);

    CompletableFuture<List<Bounty>> findBountiesByCommissioner(@NotNull UUID commissionerUniqueId);

    CompletableFuture<Bounty> create(
            @NotNull UUID targetUniqueId,
            @NotNull UUID commissionerUniqueId
    );

    CompletableFuture<Bounty> create(
            @NotNull UUID targetUniqueId,
            @NotNull UUID commissionerUniqueId,
            @NotNull List<BountyReward> rewards
    );

    CompletableFuture<Bounty> update(
            @NotNull Bounty bounty
    );

    CompletableFuture<Boolean> delete(
            @NotNull Bounty bounty
    );

    CompletableFuture<Integer> getTotalBounties();

    CompletableFuture<BountyHunter> getBountyHunter(@NotNull RDQPlayer player);

    CompletableFuture<List<BountyHunter>> getTopHunters(int limit, @NotNull String orderBy);

    CompletableFuture<BountyHunter> claim(
            @NotNull RDQPlayer player,
            double rewardValue
    );

    //TODO ADD ENTIRE LEVEL RANK SYSTEM FOR HUNTERS
    CompletableFuture<Integer> getHunterLevel(@NotNull UUID uniqueId);

    boolean isPremium();

    boolean canCreateBounty(@NotNull Player player);

    int getMaxBountiesPerCommissioner();

    int getMaxBountyRewardsPerTarget();
}
