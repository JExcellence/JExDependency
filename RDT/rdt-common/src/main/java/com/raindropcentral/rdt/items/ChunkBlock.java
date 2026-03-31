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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Factory and metadata utilities for town Chunk Block items.
 *
 * <p>Chunk Block items are tied to a single town and target chunk coordinate. Placing the item in
 * the intended chunk finalizes a pending claim started from the chunk-claim view.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.1
 */
public final class ChunkBlock {

    private static final String TOWN_UUID_KEY = "chunk_block_town_uuid";
    private static final String TOWN_NAME_KEY = "chunk_block_town_name";
    private static final String MAYOR_UUID_KEY = "chunk_block_mayor_uuid";
    private static final String CHUNK_X_KEY = "chunk_block_x";
    private static final String CHUNK_Z_KEY = "chunk_block_z";
    private static final String CHUNK_TYPE_KEY = "chunk_block_type";

    private static final Material DEFAULT_CHUNK_BLOCK_MATERIAL = Material.OAK_PLANKS;

    private ChunkBlock() {
    }

    /**
     * Creates a Chunk Block item for a specific target chunk.
     *
     * @param plugin runtime plugin
     * @param townUuid town identifier
     * @param townName town display name
     * @param mayorUuid mayor identifier
     * @param chunkX target chunk X
     * @param chunkZ target chunk Z
     * @return configured Chunk Block item
     */
    public static @NonNull ItemStack getChunkBlockItem(
            final @NotNull RDT plugin,
            final @NonNull UUID townUuid,
            final @NotNull String townName,
            final @NonNull UUID mayorUuid,
            final int chunkX,
            final int chunkZ
    ) {
        return getChunkBlockItem(
                plugin,
                townUuid,
                townName,
                mayorUuid,
                chunkX,
                chunkZ,
                ChunkType.CHUNK_BLOCK
        );
    }

    /**
     * Creates a Chunk Block item for a specific target chunk and chunk type.
     *
     * @param plugin runtime plugin
     * @param townUuid town identifier
     * @param townName town display name
     * @param mayorUuid mayor identifier
     * @param chunkX target chunk X
     * @param chunkZ target chunk Z
     * @param chunkType chunk type this item should preserve on placement
     * @return configured Chunk Block item
     */
    public static @NonNull ItemStack getChunkBlockItem(
            final @NotNull RDT plugin,
            final @NonNull UUID townUuid,
            final @NotNull String townName,
            final @NonNull UUID mayorUuid,
            final int chunkX,
            final int chunkZ,
            final @NotNull ChunkType chunkType
    ) {
        final Material displayMaterial = plugin.getDefaultConfig().getChunkTypeIconMaterial(chunkType);
        final ItemStack chunkBlock = new ItemStack(displayMaterial);
        final ItemMeta meta = chunkBlock.getItemMeta();
        meta.displayName(Component.text("Chunk Block • " + chunkType.name(), NamedTextColor.GOLD));

        final List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Place inside the claimed chunk", NamedTextColor.YELLOW));
        lore.add(Component.text("Target: (" + chunkX + ", " + chunkZ + ")", NamedTextColor.GRAY));
        lore.add(Component.text("Type: " + chunkType.name(), NamedTextColor.GRAY));
        meta.lore(lore);

        final PersistentDataContainer persistentDataContainer = meta.getPersistentDataContainer();
        persistentDataContainer.set(
                new NamespacedKey(plugin.getPlugin(), TOWN_UUID_KEY),
                PersistentDataType.STRING,
                townUuid.toString()
        );
        persistentDataContainer.set(
                new NamespacedKey(plugin.getPlugin(), TOWN_NAME_KEY),
                PersistentDataType.STRING,
                townName
        );
        persistentDataContainer.set(
                new NamespacedKey(plugin.getPlugin(), MAYOR_UUID_KEY),
                PersistentDataType.STRING,
                mayorUuid.toString()
        );
        persistentDataContainer.set(
                new NamespacedKey(plugin.getPlugin(), CHUNK_X_KEY),
                PersistentDataType.INTEGER,
                chunkX
        );
        persistentDataContainer.set(
                new NamespacedKey(plugin.getPlugin(), CHUNK_Z_KEY),
                PersistentDataType.INTEGER,
                chunkZ
        );
        persistentDataContainer.set(
                new NamespacedKey(plugin.getPlugin(), CHUNK_TYPE_KEY),
                PersistentDataType.STRING,
                chunkType.name()
        );
        chunkBlock.setItemMeta(meta);
        return chunkBlock;
    }

