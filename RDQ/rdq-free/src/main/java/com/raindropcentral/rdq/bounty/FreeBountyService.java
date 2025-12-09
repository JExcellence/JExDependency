package com.raindropcentral.rdq.bounty;

import com.raindropcentral.rdq.database.entity.bounty.Bounty;
import com.raindropcentral.rdq.database.entity.bounty.BountyHunter;
import com.raindropcentral.rdq.database.entity.bounty.BountyReward;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Free version of the Bounty Service with limited functionality.
 * Uses mock data and has restrictions on bounty creation and rewards.
 * 
 * Limitations:
 * - Maximum 1 bounty per commissioner
 * - Maximum 1 reward per bounty
 * - No database persistence (mock data only)
 * - Limited hunter stats tracking
 * 
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class FreeBountyService implements IBountyService {

    private static final int MAX_BOUNTIES_PER_COMMISSIONER = 1;
    private static final int MAX_REWARDS_PER_BOUNTY = 1;
    private static final int FREE_HUNTER_LEVEL = 1;

    private final List<Bounty> mockBounties = new ArrayList<>();

    /**
     * No-arg constructor required for ServiceLoader.
     */
    public FreeBountyService() {
        // Initialize with empty mock data
    }

    @Override
    public CompletableFuture<List<Bounty>> findAll(int page, int pageSize) {
        return CompletableFuture.supplyAsync(() -> {
            var start = page * pageSize;
            var end = Math.min(start + pageSize, mockBounties.size());
            
            if (start >= mockBounties.size()) {
                return new ArrayList<>();
            }
            
            return new ArrayList<>(mockBounties.subList(start, end));
        });
    }

    @Override
    public CompletableFuture<Bounty> findPlayerBounty(@NotNull UUID uniqueId) {
        return CompletableFuture.supplyAsync(() -> 
            mockBounties.stream()
                .filter(bounty -> bounty.getTargetUniqueId().equals(uniqueId))
                .findFirst()
                .orElse(null)
        );
    }

    @Override
    public CompletableFuture<List<Bounty>> findBountiesByCommissioner(@NotNull UUID commissionerUniqueId) {
        return CompletableFuture.supplyAsync(() -> 
            mockBounties.stream()
                .filter(bounty -> bounty.getCommissionerUniqueId().equals(commissionerUniqueId))
                .toList()
        );
    }

    @Override
    public CompletableFuture<Bounty> create(
            @NotNull UUID targetUniqueId,
            @NotNull UUID commissionerUniqueId
    ) {
        return create(targetUniqueId, commissionerUniqueId, new ArrayList<>());
    }

    @Override
    public CompletableFuture<Bounty> create(
            @NotNull UUID targetUniqueId,
            @NotNull UUID commissionerUniqueId,
            @NotNull List<BountyReward> rewards
    ) {
        return CompletableFuture.supplyAsync(() -> {
            // Check commissioner limit
            var commissionerBounties = mockBounties.stream()
                .filter(b -> b.getCommissionerUniqueId().equals(commissionerUniqueId))
                .count();
            
            if (commissionerBounties >= MAX_BOUNTIES_PER_COMMISSIONER) {
                throw new IllegalStateException(
                    "Free version: Maximum " + MAX_BOUNTIES_PER_COMMISSIONER + " bounty per player. Upgrade to Premium!"
                );
            }
            
            // Limit rewards
            var limitedRewards = rewards.stream().limit(MAX_REWARDS_PER_BOUNTY).toList();
            
            // Create mock bounty (not persisted to database)
            var bounty = new Bounty(targetUniqueId, commissionerUniqueId, rewards);
            // Note: In free version, rewards are simplified
            
            mockBounties.add(bounty);
            
            return bounty;
        });
    }

    @Override
    public CompletableFuture<Bounty> update(@NotNull Bounty bounty) {
        return CompletableFuture.supplyAsync(() -> bounty);
    }

    @Override
    public CompletableFuture<Boolean> delete(@NotNull Bounty bounty) {
        return CompletableFuture.supplyAsync(() -> {
            return mockBounties.remove(bounty);
        });
    }

    @Override
    public CompletableFuture<Integer> getTotalBounties() {
        return CompletableFuture.completedFuture(mockBounties.size());
    }

    @Override
    public CompletableFuture<BountyHunter> getBountyHunter(@NotNull RDQPlayer player) {
        return CompletableFuture.supplyAsync(() -> null);
    }

    @Override
    public CompletableFuture<List<BountyHunter>> getTopHunters(int limit, @NotNull String orderBy) {
        return CompletableFuture.supplyAsync(ArrayList::new);
    }

    @Override
    public CompletableFuture<BountyHunter> claim(@NotNull RDQPlayer player, double rewardValue) {
        return CompletableFuture.supplyAsync(() -> null);
    }

    @Override
    public CompletableFuture<Integer> getHunterLevel(@NotNull UUID uniqueId) {
        return CompletableFuture.completedFuture(FREE_HUNTER_LEVEL);
    }

    @Override
    public boolean isPremium() {
        return false;
    }

    @Override
    public boolean canCreateBounty(@NotNull Player player) {
        var playerBounties = mockBounties.stream().filter(b -> b.getCommissionerUniqueId().equals(player.getUniqueId())).count();
        return playerBounties < MAX_BOUNTIES_PER_COMMISSIONER;
    }

    @Override
    public int getMaxBountiesPerCommissioner() {
        return MAX_BOUNTIES_PER_COMMISSIONER;
    }

    @Override
    public int getMaxBountyRewardsPerTarget() {
        return MAX_REWARDS_PER_BOUNTY;
    }
}
