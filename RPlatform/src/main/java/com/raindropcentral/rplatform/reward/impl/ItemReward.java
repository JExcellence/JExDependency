package com.raindropcentral.rplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@JsonTypeName("ITEM")
public final class ItemReward extends AbstractReward {

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
     * Convenience constructor that uses the ItemStack's amount
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
        return CompletableFuture.supplyAsync(() -> {
            int remaining = amount;
            int maxStack = item.getMaxStackSize();
            
            while (remaining > 0) {
                int stackAmount = Math.min(remaining, maxStack);
                ItemStack stack = item.clone();
                stack.setAmount(stackAmount);
                
                if (player.getInventory().firstEmpty() == -1) {
                    player.getWorld().dropItem(player.getLocation(), stack);
                } else {
                    player.getInventory().addItem(stack);
                }
                
                remaining -= stackAmount;
            }
            return true;
        });
    }

    @Override
    public double getEstimatedValue() {
        return amount * 1.0;
    }

    /**
     * Gets the item template (always amount 1)
     */
    public ItemStack getItem() {
        return item.clone();
    }

    /**
     * Gets the actual amount (can exceed max stack size)
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
