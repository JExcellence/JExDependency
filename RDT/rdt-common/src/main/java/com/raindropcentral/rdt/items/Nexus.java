/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

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

/**
 * Represents the type API type.
 */
/**
 * Represents the Nexus API type.
 */
public class Nexus {

    private static final String TOWN_UUID_KEY = "town_uuid";
    /**
     * Executes this member.
     */
    private static final String TOWN_NAME_KEY = "town_name";
    private static final String MAYOR_UUID_KEY = "mayor_uuid";

    /**
     * Gets nexusItem.
     */
    public static @NonNull ItemStack getNexusItem(
            final RDT plugin,
            final @NonNull UUID town_uuid,
            final String town_name,
            final @NonNull UUID mayor_uuid
    ) {
        ItemStack nexus = new ItemStack(Material.REINFORCED_DEEPSLATE);
        ItemMeta meta = nexus.getItemMeta();
        meta.displayName(Component.text("Nexus Stone", NamedTextColor.YELLOW));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Place to define the Nexus chunk", NamedTextColor.YELLOW));
        meta.lore(lore);
        PersistentDataContainer persistentDataContainer = meta.getPersistentDataContainer();
        persistentDataContainer.set(
                new NamespacedKey(plugin.getPlugin(), TOWN_UUID_KEY),
                PersistentDataType.STRING,
                town_uuid.toString()
        );
        persistentDataContainer.set(
                new NamespacedKey(plugin.getPlugin(), TOWN_NAME_KEY),
                /**
                 * Executes method.
                 */
                PersistentDataType.STRING,
                town_name
        );
        persistentDataContainer.set(
                new NamespacedKey(plugin.getPlugin(), MAYOR_UUID_KEY),
                PersistentDataType.STRING,
                /**
                 * Executes toString.
                 */
                mayor_uuid.toString()
        );
        nexus.setItemMeta(meta);
        return nexus;
    }

    /**
     * Executes equals.
     */
    public static boolean equals(final RDT plugin, final @NonNull ItemStack item){
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            /**
             * Executes method.
             */
            return false;
        }
        PersistentDataContainer persistentDataContainer = meta.getPersistentDataContainer();
        return persistentDataContainer.has(
                new NamespacedKey(plugin.getPlugin(), TOWN_UUID_KEY),
                PersistentDataType.STRING)
                &&
                persistentDataContainer.has(
                        new NamespacedKey(plugin.getPlugin(), TOWN_NAME_KEY),
                        /**
                         * Executes this member.
                         */
                        PersistentDataType.STRING
                )
                &&
                persistentDataContainer.has(
                        new NamespacedKey(plugin.getPlugin(), MAYOR_UUID_KEY),
                        /**
                         * Executes method.
                         */
                        PersistentDataType.STRING
                );
    }

    /**
     * Gets townUUID.
     */
    public static @Nullable UUID getTownUUID(final RDT plugin, final @NonNull ItemStack item){
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        /**
         * Executes method.
         */
        }
        /**
         * Gets persistentDataContainer.
         */
        String s = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin.getPlugin(), TOWN_UUID_KEY),
                PersistentDataType.STRING
        );
        if (s == null) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
/**
 * Executes this member.
 */

    /**
     * Gets townName.
     */
    public static @Nullable String getTownName(final RDT plugin, final @NonNull ItemStack item){
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin.getPlugin(), TOWN_NAME_KEY),
                PersistentDataType.STRING
        );
    }

    /**
     * Gets mayorUUID.
     */
    public static @Nullable UUID getMayorUUID(final RDT plugin, final @NonNull ItemStack item) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        final String encodedMayor = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin.getPlugin(), MAYOR_UUID_KEY),
                PersistentDataType.STRING
        );
        if (encodedMayor == null) {
            return null;
        }
        try {
            return UUID.fromString(encodedMayor);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
