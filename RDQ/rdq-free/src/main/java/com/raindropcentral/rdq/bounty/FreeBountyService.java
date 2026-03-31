/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

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

    private static FreeBountyService instance;
    private final List<Bounty> mockBounties = new ArrayList<>();

    /**
     * Private constructor.
     */
    private FreeBountyService() {
        // Initialize with empty mock data
    }

    /**
     * Initializes the Free Bounty Service.
     * Note: Free version doesn't use repositories or RDQ instance.
     *
     * @param rdq ignored in free version (for API compatibility)
     * @return the initialized service instance
     */
    public static FreeBountyService initialize(Object rdq) {
        if (instance == null) {
            instance = new FreeBountyService();
        }
        return instance;
    }

    /**
     * Gets the initialized instance.
     * 
     * @return the service instance
     * @throws IllegalStateException if not initialized
     */
    public static FreeBountyService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("FreeBountyService not initialized. Call initialize() first.");
        }
        return instance;
    }

    /**
     * Executes findAll.
     */
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

    /**
     * Executes findPlayerBounty.
     */
    @Override
    public CompletableFuture<Bounty> findPlayerBounty(@NotNull UUID uniqueId) {
        return CompletableFuture.supplyAsync(() -> 
            mockBounties.stream()
                .filter(bounty -> bounty.getTargetUniqueId().equals(uniqueId))
                .findFirst()
                .orElse(null)
        );
    }

    /**
     * Executes findBountiesByCommissioner.
     */
    @Override
    public CompletableFuture<List<Bounty>> findBountiesByCommissioner(@NotNull UUID commissionerUniqueId) {
        return CompletableFuture.supplyAsync(() -> 
            mockBounties.stream()
                .filter(bounty -> bounty.getCommissionerUniqueId().equals(commissionerUniqueId))
                .toList()
        );
    }

    /**
     * Executes create.
     */
    @Override
    public CompletableFuture<Bounty> create(
            @NotNull UUID targetUniqueId,
            @NotNull UUID commissionerUniqueId
    ) {
        return create(targetUniqueId, commissionerUniqueId, new ArrayList<>());
    }

    /**
     * Executes create.
     */
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

    /**
     * Executes update.
     */
    @Override
    public CompletableFuture<Bounty> update(@NotNull Bounty bounty) {
        return CompletableFuture.supplyAsync(() -> bounty);
    }

    /**
     * Executes delete.
     */
    @Override
    public CompletableFuture<Boolean> delete(@NotNull Bounty bounty) {
        return CompletableFuture.supplyAsync(() -> {
            return mockBounties.remove(bounty);
        });
    }

    /**
     * Gets totalBounties.
     */
    @Override
    public CompletableFuture<Integer> getTotalBounties() {
        return CompletableFuture.completedFuture(mockBounties.size());
    }

    /**
     * Gets bountyHunter.
     */
    @Override
    public CompletableFuture<BountyHunter> getBountyHunter(@NotNull RDQPlayer player) {
        return CompletableFuture.supplyAsync(() -> null);
    }

    /**
     * Gets topHunters.
     */
    @Override
    public CompletableFuture<List<BountyHunter>> getTopHunters(int limit, @NotNull String orderBy) {
        return CompletableFuture.supplyAsync(ArrayList::new);
    }

    /**
     * Executes claim.
     */
    @Override
    public CompletableFuture<BountyHunter> claim(@NotNull RDQPlayer player, double rewardValue) {
        return CompletableFuture.supplyAsync(() -> null);
    }

    /**
     * Gets hunterLevel.
     */
    @Override
    public CompletableFuture<Integer> getHunterLevel(@NotNull UUID uniqueId) {
        return CompletableFuture.completedFuture(FREE_HUNTER_LEVEL);
    }

    /**
     * Returns whether premium.
     */
    @Override
    public boolean isPremium() {
        return false;
    }

    /**
     * Executes canCreateBounty.
     */
    @Override
    public boolean canCreateBounty(@NotNull Player player) {
        var playerBounties = mockBounties.stream().filter(b -> b.getCommissionerUniqueId().equals(player.getUniqueId())).count();
        return playerBounties < MAX_BOUNTIES_PER_COMMISSIONER;
    }

    /**
     * Gets maxBountiesPerCommissioner.
     */
    @Override
    public int getMaxBountiesPerCommissioner() {
        return MAX_BOUNTIES_PER_COMMISSIONER;
    }

    /**
     * Gets maxBountyRewardsPerTarget.
     */
    @Override
    public int getMaxBountyRewardsPerTarget() {
        return MAX_REWARDS_PER_BOUNTY;
    }
}
