package com.raindropcentral.rdt.items;

import com.raindropcentral.rdt.RDT;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Nexus {

    public static @NonNull ItemStack getNexusItem(RDT plugin, @NonNull UUID town_uuid, String town_name) {
        ItemStack nexus = new ItemStack(Material.REINFORCED_DEEPSLATE);
        ItemMeta meta = nexus.getItemMeta();
        meta.displayName(Component.text("Nexus Stone", NamedTextColor.YELLOW));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Place to define the Nexus chunk", NamedTextColor.YELLOW));
        meta.lore(lore);
        PersistentDataContainer persistentDataContainer = meta.getPersistentDataContainer();
        persistentDataContainer.set(
                new NamespacedKey(plugin.getPlugin(), "town_uuid"),
                PersistentDataType.STRING,
                town_uuid.toString()
        );
        persistentDataContainer.set(
                new NamespacedKey(plugin.getPlugin(), "town_name"),
                PersistentDataType.STRING,
                town_name
        );
        nexus.setItemMeta(meta);
        return nexus;
    }

    public static boolean equals(RDT plugin, @NonNull ItemStack item){
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer persistentDataContainer = meta.getPersistentDataContainer();
        return persistentDataContainer.has(
                new NamespacedKey(plugin.getPlugin(), "town_uuid"),
                PersistentDataType.STRING)
                &&
                persistentDataContainer.has(
                        new NamespacedKey(plugin.getPlugin(), "town_name"),
                        PersistentDataType.STRING
                );
    }

    public static @Nullable UUID getTownUUID(RDT plugin, @NonNull ItemStack item){
        ItemMeta meta = item.getItemMeta();
        String s = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin.getPlugin(), "town_uuid"),
                PersistentDataType.STRING
        );
        if (s == null) return null;
        return UUID.fromString(s);
    }

    public static String getTownName(RDT plugin, @NonNull ItemStack item){
        return item.getItemMeta().getPersistentDataContainer().get(
                new NamespacedKey(plugin.getPlugin(), "town_name"),
                PersistentDataType.STRING
        );
    }
}
