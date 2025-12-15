package com.raindropcentral.rdq.reward;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class ItemReward extends AbstractReward {

    @JsonProperty("item")
    private final ItemStack item;
    
    @JsonProperty("estimatedValue")
    private final double estimatedValue;

    @Getter
    @Setter
    @JsonProperty("amount")
    private Integer amount;

    @JsonCreator
    public ItemReward(@JsonProperty("item") @NotNull ItemStack item, 
                      @JsonProperty("estimatedValue") double estimatedValue) {
        super(Type.ITEM, "reward.item");
        // Store the amount first, before cloning
        this.amount = item.getAmount();
        // Clone the item and ensure it has amount=1 for safe serialization
        this.item = item.clone();
        this.item.setAmount(1);
        this.estimatedValue = estimatedValue;
        
        // Validate that the stored item has amount=1 for serialization safety
        if (this.item.getAmount() != 1) {
            throw new IllegalStateException("ItemReward item field must have amount=1 for serialization, but was: " + this.item.getAmount());
        }
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            ItemStack itemToGive = getItem(); // Use getItem() to get the correct amount
            var leftover = player.getInventory().addItem(itemToGive);
            if (!leftover.isEmpty()) {
                leftover.values().forEach(i -> player.getWorld().dropItem(player.getLocation(), i));
            }
            return true;
        });
    }

    @Override
    public double getEstimatedValue() { return estimatedValue; }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public ItemStack getItem() { 
        ItemStack result = item.clone(); 
        result.setAmount(amount != null ? amount : 1);
        return result;
    }
}
