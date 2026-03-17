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
import com.raindropcentral.rdq.database.entity.bounty.BountyReward;
import com.raindropcentral.rplatform.reward.impl.ItemReward;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Distributes rewards by placing them in a chest at the death location.
 * Falls back to dropping items if chest placement fails.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class ChestRewardDistributor implements RewardDistributor {

    private static final Logger LOGGER = Logger.getLogger(ChestRewardDistributor.class.getName());

    /**
     * Executes distributeRewards.
     */
    @Override
    public @NotNull CompletableFuture<Void> distributeRewards(
            @NotNull Player hunter,
            @NotNull Bounty bounty,
            @NotNull Location location,
            double proportion
    ) {
        return CompletableFuture.runAsync(() -> {
            Location chestLocation = findSafeChestLocation(location);
            
            if (chestLocation != null && placeChest(chestLocation, bounty, proportion)) {
                LOGGER.log(Level.INFO, "Placed bounty chest at " + formatLocation(chestLocation));
            } else {
                // Fallback to dropping items
                LOGGER.log(Level.WARNING, "Failed to place chest, dropping items instead");
                dropItems(location, bounty, proportion);
            }
        });
    }

    private Location findSafeChestLocation(@NotNull Location location) {
        Block block = location.getBlock();
        
        // Try the exact location first
        if (canPlaceChest(block)) {
            return block.getLocation();
        }

        // Try nearby locations
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block nearby = block.getRelative(x, y, z);
                    if (canPlaceChest(nearby)) {
                        return nearby.getLocation();
                    }
                }
            }
        }

        return null;
    }

    private boolean canPlaceChest(@NotNull Block block) {
        return block.getType() == Material.AIR || block.getType().isAir();
    }

    private boolean placeChest(@NotNull Location location, @NotNull Bounty bounty, double proportion) {
        Block block = location.getBlock();
        block.setType(Material.CHEST);

        if (block.getState() instanceof Chest chest) {
            Inventory inventory = chest.getInventory();

            for (BountyReward reward : bounty.getRewards()) {
                if (!(reward.getReward() instanceof ItemReward itemReward)) {
                    continue;
                }

                ItemStack item = itemReward.getItem().clone();
                
                int adjustedAmount = Math.max(1, (int) (item.getAmount() * proportion));
                item.setAmount(adjustedAmount);

                inventory.addItem(item);
            }

            chest.update();
            return true;
        }

        return false;
    }

    private void dropItems(@NotNull Location location, @NotNull Bounty bounty, double proportion) {
        for (BountyReward reward : bounty.getRewards()) {
            if (!(reward.getReward() instanceof ItemReward)) {
                continue;
            }

            ItemReward itemReward = (ItemReward) reward.getReward();
            ItemStack item = itemReward.getItem().clone();
            
            int adjustedAmount = Math.max(1, (int) (item.getAmount() * proportion));
            item.setAmount(adjustedAmount);

            location.getWorld().dropItemNaturally(location, item);
        }
    }

    private String formatLocation(@NotNull Location location) {
        return String.format("%s (%.1f, %.1f, %.1f)",
                location.getWorld().getName(),
                location.getX(),
                location.getY(),
                location.getZ());
    }
}
