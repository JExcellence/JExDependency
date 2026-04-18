package de.jexcellence.jexplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.jexplatform.reward.AbstractReward;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Grants items to the player's inventory, dropping overflow on the ground.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class ItemReward extends AbstractReward {

    @JsonProperty("material") private final String material;
    @JsonProperty("amount") private final int amount;

    public ItemReward(@JsonProperty("material") @NotNull String material,
                      @JsonProperty("amount") int amount) {
        super("ITEM");
        this.material = material;
        this.amount = Math.max(1, amount);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        var mat = Material.matchMaterial(material);
        if (mat == null) return CompletableFuture.completedFuture(false);

        var item = new ItemStack(mat, amount);
        var overflow = player.getInventory().addItem(item);
        overflow.values().forEach(leftover ->
                player.getWorld().dropItemNaturally(player.getLocation(), leftover));
        return CompletableFuture.completedFuture(true);
    }

    @Override public @NotNull String descriptionKey() { return "reward.item"; }
    @Override public double estimatedValue() { return amount; }
}
