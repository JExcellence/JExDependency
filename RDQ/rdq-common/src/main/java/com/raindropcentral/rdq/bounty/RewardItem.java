package com.raindropcentral.rdq.bounty;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Immutable record representing a reward item with its amount and estimated value.
 *
 * @author JExcellence
 * @since 6.0.0
 */
public record RewardItem(
    @NotNull ItemStack item,
    int amount,
    double estimatedValue
) {
    /**
     * Compact constructor with validation.
     */
    public RewardItem {
        Objects.requireNonNull(item, "item cannot be null");
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive, got: " + amount);
        }
    }

    /**
     * Creates a RewardItem from an ItemStack using its amount.
     */
    public static RewardItem fromItemStack(@NotNull ItemStack item, double valuePerItem) {
        return new RewardItem(item.clone(), item.getAmount(), item.getAmount() * valuePerItem);
    }
}
