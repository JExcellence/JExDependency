package com.raindropcentral.rdq.bounty.distribution;

import com.raindropcentral.rdq.bounty.dto.Bounty;
import com.raindropcentral.rdq.bounty.dto.RewardItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Distributes rewards by placing them in a chest at the death location.
 * <p>
 * A chest is placed at the target's death location and filled with reward items.
 * If chest placement fails (e.g., block is occupied), items are dropped instead.
 * Currency rewards are credited to the hunter's economy balance.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 2.0.0
 */
public final class ChestRewardDistributor implements RewardDistributor {
    
    private static final Logger LOGGER = Logger.getLogger(ChestRewardDistributor.class.getName());
    
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
            // Validate location is safe
            Location chestLocation = validateAndAdjustLocation(location);
            
            // Try to place chest and fill with rewards
            boolean chestPlaced = placeAndFillChest(chestLocation, bounty.rewardItems());
            
            // If chest placement failed, drop items instead
            if (!chestPlaced) {
                LOGGER.log(Level.WARNING, "Failed to place chest at " + 
                        formatLocation(chestLocation) + ", dropping items instead");
                dropItemsAsFallback(chestLocation, bounty.rewardItems());
            }
            
            // Credit currency rewards to hunter
            creditCurrencies(hunter, bounty.rewardCurrencies());
            
            LOGGER.log(Level.INFO, "Distributed rewards via chest at " + 
                    formatLocation(chestLocation) + " for bounty #" + bounty.id());
        });
    }
    
    /**
     * Validates and adjusts the chest location to ensure it's safe.
     *
     * @param location the original location
     * @return a safe location for placing the chest
     */
    private @NotNull Location validateAndAdjustLocation(@NotNull Location location) {
        World world = location.getWorld();
        
        if (world == null) {
            throw new IllegalStateException("Location world is null, cannot place chest");
        }
        
        // Clone location to avoid modifying the original
        Location chestLocation = location.clone();
        
        // Ensure Y coordinate is within world bounds
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();
        
        if (chestLocation.getY() < minY) {
            chestLocation.setY(minY);
        } else if (chestLocation.getY() > maxY - 1) {
            chestLocation.setY(maxY - 1);
        }
        
        // Round to block coordinates
        chestLocation.setX(chestLocation.getBlockX());
        chestLocation.setY(chestLocation.getBlockY());
        chestLocation.setZ(chestLocation.getBlockZ());
        
        // Ensure the chunk is loaded
        if (!world.isChunkLoaded(chestLocation.getBlockX() >> 4, chestLocation.getBlockZ() >> 4)) {
            world.loadChunk(chestLocation.getBlockX() >> 4, chestLocation.getBlockZ() >> 4);
        }
        
        return chestLocation;
    }
    
    /**
     * Places a chest at the location and fills it with reward items.
     *
     * @param location the location to place the chest
     * @param rewardItems the items to place in the chest
     * @return true if chest was successfully placed and filled, false otherwise
     */
    private boolean placeAndFillChest(
            @NotNull Location location,
            @NotNull java.util.Set<RewardItem> rewardItems
    ) {
        World world = location.getWorld();
        
        if (world == null) {
            return false;
        }
        
        Block block = world.getBlockAt(location);
        
        // Check if block can be replaced
        if (!canPlaceChest(block)) {
            // Try to find a nearby suitable location
            Location nearbyLocation = findNearbyLocation(location);
            if (nearbyLocation != null) {
                block = world.getBlockAt(nearbyLocation);
                location = nearbyLocation;
            } else {
                return false; // No suitable location found
            }
        }
        
        try {
            // Place the chest
            block.setType(Material.CHEST);
            BlockState state = block.getState();
            
            if (!(state instanceof Chest chest)) {
                LOGGER.log(Level.SEVERE, "Block state is not a chest after placement");
                return false;
            }
            
            // Fill the chest with reward items
            Inventory chestInventory = chest.getInventory();
            List<ItemStack> excessItems = new ArrayList<>();
            
            for (RewardItem rewardItem : rewardItems) {
                ItemStack item = rewardItem.item().clone();
                item.setAmount(rewardItem.amount());
                
                // Try to add item to chest
                Map<Integer, ItemStack> leftover = chestInventory.addItem(item);
                
                // If there are leftover items, collect them for dropping
                if (!leftover.isEmpty()) {
                    excessItems.addAll(leftover.values());
                }
            }
            
            // Update the chest
            chest.update();
            
            // Drop excess items if chest was full
            if (!excessItems.isEmpty()) {
                for (ItemStack excessItem : excessItems) {
                    world.dropItemNaturally(location, excessItem);
                }
                LOGGER.log(Level.INFO, "Dropped " + excessItems.size() + 
                        " excess items (chest full)");
            }
            
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to place and fill chest", e);
            return false;
        }
    }
    
    /**
     * Checks if a chest can be placed at the block location.
     *
     * @param block the block to check
     * @return true if a chest can be placed, false otherwise
     */
    private boolean canPlaceChest(@NotNull Block block) {
        Material type = block.getType();
        
        // Can replace air, grass, flowers, etc.
        return type.isAir() || 
               !type.isSolid() || 
               type == Material.SHORT_GRASS || 
               type == Material.TALL_GRASS ||
               type == Material.FERN ||
               type == Material.LARGE_FERN ||
               type == Material.DEAD_BUSH ||
               type == Material.DANDELION ||
               type == Material.POPPY;
    }
    
    /**
     * Finds a nearby suitable location for placing a chest.
     * Searches in a 3x3x3 area around the original location.
     *
     * @param location the original location
     * @return a suitable nearby location, or null if none found
     */
    private Location findNearbyLocation(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) {
            return null;
        }
        
        // Search in a 3x3x3 area
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue; // Skip the original location
                    }
                    
                    Location nearby = location.clone().add(dx, dy, dz);
                    Block block = world.getBlockAt(nearby);
                    
                    if (canPlaceChest(block)) {
                        return nearby;
                    }
                }
            }
        }
        
        return null; // No suitable location found
    }
    
    /**
     * Drops items as a fallback when chest placement fails.
     *
     * @param location the location to drop items
     * @param rewardItems the items to drop
     */
    private void dropItemsAsFallback(
            @NotNull Location location,
            @NotNull java.util.Set<RewardItem> rewardItems
    ) {
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
