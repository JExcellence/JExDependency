package com.raindropcentral.rplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Reward that grants items to a player.
 * <p>
 * This reward handles inventory management, including dropping items
 * on the ground if the player's inventory is full. It ensures all
 * operations run on the main thread.
 * </p>
 *
 * @author RaindropCentral
 * @version 2.0.0
 * @since TBD
 */
@JsonTypeName("ITEM")
public final class ItemReward extends AbstractReward {

    private static final Logger LOGGER = Logger.getLogger(ItemReward.class.getName());

    @JsonProperty("item")
    private final ItemStack item;

    @JsonProperty("amount")
    private final int amount;

    @JsonCreator
    public ItemReward(
        @JsonProperty("item") @NotNull ItemStack item,
        @JsonProperty("amount") int amount
    ) {
        this.item = item.clone();
        this.item.setAmount(1);
        this.amount = Math.max(1, amount);
    }
    
    /**
     * Convenience constructor that uses the ItemStack's amount.
     *
     * @param item the item to reward
     */
    public ItemReward(@NotNull ItemStack item) {
        this(item, item.getAmount());
    }

    @Override
    public @NotNull String getTypeId() {
        return "ITEM";
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();

        // Get any plugin for scheduling (use first available)
        final Plugin plugin = Bukkit.getPluginManager().getPlugins()[0];

        // Must run on main thread for inventory operations
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                int remaining = amount;
                int maxStack = item.getMaxStackSize();
                int droppedStacks = 0;

                while (remaining > 0) {
                    int stackAmount = Math.min(remaining, maxStack);
                    ItemStack stack = item.clone();
                    stack.setAmount(stackAmount);

                    // Try to add to inventory
                    HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(stack);

                    // Drop items that don't fit
                    if (!leftover.isEmpty()) {
                        for (ItemStack drop : leftover.values()) {
                            player.getWorld().dropItem(player.getLocation(), drop);
                            droppedStacks++;
                        }
                    }

                    remaining -= stackAmount;
                }

                if (droppedStacks > 0) {
                    LOGGER.fine("Granted " + amount + " " + item.getType() + " to " + player.getName() +
                               " (" + droppedStacks + " stacks dropped on ground)");
                } else {
                    LOGGER.fine("Granted " + amount + " " + item.getType() + " to " + player.getName());
                }

                future.complete(true);
            } catch (Exception e) {
                LOGGER.warning("Failed to grant item reward to " + player.getName() + ": " + e.getMessage());
                future.complete(false);
            }
        });

        return future;
    }

    @Override
    public double getEstimatedValue() {
        return amount * 1.0;
    }

    /**
     * Gets the item template (always amount 1).
     *
     * @return the item template
     */
    public ItemStack getItem() {
        return item.clone();
    }

    /**
     * Gets the actual amount (can exceed max stack size).
     *
     * @return the amount
     */
    public int getAmount() {
        return amount;
    }

    @Override
    public void validate() {
        if (item == null || item.getType().isAir()) {
            throw new IllegalArgumentException("Item reward must have a valid item");
        }
        if (amount < 1) {
            throw new IllegalArgumentException("Item amount must be at least 1");
        }
    }
}
