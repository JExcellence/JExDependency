package com.raindropcentral.rds.items;

import com.raindropcentral.rds.RDS;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/**
 * Represents shop block.
 */
public class ShopBlock {

    /**
     * Returns the shop block.
     *
     * @param plugin plugin instance
     * @param player target player
     * @return the shop block
     */
    public static @NonNull ItemStack getShopBlock(RDS plugin, @NonNull Player player) {
        ItemStack shop = new ItemStack(Material.CHEST);
        ItemMeta meta = shop.getItemMeta();
        meta.displayName(new I18n.Builder("shop_block.name", player).build().component());
        meta.lore(new I18n.Builder("shop_block.lore", player).build().children());
        PersistentDataContainer persistentDataContainer = meta.getPersistentDataContainer();
        persistentDataContainer.set(
                new NamespacedKey(plugin.getPlugin(), "owner"),
                PersistentDataType.STRING,
                player.getUniqueId().toString()
        );
        shop.setItemMeta(meta);
        return shop;
    }

    /**
     * Executes equals.
     *
     * @param plugin plugin instance
     * @param item target item payload
     * @return {@code true} if the operation succeeds; otherwise {@code false}
     */
    public static boolean equals(RDS plugin, @NonNull ItemStack item){
        if (item == null || item.getType() != Material.CHEST) {
            return false;
        }

        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        final PersistentDataContainer persistentDataContainer = meta.getPersistentDataContainer();
        return persistentDataContainer.has(
                new NamespacedKey(plugin.getPlugin(), "owner"),
                PersistentDataType.STRING
        );
    }

    /**
     * Returns the owner.
     *
     * @param plugin plugin instance
     * @param item target item payload
     * @return the owner
     */
    public static UUID getOwner(RDS plugin, ItemStack item) {
        if (item == null) return null;

        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        final String s = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin.getPlugin(), "owner"),
                PersistentDataType.STRING
        );
        if (s == null) return null;
        return UUID.fromString(s);
    }
}
