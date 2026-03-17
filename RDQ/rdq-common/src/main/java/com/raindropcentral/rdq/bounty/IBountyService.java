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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Represents the IBountyService API type.
 */
public interface IBountyService {

    /**
     * Returns paged bounties.
     *
     * @param page page index
     * @param pageSize page size
     * @return future containing bounty page
     */
    CompletableFuture<List<Bounty>> findAll(int page, int pageSize);

    /**
     * Finds the active bounty for a player.
     *
     * @param uniqueId target player UUID
     * @return future containing the matching bounty
     */
    CompletableFuture<Bounty> findPlayerBounty(@NotNull UUID uniqueId);

    /**
     * Finds bounties created by a commissioner.
     *
     * @param commissionerUniqueId commissioner UUID
     * @return future containing commissioner bounties
     */
    CompletableFuture<List<Bounty>> findBountiesByCommissioner(@NotNull UUID commissionerUniqueId);

    /**
     * Creates a bounty without explicit rewards.
     *
     * @param targetUniqueId target UUID
     * @param commissionerUniqueId commissioner UUID
     * @return future containing the created bounty
     */
    CompletableFuture<Bounty> create(
            @NotNull UUID targetUniqueId,
            @NotNull UUID commissionerUniqueId
    );

    /**
     * Creates a bounty with rewards.
     *
     * @param targetUniqueId target UUID
     * @param commissionerUniqueId commissioner UUID
     * @param rewards bounty rewards
     * @return future containing the created bounty
     */
    CompletableFuture<Bounty> create(
            @NotNull UUID targetUniqueId,
            @NotNull UUID commissionerUniqueId,
            @NotNull List<BountyReward> rewards
    );

    /**
     * Updates a bounty.
     *
     * @param bounty bounty to update
     * @return future containing updated bounty
     */
    CompletableFuture<Bounty> update(
            @NotNull Bounty bounty
    );

    /**
     * Deletes a bounty.
     *
     * @param bounty bounty to delete
     * @return future containing deletion outcome
     */
    CompletableFuture<Boolean> delete(
            @NotNull Bounty bounty
    );

    /**
     * Returns the total number of bounties.
     *
     * @return future containing bounty count
     */
    CompletableFuture<Integer> getTotalBounties();

    /**
     * Returns hunter stats for a player.
     *
     * @param player player entity
     * @return future containing hunter data
     */
    CompletableFuture<BountyHunter> getBountyHunter(@NotNull RDQPlayer player);

    /**
     * Returns top hunters.
     *
     * @param limit maximum rows
     * @param orderBy sort field
     * @return future containing top hunters
     */
    CompletableFuture<List<BountyHunter>> getTopHunters(int limit, @NotNull String orderBy);

    /**
     * Claims a bounty for a player.
     *
     * @param player claiming player
     * @param rewardValue reward value
     * @return future containing updated hunter state
     */
    CompletableFuture<BountyHunter> claim(
            @NotNull RDQPlayer player,
            double rewardValue
    );

    //TODO ADD ENTIRE LEVEL RANK SYSTEM FOR HUNTERS
    /**
     * Returns hunter level.
     *
     * @param uniqueId hunter UUID
     * @return future containing level
     */
    CompletableFuture<Integer> getHunterLevel(@NotNull UUID uniqueId);

    /**
     * Returns whether premium features are enabled.
     *
     * @return {@code true} when premium mode is active
     */
    boolean isPremium();

    /**
     * Returns whether a player can create a bounty.
     *
     * @param player player to validate
     * @return {@code true} when creation is allowed
     */
    boolean canCreateBounty(@NotNull Player player);

    /**
     * Returns max bounties per commissioner.
     *
     * @return configured commissioner limit
     */
    int getMaxBountiesPerCommissioner();

    /**
     * Returns max rewards per bounty.
     *
     * @return configured reward limit
     */
    int getMaxBountyRewardsPerTarget();
}
