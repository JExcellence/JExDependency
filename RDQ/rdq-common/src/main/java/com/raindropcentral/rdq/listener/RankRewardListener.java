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

package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankReward;
import com.raindropcentral.rdq.event.RankAssignedEvent;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.reward.RewardService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Automatically grants rank rewards when a player is assigned a new rank.
 * Listens to RankAssignedEvent and processes rewards asynchronously.
 */
public class RankRewardListener implements Listener {

	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
	
	private final RDQ rdq;
	private final RewardService rewardService;

	/**
	 * Executes RankRewardListener.
	 */
	public RankRewardListener(final @NotNull RDQ rdq) {
		this.rdq = rdq;
		this.rewardService = RewardService.getInstance();
	}

	/**
	 * Executes onRankAssigned.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onRankAssigned(final @NotNull RankAssignedEvent event) {
		final Player player = event.getPlayer();
		final RRank rank = event.getRank();
		
		this.rdq.getExecutor().submit(() -> {
			try {
				final List<RRankReward> rewards = this.getRankRewards(rank);
				
				if (rewards.isEmpty()) {
					LOGGER.fine("No rewards configured for rank: " + rank.getIdentifier());
					return;
				}
				
				for (final RRankReward rankReward : rewards) {
					if (rankReward.isAutoGrant()) {
						Bukkit.getScheduler().runTask(this.rdq.getPlugin(), () -> {
							this.rewardService.grant(player, rankReward.getReward().getReward())
								.thenAccept(success -> {
									if (success) {
										LOGGER.info("Granted reward to " + player.getName() + 
										           " for rank: " + rank.getIdentifier());
									} else {
										LOGGER.warning("Failed to grant reward to " + player.getName() + 
										              " for rank: " + rank.getIdentifier());
									}
								});
						});
					}
				}
			} catch (final Exception exception) {
				LOGGER.log(Level.WARNING, "Failed to grant rank rewards for " + player.getName(), exception);
			}
		});
	}

	private @NotNull List<RRankReward> getRankRewards(final @NotNull RRank rank) {
		try {
			// Rewards are eagerly loaded with the rank entity
			return new ArrayList<>(rank.getRewards());
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to get rewards for rank: " + rank.getIdentifier(), exception);
			return List.of();
		}
	}
}
