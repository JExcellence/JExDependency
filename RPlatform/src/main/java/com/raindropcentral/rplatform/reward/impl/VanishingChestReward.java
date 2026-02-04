package com.raindropcentral.rplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Reward that spawns a temporary chest with items at the player's location.
 * The chest vanishes after a specified duration.
 */
public final class VanishingChestReward extends AbstractReward {

    private final List<ItemStack> items;
    private final long durationTicks;
    private final boolean dropItemsOnVanish;

    @JsonCreator
    public VanishingChestReward(
        @JsonProperty("items") @NotNull List<ItemStack> items,
        @JsonProperty("durationTicks") long durationTicks,
        @JsonProperty("dropItemsOnVanish") boolean dropItemsOnVanish
    ) {
        this.items = items.stream().map(ItemStack::clone).toList();
        this.durationTicks = Math.max(20, durationTicks); // Minimum 1 second
        this.dropItemsOnVanish = dropItemsOnVanish;
    }

    @Override
    public @NotNull String getTypeId() {
        return "VANISHING_CHEST";
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Location location = findSafeChestLocation(player.getLocation());
                if (location == null) {
                    return false;
                }

                // Place chest on main thread
                Bukkit.getScheduler().runTask(
                    Bukkit.getPluginManager().getPlugins()[0],
                    () -> placeChest(location, player)
                );

                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }

    private void placeChest(@NotNull Location location, @NotNull Player player) {
        Block block = location.getBlock();
        Material originalType = block.getType();
        
        block.setType(Material.CHEST);

        if (block.getState() instanceof Chest chest) {
            Inventory inventory = chest.getInventory();
            
            // Add items to chest
            for (ItemStack item : items) {
                inventory.addItem(item.clone());
            }
            
            chest.update();

            // Schedule chest removal
            Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugins()[0],
                () -> removeChest(block, originalType, player),
                durationTicks
            );
        }
    }

    private void removeChest(@NotNull Block block, @NotNull Material originalType, @NotNull Player player) {
        if (block.getType() != Material.CHEST) {
            return;
        }

        if (block.getState() instanceof Chest chest) {
            Inventory inventory = chest.getInventory();
            
            // Drop remaining items if configured
            if (dropItemsOnVanish) {
                for (ItemStack item : inventory.getContents()) {
                    if (item != null && !item.getType().isAir()) {
                        block.getWorld().dropItemNaturally(block.getLocation(), item);
                    }
                }
            }
        }

        // Restore original block
        block.setType(originalType);
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

    @Override
    public double getEstimatedValue() {
        return items.stream()
            .mapToDouble(item -> item.getAmount() * 1.0)
            .sum();
    }

    public List<ItemStack> getItems() {
        return items.stream().map(ItemStack::clone).toList();
    }

    public long getDurationTicks() {
        return durationTicks;
    }

    public boolean isDropItemsOnVanish() {
        return dropItemsOnVanish;
    }

    @Override
    public void validate() {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Vanishing chest must have at least one item");
        }
        
        for (ItemStack item : items) {
            if (item == null || item.getType().isAir()) {
                throw new IllegalArgumentException("Vanishing chest cannot contain null or air items");
            }
        }
    }
}
