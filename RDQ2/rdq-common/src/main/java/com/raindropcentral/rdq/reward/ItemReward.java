package com.raindropcentral.rdq.reward;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class ItemReward extends AbstractReward {

    private final ItemStack item;
    private final double estimatedValue;

    public ItemReward(@NotNull ItemStack item, double estimatedValue) {
        super(Type.ITEM, "reward.item");
        this.item = item.clone();
        this.estimatedValue = estimatedValue;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            var leftover = player.getInventory().addItem(item);
            if (!leftover.isEmpty()) {
                leftover.values().forEach(i -> player.getWorld().dropItem(player.getLocation(), i));
            }
            return true;
        });
    }

    @Override
    public double getEstimatedValue() { return estimatedValue; }

    public ItemStack getItem() { return item.clone(); }
}
