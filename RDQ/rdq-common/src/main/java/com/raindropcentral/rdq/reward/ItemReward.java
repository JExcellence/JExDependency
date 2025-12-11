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
        this.item = item.clone();
        this.amount = item.getAmount();
        this.item.setAmount(1);
        this.estimatedValue = estimatedValue;
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

    public ItemStack getItem() { 
        ItemStack result = item.clone(); 
        result.setAmount(amount != null ? amount : 1);
        return result;
    }
}
