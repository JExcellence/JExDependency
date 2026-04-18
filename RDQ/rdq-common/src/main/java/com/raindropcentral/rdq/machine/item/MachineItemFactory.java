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

package com.raindropcentral.rdq.machine.item;

import com.raindropcentral.rdq.machine.type.EMachineType;
import de.jexcellence.jextranslate.i18n.I18n;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Factory for creating machine items with custom NBT data.
 *
 * <p>This factory creates ItemStacks that represent machine items, which can be
 * placed by players to construct machines. Each machine item contains:
 * <ul>
 *     <li>Machine type identifier stored in NBT</li>
 *     <li>Translated display name based on player locale</li>
 *     <li>Translated lore with machine description</li>
 *     <li>Custom model data for resource pack support (future)</li>
 * </ul>
 *
 * <p>Machine items use the PersistentDataContainer API to store machine type
 * information, ensuring compatibility across server restarts and item transfers.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 1.0.0
 */
public class MachineItemFactory {

    private static final String NBT_KEY_MACHINE_TYPE = "machine_type";
    private static final String NBT_KEY_MACHINE_ITEM = "machine_item";

    private final JavaPlugin plugin;
    private final NamespacedKey machineTypeKey;
    private final NamespacedKey machineItemKey;

    /**
     * Creates a new machine item factory.
     *
     * @param plugin the plugin instance for creating namespaced keys
     */
    public MachineItemFactory(final @NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.machineTypeKey = new NamespacedKey(plugin, NBT_KEY_MACHINE_TYPE);
        this.machineItemKey = new NamespacedKey(plugin, NBT_KEY_MACHINE_ITEM);
    }

    /**
     * Creates a machine item for the specified machine type.
     *
     * <p>The created item will have:
     * <ul>
     *     <li>Material matching the machine's core block</li>
     *     <li>Display name from translation key "machine.item.{type}.name"</li>
     *     <li>Lore from translation key "machine.item.{type}.lore"</li>
     *     <li>NBT data identifying the machine type</li>
     * </ul>
     *
     * @param machineType the type of machine to create an item for
     * @param player      the player who will receive the item (for locale)
     * @return the created machine item
     */
    public @NotNull ItemStack createMachineItem(
        final @NotNull EMachineType machineType,
        final @NotNull Player player
    ) {
        final ItemStack item = new ItemStack(machineType.getCoreMaterial());
        final ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        // Set display name
        final Component displayName = new I18n.Builder(
            "machine.item." + machineType.getIdentifier() + ".name",
            player
        ).build().component();
        meta.displayName(displayName);

        // Set lore
        final List<Component> lore = new I18n.Builder(
            "machine.item." + machineType.getIdentifier() + ".lore",
            player
        ).build().children();
        meta.lore(lore);

        // Store machine type in NBT
        final PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(machineTypeKey, PersistentDataType.STRING, machineType.name());
        container.set(machineItemKey, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Checks if an item is a machine item.
     *
     * @param item the item to check
     * @return true if the item is a machine item, false otherwise
     */
    public boolean isMachineItem(final @Nullable ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        final ItemMeta meta = item.getItemMeta();
        final PersistentDataContainer container = meta.getPersistentDataContainer();

        return container.has(machineItemKey, PersistentDataType.BYTE);
    }

    /**
     * Gets the machine type from a machine item.
     *
     * @param item the machine item
     * @return the machine type, or null if the item is not a machine item
     */
    public @Nullable EMachineType getMachineType(final @Nullable ItemStack item) {
        if (!isMachineItem(item)) {
            return null;
        }

        final ItemMeta meta = item.getItemMeta();
        final PersistentDataContainer container = meta.getPersistentDataContainer();

        final String typeString = container.get(machineTypeKey, PersistentDataType.STRING);
        if (typeString == null) {
            return null;
        }

        try {
            return EMachineType.valueOf(typeString);
        } catch (final IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid machine type in item NBT: " + typeString);
            return null;
        }
    }

    /**
     * Validates that a machine item matches the expected machine type.
     *
     * @param item        the item to validate
     * @param machineType the expected machine type
     * @return true if the item is a valid machine item of the specified type
     */
    public boolean validateMachineItem(
        final @Nullable ItemStack item,
        final @NotNull EMachineType machineType
    ) {
        final EMachineType itemType = getMachineType(item);
        return itemType != null && itemType == machineType;
    }
}
