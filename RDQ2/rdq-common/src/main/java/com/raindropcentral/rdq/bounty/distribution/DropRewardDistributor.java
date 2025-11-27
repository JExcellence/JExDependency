package com.raindropcentral.rdq.bounty.distribution;

import com.raindropcentral.rdq.bounty.dto.Bounty;
import com.raindropcentral.rdq.bounty.dto.RewardItem;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Distributes rewards by dropping them at the target's death location.
 * <p>
 * Items are dropped naturally at the death location, allowing any player
 * to pick them up. This mode is useful for creating a competitive environment
 * where multiple players can compete for the rewards.
 * Handles world boundaries and invalid locations gracefully.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 2.0.0
 */
public final class DropRewardDistributor implements RewardDistributor {
    
    private static final Logger LOGGER = Logger.getLogger(DropRewardDistributor.class.getName());
    
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
            // Validate location is safe for dropping items
            Location dropLocation = validateAndAdjustLocation(location);
            
            // Drop item rewards at the location
            dropItems(dropLocation, bounty.rewardItems());
            
            // Currency rewards cannot be dropped, credit to hunter instead
            creditCurrencies(hunter, bounty.rewardCurrencies());
            
            LOGGER.log(Level.INFO, "Dropped rewards at " + formatLocation(dropLocation) + 
                    " for bounty #" + bounty.id());
        });
    }
    
    /**
     * Validates and adjusts the drop location to ensure it's safe.
     * Handles world boundaries and invalid locations.
     *
     * @param location the original location
     * @return a safe location for dropping items
     */
    private @NotNull Location validateAndAdjustLocation(@NotNull Location location) {
        World world = location.getWorld();
        
        if (world == null) {
            throw new IllegalStateException("Location world is null, cannot drop items");
        }
        
        // Clone location to avoid modifying the original
        Location dropLocation = location.clone();
        
        // Ensure Y coordinate is within world bounds
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();
        
        if (dropLocation.getY() < minY) {
            dropLocation.setY(minY);
            LOGGER.log(Level.WARNING, "Adjusted drop location Y from below min height to " + minY);
        } else if (dropLocation.getY() > maxY) {
            dropLocation.setY(maxY - 1);
            LOGGER.log(Level.WARNING, "Adjusted drop location Y from above max height to " + (maxY - 1));
        }
        
        // Ensure the block at the location is loaded
        if (!world.isChunkLoaded(dropLocation.getBlockX() >> 4, dropLocation.getBlockZ() >> 4)) {
            world.loadChunk(dropLocation.getBlockX() >> 4, dropLocation.getBlockZ() >> 4);
        }
        
        return dropLocation;
    }
    
    /**
     * Drops item rewards at the specified location.
     *
     * @param location the location to drop items
     * @param rewardItems the items to drop
     */
    private void dropItems(@NotNull Location location, @NotNull java.util.Set<RewardItem> rewardItems) {
        World world = location.getWorld();
        
        if (world == null) {
            LOGGER.log(Level.SEVERE, "Cannot drop items: world is null");
            return;
        }
        
        for (RewardItem rewardItem : rewardItems) {
            ItemStack item = rewardItem.item().clone();
            item.setAmount(rewardItem.amount());
            
            try {
                world.dropItemNaturally(location, item);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to drop item at " + formatLocation(location), e);
            }
        }
    }
    
    /**
     * Credits currency rewards to the hunter's economy balance.
     * Currency cannot be dropped, so it's credited directly.
     *
     * @param hunter the hunter receiving currencies
     * @param rewardCurrencies the currencies to credit
     */
    private void creditCurrencies(
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
    
    /**
     * Formats a location for logging.
     *
     * @param location the location to format
     * @return a formatted string representation
     */
    private @NotNull String formatLocation(@NotNull Location location) {
        return String.format("(%d, %d, %d) in %s",
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                location.getWorld() != null ? location.getWorld().getName() : "unknown"
        );
    }
}
