package de.jexcellence.jexplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.jexplatform.reward.AbstractReward;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Opens a temporary chest inventory for the player that vanishes after a timer.
 *
 * <p>When the timer expires or the player closes the inventory, any remaining
 * items are optionally dropped at the player's location.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class VanishingChestReward extends AbstractReward {

    @JsonProperty("title") private final String title;
    @JsonProperty("items") private final List<String> items;
    @JsonProperty("dropOnVanish") private final boolean dropOnVanish;
    @JsonProperty("vanishDelayTicks") private final int vanishDelayTicks;

    /**
     * Creates a vanishing chest reward.
     *
     * @param title            the inventory title
     * @param items            the material names to fill the chest with
     * @param dropOnVanish     whether to drop remaining items when the chest vanishes
     * @param vanishDelayTicks the delay in ticks before the chest vanishes
     */
    public VanishingChestReward(@JsonProperty("title") String title,
                                @JsonProperty("items") List<String> items,
                                @JsonProperty("dropOnVanish") boolean dropOnVanish,
                                @JsonProperty("vanishDelayTicks") int vanishDelayTicks) {
        super("VANISHING_CHEST");
        this.title = title != null ? title : "Reward Chest";
        this.items = items != null ? List.copyOf(items) : List.of();
        this.dropOnVanish = dropOnVanish;
        this.vanishDelayTicks = vanishDelayTicks > 0 ? vanishDelayTicks : 600;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        var inventory = Bukkit.createInventory(null, 27,
                net.kyori.adventure.text.Component.text(title));

        for (var materialName : items) {
            var mat = Material.matchMaterial(materialName);
            if (mat != null) {
                inventory.addItem(new ItemStack(mat));
            }
        }

        player.openInventory(inventory);
        scheduleVanish(player, inventory);
        return CompletableFuture.completedFuture(true);
    }

    private void scheduleVanish(@NotNull Player player, @NotNull Inventory inventory) {
        Bukkit.getScheduler().runTaskLater(
                Bukkit.getPluginManager().getPlugins()[0],
                () -> {
                    if (player.isOnline()
                            && player.getOpenInventory().getTopInventory().equals(inventory)) {
                        if (dropOnVanish) {
                            for (var item : inventory.getContents()) {
                                if (item != null && item.getType() != Material.AIR) {
                                    player.getWorld().dropItemNaturally(
                                            player.getLocation(), item);
                                }
                            }
                        }
                        player.closeInventory();
                    }
                },
                vanishDelayTicks);
    }

    @Override
    public @NotNull String descriptionKey() {
        return "reward.vanishing_chest";
    }
}
