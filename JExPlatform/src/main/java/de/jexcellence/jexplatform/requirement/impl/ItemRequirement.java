package de.jexcellence.jexplatform.requirement.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.jexplatform.requirement.AbstractRequirement;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Requires the player to possess a specific item in their inventory.
 *
 * <p>Supports exact item matching or material-type-only matching.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class ItemRequirement extends AbstractRequirement {

    @JsonProperty("material")
    private final String material;

    @JsonProperty("amount")
    private final int amount;

    @JsonProperty("exactMatch")
    private final boolean exactMatch;

    /**
     * Creates an item requirement.
     *
     * @param material         the material name
     * @param amount           the required amount
     * @param exactMatch       whether to check metadata (not just type)
     * @param consumeOnComplete whether to remove items on fulfillment
     */
    public ItemRequirement(@JsonProperty("material") @NotNull String material,
                           @JsonProperty("amount") int amount,
                           @JsonProperty("exactMatch") boolean exactMatch,
                           @JsonProperty("consumeOnComplete") boolean consumeOnComplete) {
        super("ITEM", consumeOnComplete);
        this.material = material;
        this.amount = Math.max(1, amount);
        this.exactMatch = exactMatch;
    }

    @Override
    public boolean isMet(@NotNull Player player) {
        return countItems(player) >= amount;
    }

    @Override
    public double calculateProgress(@NotNull Player player) {
        return Math.min(1.0, (double) countItems(player) / amount);
    }

    @Override
    public void consume(@NotNull Player player) {
        if (!shouldConsume()) {
            return;
        }

        var mat = Material.matchMaterial(material);
        if (mat == null) {
            return;
        }

        var remaining = amount;
        for (var item : player.getInventory().getContents()) {
            if (item == null || item.getType() != mat) {
                continue;
            }
            if (remaining <= 0) {
                break;
            }
            var take = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - take);
            remaining -= take;
        }
    }

    @Override
    public @NotNull String descriptionKey() {
        return "requirement.item";
    }

    private int countItems(@NotNull Player player) {
        var mat = Material.matchMaterial(material);
        if (mat == null) {
            return 0;
        }

        var total = 0;
        for (var item : player.getInventory().getContents()) {
            if (item != null && item.getType() == mat) {
                total += item.getAmount();
            }
        }
        return total;
    }
}
