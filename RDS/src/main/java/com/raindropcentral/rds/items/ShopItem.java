package com.raindropcentral.rds.items;

import com.raindropcentral.rds.RDS;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShopItem {

    public static @NonNull ItemStack getShopItem(RDS plugin, @NonNull UUID owner) {
        ItemStack shop = new ItemStack(Material.CHEST);
        ItemMeta meta = shop.getItemMeta();
        meta.displayName(Component.text("RaindropShop", NamedTextColor.BLUE));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Place to to set up shop", NamedTextColor.YELLOW));
        meta.lore(lore);
        PersistentDataContainer persistentDataContainer = meta.getPersistentDataContainer();
        persistentDataContainer.set(
                new NamespacedKey(plugin, "owner"),
                PersistentDataType.STRING,
                owner.toString()
        );
        shop.setItemMeta(meta);
        return shop;
    }

    public static boolean equals(RDS plugin, @NonNull ItemStack item){
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer persistentDataContainer = meta.getPersistentDataContainer();
        return persistentDataContainer.has(
                new NamespacedKey(plugin, "owner"),
                PersistentDataType.STRING)
                &&
                persistentDataContainer.has(
                        new NamespacedKey(plugin, "owner"),
                        PersistentDataType.STRING
                );
    }

    public static UUID getOwner(RDS plugin, ItemStack item) {
        if (item == null) return null;

        ItemMeta meta = item.getItemMeta();
        String s = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "owner"),
                PersistentDataType.STRING
        );
        if (s == null) return null;
        return UUID.fromString(s);
    }
}
