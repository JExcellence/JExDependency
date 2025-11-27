package com.raindropcentral.rdq.bounty.distribution;

import com.raindropcentral.rdq.bounty.dto.Bounty;
import com.raindropcentral.rdq.bounty.dto.RewardItem;
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
 * <p>
 * Items are added directly to the hunter's inventory.
 * If the inventory is full, excess items are dropped at the hunter's location.
 * Currency rewards are credited to the hunter's economy balance.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 2.0.0
 */
public final class InstantRewardDistributor implements RewardDistributor {
    
    private static final Logger LOGGER = Logger.getLogger(InstantRewardDistributor.class.getName());
    
    @Override
    public @NotNull CompletableFuture<Void> distributeRewards(
            @NotNull Player hunter,
            @NotNull Bounty bounty,
            @NotNull Location location
    ) {
        Objects.requireNonNull(hunter, "hunter cannot be null");
        Objects.requireNonNull(bounty, "bounty cannot be null");
        Objects.requireNonNull(location, "location cannot be null");
        
        return CompletableFuture.runAsync(() -> {
            // Distribute item rewards
            distributeItems(hunter, bounty.rewardItems());
            
            // Distribute currency rewards
            distributeCurrencies(hunter, bounty.rewardCurrencies());
            
            LOGGER.log(Level.INFO, "Distributed rewards to " + hunter.getName() + 
                    " for bounty #" + bounty.id());
        });
    }
    
    /**
     * Distributes item rewards to the hunter's inventory.
     * Drops excess items if inventory is full.
     *
     * @param hunter the hunter receiving items
     * @param rewardItems the items to distribute
     */
    private void distributeItems(@NotNull Player hunter, @NotNull Set<RewardItem> rewardItems) {
        PlayerInventory inventory = hunter.getInventory();
        List<ItemStack> excessItems = new ArrayList<>();
        
        for (RewardItem rewardItem : rewardItems) {
            ItemStack item = rewardItem.item().clone();
            item.setAmount(rewardItem.amount());
            
            // Try to add item to inventory
            HashMap<Integer, ItemStack> leftover = inventory.addItem(item);
            
            // If there are leftover items, collect them for dropping
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
    
    /**
     * Distributes currency rewards to the hunter's economy balance.
     * <p>
     * Note: This is a placeholder implementation. Full economy integration
     * will be added when the economy system is available.
     * </p>
     *
     * @param hunter the hunter receiving currencies
     * @param rewardCurrencies the currencies to distribute
     */
    private void distributeCurrencies(
            @NotNull Player hunter,
            @NotNull Map<String, Double> rewardCurrencies
    ) {
        if (rewardCurrencies.isEmpty()) {
            return;
        }
        
        // TODO: Integrate with economy system (JExEconomy)
        // For now, just log the currency distribution
        for (Map.Entry<String, Double> entry : rewardCurrencies.entrySet()) {
            String currencyName = entry.getKey();
            Double amount = entry.getValue();
            
            LOGGER.log(Level.INFO, "Would credit " + amount + " " + currencyName + 
                    " to " + hunter.getName() + " (economy integration pending)");
        }
    }
}
