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

    int getMaxBountiesPerPlayer();

    int getMaxRewardItems();

    boolean canCreateBounty(@NotNull Player player);

    @NotNull CompletableFuture<Integer> getTotalBountyCount();
}