    /**
     * Returns whether the provided item carries Chunk Block metadata.
     *
     * @param plugin runtime plugin
     * @param item candidate item
     * @return {@code true} when this item is a Chunk Block
     */
    public static boolean equals(
            final @NotNull RDT plugin,
            final @NotNull ItemStack item
    ) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }

        final PersistentDataContainer persistentDataContainer = meta.getPersistentDataContainer();
        return persistentDataContainer.has(
                new NamespacedKey(plugin.getPlugin(), TOWN_UUID_KEY),
                PersistentDataType.STRING
        ) && persistentDataContainer.has(
                new NamespacedKey(plugin.getPlugin(), TOWN_NAME_KEY),
                PersistentDataType.STRING
        ) && persistentDataContainer.has(
                new NamespacedKey(plugin.getPlugin(), MAYOR_UUID_KEY),
                PersistentDataType.STRING
        ) && persistentDataContainer.has(
                new NamespacedKey(plugin.getPlugin(), CHUNK_X_KEY),
                PersistentDataType.INTEGER
        ) && persistentDataContainer.has(
                new NamespacedKey(plugin.getPlugin(), CHUNK_Z_KEY),
                PersistentDataType.INTEGER
        ) && persistentDataContainer.has(
                new NamespacedKey(plugin.getPlugin(), CHUNK_TYPE_KEY),
                PersistentDataType.STRING
        );
    }

    /**
     * Reads the town UUID encoded in a Chunk Block item.
     *
     * @param plugin runtime plugin
     * @param item chunk block item
     * @return town UUID or {@code null} when unavailable/invalid
     */
    public static @Nullable UUID getTownUUID(
            final @NotNull RDT plugin,
            final @NotNull ItemStack item
    ) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        final String encodedUuid = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin.getPlugin(), TOWN_UUID_KEY),
                PersistentDataType.STRING
        );
        if (encodedUuid == null) {
            return null;
        }
        try {
            return UUID.fromString(encodedUuid);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Reads the town name encoded in a Chunk Block item.
     *
     * @param plugin runtime plugin
     * @param item chunk block item
     * @return encoded town name or {@code null}
     */
    public static @Nullable String getTownName(
            final @NotNull RDT plugin,
            final @NotNull ItemStack item
    ) {
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
     * Reads the mayor UUID encoded in a Chunk Block item.
     *
     * @param plugin runtime plugin
     * @param item chunk block item
     * @return mayor UUID or {@code null}
     */
    public static @Nullable UUID getMayorUUID(
            final @NotNull RDT plugin,
            final @NotNull ItemStack item
    ) {
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

    /**
     * Reads the target chunk X coordinate encoded in a Chunk Block item.
     *
     * @param plugin runtime plugin
     * @param item chunk block item
     * @return target chunk X or {@code null}
     */
    public static @Nullable Integer getChunkX(
            final @NotNull RDT plugin,
            final @NotNull ItemStack item
    ) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin.getPlugin(), CHUNK_X_KEY),
                PersistentDataType.INTEGER
        );
    }

    /**
     * Reads the target chunk Z coordinate encoded in a Chunk Block item.
     *
     * @param plugin runtime plugin
     * @param item chunk block item
     * @return target chunk Z or {@code null}
     */
    public static @Nullable Integer getChunkZ(
            final @NotNull RDT plugin,
            final @NotNull ItemStack item
    ) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        return meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin.getPlugin(), CHUNK_Z_KEY),
                PersistentDataType.INTEGER
        );
    }

    /**
     * Reads the chunk type encoded in a Chunk Block item.
     *
     * @param plugin runtime plugin
     * @param item chunk block item
     * @return chunk type encoded on the item, defaults to {@link ChunkType#CHUNK_BLOCK}
     */
    public static @NotNull ChunkType getChunkType(
            final @NotNull RDT plugin,
            final @NotNull ItemStack item
    ) {
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return ChunkType.CHUNK_BLOCK;
        }

        final String rawChunkType = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin.getPlugin(), CHUNK_TYPE_KEY),
                PersistentDataType.STRING
        );
        if (rawChunkType == null || rawChunkType.isBlank()) {
            return ChunkType.CHUNK_BLOCK;
        }

        try {
            return ChunkType.valueOf(rawChunkType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ChunkType.CHUNK_BLOCK;
        }
    }

    /**
     * Returns the material used by Chunk Block items.
     *
     * @return chunk block material
     */
    public static @NotNull Material getMaterial() {
        return DEFAULT_CHUNK_BLOCK_MATERIAL;
    }
}
