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
import com.raindropcentral.rdt.utils.ChunkType;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Bound chunk-claim item payload used to claim one specific target chunk.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class ChunkBlock {

    private static final String ITEM_KIND = "chunk_block";
    private static final String ITEM_TYPE_KEY = "rdt_item_type";
    private static final String TOWN_UUID_KEY = "rdt_town_uuid";
    private static final String MAYOR_UUID_KEY = "rdt_mayor_uuid";
    private static final String WORLD_KEY = "rdt_claim_world";
    private static final String CHUNK_X_KEY = "rdt_claim_chunk_x";
    private static final String CHUNK_Z_KEY = "rdt_claim_chunk_z";
    private static final String CHUNK_TYPE_KEY = "rdt_chunk_type";

    private ChunkBlock() {
    }

    /**
     * Creates a bound chunk-claim item.
     *
     * @param plugin active plugin runtime
     * @param player receiving player
     * @param townUuid owning town UUID
     * @param mayorUuid mayor UUID
     * @param worldName required target world
     * @param chunkX required target chunk X
     * @param chunkZ required target chunk Z
     * @return bound chunk claim item
     */
    public static @NotNull ItemStack getChunkBlockItem(
        final @NotNull RDT plugin,
        final @NotNull Player player,
        final @NotNull UUID townUuid,
        final @NotNull UUID mayorUuid,
        final @NotNull String worldName,
        final int chunkX,
        final int chunkZ
    ) {
        return getChunkBlockItem(plugin, player, townUuid, mayorUuid, worldName, chunkX, chunkZ, ChunkType.DEFAULT);
    }

    /**
     * Creates a bound chunk-claim item.
     *
     * @param plugin active plugin runtime
     * @param player receiving player
     * @param townUuid owning town UUID
     * @param mayorUuid mayor UUID
     * @param worldName required target world
     * @param chunkX required target chunk X
     * @param chunkZ required target chunk Z
     * @param initialChunkType initial chunk type that will be created on placement
     * @return bound chunk claim item
     */
    public static @NotNull ItemStack getChunkBlockItem(
        final @NotNull RDT plugin,
        final @NotNull Player player,
        final @NotNull UUID townUuid,
        final @NotNull UUID mayorUuid,
        final @NotNull String worldName,
        final int chunkX,
        final int chunkZ,
        final @Nullable ChunkType initialChunkType
    ) {
        final ChunkType resolvedChunkType = initialChunkType == ChunkType.FOB ? ChunkType.FOB : ChunkType.DEFAULT;
        final ItemStack chunkBlock = new ItemStack(plugin.getChunkTypeDisplayMaterial(resolvedChunkType));
        final ItemMeta meta = Objects.requireNonNull(chunkBlock.getItemMeta(), "chunk block meta");
        meta.displayName(new I18n.Builder("chunk_block.name", player)
            .withPlaceholders(Map.of(
                "chunk_x", chunkX,
                "chunk_z", chunkZ
            ))
            .build()
            .component());
        meta.lore(new I18n.Builder("chunk_block.lore", player)
            .withPlaceholders(Map.of(
                "world", worldName,
                "chunk_x", chunkX,
                "chunk_z", chunkZ
            ))
            .build()
            .children());
        final PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(key(plugin, ITEM_TYPE_KEY), PersistentDataType.STRING, ITEM_KIND);
        data.set(key(plugin, TOWN_UUID_KEY), PersistentDataType.STRING, townUuid.toString());
        data.set(key(plugin, MAYOR_UUID_KEY), PersistentDataType.STRING, mayorUuid.toString());
        data.set(key(plugin, WORLD_KEY), PersistentDataType.STRING, worldName);
        data.set(key(plugin, CHUNK_X_KEY), PersistentDataType.INTEGER, chunkX);
        data.set(key(plugin, CHUNK_Z_KEY), PersistentDataType.INTEGER, chunkZ);
        data.set(key(plugin, CHUNK_TYPE_KEY), PersistentDataType.STRING, resolvedChunkType.name());
        chunkBlock.setItemMeta(meta);
        return chunkBlock;
    }

    /**
     * Returns whether an item is a bound chunk-block payload.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return {@code true} when the item is a bound chunk claim item
     */
    public static boolean equals(final @NotNull RDT plugin, final @Nullable ItemStack item) {
        if (item == null || item.getItemMeta() == null) {
            return false;
        }
        final String itemType = item.getItemMeta().getPersistentDataContainer().get(
            key(plugin, ITEM_TYPE_KEY),
            PersistentDataType.STRING
        );
        return ITEM_KIND.equalsIgnoreCase(itemType);
    }

    /**
     * Returns the stored owning town UUID.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return stored town UUID, or {@code null} when unavailable
     */
    public static @Nullable UUID getTownUUID(final @NotNull RDT plugin, final @Nullable ItemStack item) {
        return parseUuid(readString(plugin, item, TOWN_UUID_KEY));
    }

    /**
     * Returns the stored mayor UUID.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return stored mayor UUID, or {@code null} when unavailable
     */
    public static @Nullable UUID getMayorUUID(final @NotNull RDT plugin, final @Nullable ItemStack item) {
        return parseUuid(readString(plugin, item, MAYOR_UUID_KEY));
    }

    /**
     * Returns the stored target world name.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return target world name, or {@code null} when unavailable
     */
    public static @Nullable String getWorldName(final @NotNull RDT plugin, final @Nullable ItemStack item) {
        return readString(plugin, item, WORLD_KEY);
    }

    /**
     * Returns the stored target chunk X coordinate.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return target chunk X coordinate, or {@code null} when unavailable
     */
    public static @Nullable Integer getXLoc(final @NotNull RDT plugin, final @Nullable ItemStack item) {
        return readInteger(plugin, item, CHUNK_X_KEY);
    }

    /**
     * Returns the stored target chunk Z coordinate.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return target chunk Z coordinate, or {@code null} when unavailable
     */
    public static @Nullable Integer getZLoc(final @NotNull RDT plugin, final @Nullable ItemStack item) {
        return readInteger(plugin, item, CHUNK_Z_KEY);
    }

    /**
     * Returns the stored initial chunk type.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return stored chunk type, or {@code null} when unavailable
     */
    public static @Nullable ChunkType getChunkType(final @NotNull RDT plugin, final @Nullable ItemStack item) {
        final String rawType = readString(plugin, item, CHUNK_TYPE_KEY);
        if (rawType == null || rawType.isBlank()) {
            return null;
        }
        try {
            return ChunkType.valueOf(rawType.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (final IllegalArgumentException ignored) {
            return null;
        }
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

    private static @Nullable UUID parseUuid(final @Nullable String rawUuid) {
        if (rawUuid == null || rawUuid.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(rawUuid);
        } catch (final IllegalArgumentException ignored) {
            return null;
        }
    }

    private static @NotNull NamespacedKey key(final @NotNull RDT plugin, final @NotNull String key) {
        return new NamespacedKey(plugin.getPlugin(), key);
    }
}
