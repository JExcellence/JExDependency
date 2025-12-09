package com.raindropcentral.rdq.bounty.distribution;

import com.raindropcentral.rdq.database.entity.bounty.Bounty;
import com.raindropcentral.rdq.database.entity.bounty.BountyReward;
import com.raindropcentral.rdq.reward.ItemReward;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Distributes rewards instantly to the hunter's inventory.
 * Items are added directly to inventory, excess items are dropped.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class InstantRewardDistributor implements RewardDistributor {

    private static final Logger LOGGER = Logger.getLogger(InstantRewardDistributor.class.getName());

    @Override
    public @NotNull CompletableFuture<Void> distributeRewards(
            @NotNull Player hunter,
            @NotNull Bounty bounty,
            @NotNull Location location,
            double proportion
    ) {
        return CompletableFuture.runAsync(() -> {
            distributeItems(hunter, bounty, proportion);
            LOGGER.log(Level.INFO, "Distributed " + (proportion * 100) + "% of bounty rewards to " + hunter.getName());
        });
    }

    private void distributeItems(@NotNull Player hunter, @NotNull Bounty bounty, double proportion) {
        PlayerInventory inventory = hunter.getInventory();
        List<ItemStack> excessItems = new ArrayList<>();

        for (BountyReward reward : bounty.getRewards()) {
            if (reward.getReward().getType() != com.raindropcentral.rdq.reward.Reward.Type.ITEM) {
                continue;
            }

            ItemReward itemReward = (ItemReward) reward.getReward();
            ItemStack item = itemReward.getItem().clone();
            
            // Apply proportion to item amount
            int adjustedAmount = Math.max(1, (int) (item.getAmount() * proportion));
            item.setAmount(adjustedAmount);

            // Try to add item to inventory
            HashMap<Integer, ItemStack> leftover = inventory.addItem(item);

            // Collect leftover items for dropping
            if (!leftover.isEmpty()) {
                excessItems.addAll(leftover.values());
            }
        }

        // Drop excess items at hunter's location if inventory was full
        if (!excessItems.isEmpty()) {
            Location dropLocation = hunter.getLocation();
            for (ItemStack excessItem : excessItems) {
                hunter.getWorld().dropItemNaturally(dropLocation, excessItem);
            }

            LOGGER.log(Level.INFO, "Dropped " + excessItems.size() +
                    " excess items for " + hunter.getName() + " (inventory full)");
        }
    }
}
