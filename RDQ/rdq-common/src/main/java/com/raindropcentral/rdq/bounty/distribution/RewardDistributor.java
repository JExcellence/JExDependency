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

package com.raindropcentral.rdq.bounty.distribution;

import com.raindropcentral.rdq.database.entity.bounty.Bounty;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Interface for distributing bounty rewards to hunters.
 * Different implementations handle different distribution modes.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public interface RewardDistributor {

    /**
     * Distributes the rewards from a bounty to the hunter.
     *
     * @param hunter the player receiving the rewards
     * @param bounty the bounty being claimed
     * @param location the location where the target died
     * @param proportion the proportion of the bounty this hunter receives (0.0 to 1.0)
     * @return a future that completes when rewards are distributed
     */
    @NotNull
    CompletableFuture<Void> distributeRewards(
            @NotNull Player hunter,
            @NotNull Bounty bounty,
            @NotNull Location location,
            double proportion
    );
}
