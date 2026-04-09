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
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Bound Armory salvage-block item helper.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class SalvageBlock {

    private static final String ITEM_KIND = "salvage_block";
    private static final String ITEM_TYPE_KEY = "rdt_item_type";
    private static final String TOWN_UUID_KEY = "rdt_town_uuid";
    private static final String WORLD_KEY = "rdt_claim_world";
    private static final String CHUNK_X_KEY = "rdt_claim_chunk_x";
    private static final String CHUNK_Z_KEY = "rdt_claim_chunk_z";

    private SalvageBlock() {
    }

    /**
     * Creates a bound Armory salvage-block item.
     *
     * @param plugin active plugin runtime
     * @param player receiving player
     * @param townUuid owning town UUID
     * @param worldName owning world name
     * @param chunkX owning chunk X coordinate
     * @param chunkZ owning chunk Z coordinate
     * @return bound salvage-block item
     */
    public static @NotNull ItemStack getSalvageBlockItem(
        final @NotNull RDT plugin,
        final @NotNull Player player,
        final @NotNull UUID townUuid,
        final @NotNull String worldName,
        final int chunkX,
        final int chunkZ
    ) {
        final ItemStack salvageBlock = new ItemStack(plugin.getArmoryConfig().getSalvageBlock().blockMaterial());
        final ItemMeta meta = Objects.requireNonNull(salvageBlock.getItemMeta(), "salvage block meta");
        meta.displayName(new I18n.Builder("salvage_block.name", player)
            .withPlaceholders(Map.of(
                "chunk_x", chunkX,
                "chunk_z", chunkZ
            ))
            .build()
            .component());
        meta.lore(new I18n.Builder("salvage_block.lore", player)
            .withPlaceholders(Map.of(
                "world", worldName,
                "chunk_x", chunkX,
                "chunk_z", chunkZ
            ))
            .build()
            .children());
        meta.getPersistentDataContainer().set(key(plugin, ITEM_TYPE_KEY), PersistentDataType.STRING, ITEM_KIND);
        meta.getPersistentDataContainer().set(key(plugin, TOWN_UUID_KEY), PersistentDataType.STRING, townUuid.toString());
        meta.getPersistentDataContainer().set(key(plugin, WORLD_KEY), PersistentDataType.STRING, worldName);
        meta.getPersistentDataContainer().set(key(plugin, CHUNK_X_KEY), PersistentDataType.INTEGER, chunkX);
        meta.getPersistentDataContainer().set(key(plugin, CHUNK_Z_KEY), PersistentDataType.INTEGER, chunkZ);
        salvageBlock.setItemMeta(meta);
        return salvageBlock;
    }

    /**
     * Returns whether an item is a bound salvage-block payload.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return {@code true} when the item is a bound salvage block
     */
    public static boolean equals(final @NotNull RDT plugin, final @Nullable ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return false;
        }
        return ITEM_KIND.equalsIgnoreCase(item.getItemMeta().getPersistentDataContainer().get(
            key(plugin, ITEM_TYPE_KEY),
            PersistentDataType.STRING
        ));
    }

    /**
     * Returns the stored owning town UUID from a bound item.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return stored town UUID, or {@code null} when unavailable
     */
    public static @Nullable UUID getTownUUID(final @NotNull RDT plugin, final @Nullable ItemStack item) {
        final String rawUuid = readString(plugin, item, TOWN_UUID_KEY);
        if (rawUuid == null || rawUuid.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(rawUuid);
        } catch (final IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Returns the stored owning world name from a bound item.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return stored world name, or {@code null} when unavailable
     */
    public static @Nullable String getWorldName(final @NotNull RDT plugin, final @Nullable ItemStack item) {
        return readString(plugin, item, WORLD_KEY);
    }

    /**
     * Returns the stored owning chunk X coordinate from a bound item.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return stored chunk X coordinate, or {@code null} when unavailable
     */
    public static @Nullable Integer getChunkX(final @NotNull RDT plugin, final @Nullable ItemStack item) {
        return readInteger(plugin, item, CHUNK_X_KEY);
    }

    /**
     * Returns the stored owning chunk Z coordinate from a bound item.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return stored chunk Z coordinate, or {@code null} when unavailable
     */
    public static @Nullable Integer getChunkZ(final @NotNull RDT plugin, final @Nullable ItemStack item) {
        return readInteger(plugin, item, CHUNK_Z_KEY);
    }

    private static @Nullable String readString(
        final @NotNull RDT plugin,
        final @Nullable ItemStack item,
        final @NotNull String key
    ) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(
            key(plugin, key),
            PersistentDataType.STRING
        );
    }

    private static @Nullable Integer readInteger(
        final @NotNull RDT plugin,
        final @Nullable ItemStack item,
        final @NotNull String key
    ) {
        if (item == null || item.getItemMeta() == null) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(
            key(plugin, key),
            PersistentDataType.INTEGER
        );
    }

    private static @NotNull NamespacedKey key(final @NotNull RDT plugin, final @NotNull String key) {
        return new NamespacedKey(plugin.getPlugin(), key);
    }
}
