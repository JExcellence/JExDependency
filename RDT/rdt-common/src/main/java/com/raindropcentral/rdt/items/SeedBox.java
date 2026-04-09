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
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Chest;
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
 * Bound Farm seed-box item and placed chest payload helper.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class SeedBox {

    private static final String ITEM_KIND = "seed_box";
    private static final String ITEM_TYPE_KEY = "rdt_item_type";
    private static final String TOWN_UUID_KEY = "rdt_town_uuid";
    private static final String WORLD_KEY = "rdt_claim_world";
    private static final String CHUNK_X_KEY = "rdt_claim_chunk_x";
    private static final String CHUNK_Z_KEY = "rdt_claim_chunk_z";

    private SeedBox() {
    }

    /**
     * Creates a bound Farm seed-box item.
     *
     * @param plugin active plugin runtime
     * @param player receiving player
     * @param townUuid owning town UUID
     * @param worldName owning world name
     * @param chunkX owning chunk X coordinate
     * @param chunkZ owning chunk Z coordinate
     * @return bound seed-box chest item
     */
    public static @NotNull ItemStack getSeedBoxItem(
        final @NotNull RDT plugin,
        final @NotNull Player player,
        final @NotNull UUID townUuid,
        final @NotNull String worldName,
        final int chunkX,
        final int chunkZ
    ) {
        final ItemStack seedBox = new ItemStack(Material.CHEST);
        final ItemMeta meta = Objects.requireNonNull(seedBox.getItemMeta(), "seed box meta");
        meta.displayName(new I18n.Builder("seed_box.name", player)
            .withPlaceholders(Map.of(
                "chunk_x", chunkX,
                "chunk_z", chunkZ
            ))
            .build()
            .component());
        meta.lore(new I18n.Builder("seed_box.lore", player)
            .withPlaceholders(Map.of(
                "world", worldName,
                "chunk_x", chunkX,
                "chunk_z", chunkZ
            ))
            .build()
            .children());
        writeBinding(plugin, meta.getPersistentDataContainer(), townUuid, worldName, chunkX, chunkZ);
        seedBox.setItemMeta(meta);
        return seedBox;
    }

    /**
     * Returns whether an item is a bound seed-box payload.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return {@code true} when the item is a bound seed box
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
     * Returns whether a placed chest state is a bound seed box.
     *
     * @param plugin active plugin runtime
     * @param chest placed chest to inspect
     * @return {@code true} when the chest is a bound seed box
     */
    public static boolean isPlacedSeedBox(final @NotNull RDT plugin, final @Nullable Chest chest) {
        return chest != null && isPlacedSeedBox(plugin, chest.getPersistentDataContainer());
    }

    /**
     * Binds a placed chest to one Farm chunk using the chest state PDC.
     *
     * @param plugin active plugin runtime
     * @param chest placed chest to update
     * @param townUuid owning town UUID
     * @param worldName owning world name
     * @param chunkX owning chunk X coordinate
     * @param chunkZ owning chunk Z coordinate
     */
    public static void bindPlacedSeedBox(
        final @NotNull RDT plugin,
        final @NotNull Chest chest,
        final @NotNull UUID townUuid,
        final @NotNull String worldName,
        final int chunkX,
        final int chunkZ
    ) {
        writeBinding(plugin, chest.getPersistentDataContainer(), townUuid, worldName, chunkX, chunkZ);
        chest.update(true, false);
    }

    /**
     * Returns the stored owning town UUID from an item or chest state.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return stored town UUID, or {@code null} when unavailable
     */
    public static @Nullable UUID getTownUUID(final @NotNull RDT plugin, final @Nullable ItemStack item) {
        return parseUuid(readString(plugin, item, TOWN_UUID_KEY));
    }

    /**
     * Returns the stored owning town UUID from a placed chest state.
     *
     * @param plugin active plugin runtime
     * @param chest chest to inspect
     * @return stored town UUID, or {@code null} when unavailable
     */
    public static @Nullable UUID getTownUUID(final @NotNull RDT plugin, final @Nullable Chest chest) {
        return parseUuid(readString(plugin, chest, TOWN_UUID_KEY));
    }

    /**
     * Returns the stored owning world name from an item or chest state.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return stored world name, or {@code null} when unavailable
     */
    public static @Nullable String getWorldName(final @NotNull RDT plugin, final @Nullable ItemStack item) {
        return readString(plugin, item, WORLD_KEY);
    }

    /**
     * Returns the stored owning world name from a placed chest state.
     *
     * @param plugin active plugin runtime
     * @param chest chest to inspect
     * @return stored world name, or {@code null} when unavailable
     */
    public static @Nullable String getWorldName(final @NotNull RDT plugin, final @Nullable Chest chest) {
        return readString(plugin, chest, WORLD_KEY);
    }

    /**
     * Returns the stored owning chunk X coordinate from an item or chest state.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return stored chunk X coordinate, or {@code null} when unavailable
     */
    public static @Nullable Integer getChunkX(final @NotNull RDT plugin, final @Nullable ItemStack item) {
        return readInteger(plugin, item, CHUNK_X_KEY);
    }

    /**
     * Returns the stored owning chunk X coordinate from a placed chest state.
     *
     * @param plugin active plugin runtime
     * @param chest chest to inspect
     * @return stored chunk X coordinate, or {@code null} when unavailable
     */
    public static @Nullable Integer getChunkX(final @NotNull RDT plugin, final @Nullable Chest chest) {
        return readInteger(plugin, chest, CHUNK_X_KEY);
    }

    /**
     * Returns the stored owning chunk Z coordinate from an item or chest state.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return stored chunk Z coordinate, or {@code null} when unavailable
     */
    public static @Nullable Integer getChunkZ(final @NotNull RDT plugin, final @Nullable ItemStack item) {
        return readInteger(plugin, item, CHUNK_Z_KEY);
    }

    /**
     * Returns the stored owning chunk Z coordinate from a placed chest state.
     *
     * @param plugin active plugin runtime
     * @param chest chest to inspect
     * @return stored chunk Z coordinate, or {@code null} when unavailable
     */
    public static @Nullable Integer getChunkZ(final @NotNull RDT plugin, final @Nullable Chest chest) {
        return readInteger(plugin, chest, CHUNK_Z_KEY);
    }

    private static void writeBinding(
        final @NotNull RDT plugin,
        final @NotNull PersistentDataContainer data,
        final @NotNull UUID townUuid,
        final @NotNull String worldName,
        final int chunkX,
        final int chunkZ
    ) {
        data.set(key(plugin, ITEM_TYPE_KEY), PersistentDataType.STRING, ITEM_KIND);
        data.set(key(plugin, TOWN_UUID_KEY), PersistentDataType.STRING, townUuid.toString());
        data.set(key(plugin, WORLD_KEY), PersistentDataType.STRING, worldName);
        data.set(key(plugin, CHUNK_X_KEY), PersistentDataType.INTEGER, chunkX);
        data.set(key(plugin, CHUNK_Z_KEY), PersistentDataType.INTEGER, chunkZ);
    }

    private static boolean isPlacedSeedBox(
        final @NotNull RDT plugin,
        final @NotNull PersistentDataContainer persistentDataContainer
    ) {
        return ITEM_KIND.equalsIgnoreCase(
            persistentDataContainer.get(key(plugin, ITEM_TYPE_KEY), PersistentDataType.STRING)
        );
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

    private static @Nullable String readString(
        final @NotNull RDT plugin,
        final @Nullable Chest chest,
        final @NotNull String key
    ) {
        if (chest == null) {
            return null;
        }
        return chest.getPersistentDataContainer().get(
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

    private static @Nullable Integer readInteger(
        final @NotNull RDT plugin,
        final @Nullable Chest chest,
        final @NotNull String key
    ) {
        if (chest == null) {
            return null;
        }
        return chest.getPersistentDataContainer().get(
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
