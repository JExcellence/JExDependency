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

package com.raindropcentral.rplatform.reward;

import com.raindropcentral.rplatform.reward.event.RewardFailedEvent;
import com.raindropcentral.rplatform.reward.event.RewardGrantEvent;
import com.raindropcentral.rplatform.reward.event.RewardGrantedEvent;
import com.raindropcentral.rplatform.reward.lifecycle.LifecycleRegistry;
import com.raindropcentral.rplatform.reward.metrics.RewardMetrics;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the RewardService API type.
 */
public final class RewardService {

    private static final Logger LOGGER = Logger.getLogger(RewardService.class.getName());
    private static final RewardService INSTANCE = new RewardService();

    private final LifecycleRegistry lifecycleRegistry = LifecycleRegistry.getInstance();
    private final RewardMetrics metrics = RewardMetrics.getInstance();

    private RewardService() {}

    /**
     * Gets instance.
     */
    public static RewardService getInstance() {
        return INSTANCE;
    }

    /**
     * Executes grant.
     */
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player, @NotNull AbstractReward reward) {
        if (!lifecycleRegistry.executeBeforeGrant(player, reward)) {
            return CompletableFuture.completedFuture(false);
        }

        RewardGrantEvent event = new RewardGrantEvent(player, reward);
        Bukkit.getPluginManager().callEvent(event);
        
        if (event.isCancelled()) {
            return CompletableFuture.completedFuture(false);
        }

        long startTime = System.nanoTime();

        return reward.grant(player).thenApply(success -> {
            long duration = System.nanoTime() - startTime;
            metrics.recordGrant(reward.getTypeId(), duration, success);
            
            if (success) {
                Bukkit.getPluginManager().callEvent(new RewardGrantedEvent(player, reward));
                lifecycleRegistry.executeAfterGrant(player, reward, true);
            } else {
                lifecycleRegistry.executeAfterGrant(player, reward, false);
            }
            
            return success;
        }).exceptionally(throwable -> {
            long duration = System.nanoTime() - startTime;
            metrics.recordGrant(reward.getTypeId(), duration, false);
            
            LOGGER.log(Level.SEVERE, "Error granting reward: " + reward.getTypeId(), throwable);
            Bukkit.getPluginManager().callEvent(new RewardFailedEvent(player, reward, throwable));
            lifecycleRegistry.executeOnError(player, reward, throwable);
            
            return false;
        });
    }

    /**
     * Executes grantAll.
     */
    public @NotNull CompletableFuture<Boolean> grantAll(@NotNull Player player, @NotNull List<AbstractReward> rewards) {
        if (rewards.isEmpty()) {
            return CompletableFuture.completedFuture(true);
        }

        CompletableFuture<Boolean>[] futures = rewards.stream()
            .map(reward -> grant(player, reward))
            .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures)
            .thenApply(v -> {
                for (CompletableFuture<Boolean> future : futures) {
                    if (!future.join()) {
                        return false;
                    }
                }
                return true;
            });
    }

    /**
     * Executes calculateTotalValue.
     */
    public double calculateTotalValue(@NotNull List<AbstractReward> rewards) {
        return rewards.stream()
            .mapToDouble(AbstractReward::getEstimatedValue)
            .sum();
    }
}
