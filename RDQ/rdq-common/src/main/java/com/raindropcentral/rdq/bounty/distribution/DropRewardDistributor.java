package com.raindropcentral.rdq.bounty.distribution;

import com.raindropcentral.rdq.database.entity.bounty.Bounty;
import com.raindropcentral.rdq.database.entity.bounty.BountyReward;
import com.raindropcentral.rplatform.reward.impl.ItemReward;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Distributes rewards by dropping them at the death location.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class DropRewardDistributor implements RewardDistributor {

    private static final Logger LOGGER = Logger.getLogger(DropRewardDistributor.class.getName());

    @Override
    public @NotNull CompletableFuture<Void> distributeRewards(
            @NotNull Player hunter,
            @NotNull Bounty bounty,
            @NotNull Location location,
            double proportion
    ) {
        return CompletableFuture.runAsync(() -> {
            dropItems(location, bounty, proportion);
            LOGGER.log(Level.INFO, "Dropped bounty rewards at " + formatLocation(location));
        });
    }

    private void dropItems(@NotNull Location location, @NotNull Bounty bounty, double proportion) {
        for (BountyReward reward : bounty.getRewards()) {
            if (!(reward.getReward() instanceof ItemReward)) {
                continue;
            }

            ItemReward itemReward = (ItemReward) reward.getReward();
            ItemStack item = itemReward.getItem().clone();
            
            // Apply proportion to item amount
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
