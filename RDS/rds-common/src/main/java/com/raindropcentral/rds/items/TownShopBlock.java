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

package com.raindropcentral.rds.items;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.database.entity.TownShopOutpost;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Material;
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
 * Creates and parses bound Outpost town-shop placement tokens.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownShopBlock {

    private static final String PROTECTION_PLUGIN_KEY = "town_shop_protection_plugin";
    private static final String TOWN_IDENTIFIER_KEY = "town_shop_town_identifier";
    private static final String TOWN_DISPLAY_NAME_KEY = "town_shop_town_display_name";
    private static final String CHUNK_UUID_KEY = "town_shop_chunk_uuid";
    private static final String WORLD_NAME_KEY = "town_shop_world_name";
    private static final String CHUNK_X_KEY = "town_shop_chunk_x";
    private static final String CHUNK_Z_KEY = "town_shop_chunk_z";

    private TownShopBlock() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Creates one bound town-shop token for the supplied outpost.
     *
     * @param plugin plugin instance
     * @param player player receiving the token
     * @param outpost bound outpost metadata
     * @return bound town-shop token item
     */
    public static @NotNull ItemStack getTownShopBlock(
        final @NotNull RDS plugin,
        final @NotNull Player player,
        final @NotNull TownShopOutpost outpost
    ) {
        final ItemStack shop = new ItemStack(Material.CHEST);
        final ItemMeta meta = shop.getItemMeta();
        if (meta == null) {
            return shop;
        }

        final Map<String, Object> placeholders = Map.of(
            "town_name", outpost.getTownDisplayName(),
            "world_name", outpost.getWorldName(),
            "chunk_x", outpost.getChunkX(),
            "chunk_z", outpost.getChunkZ()
        );
        meta.displayName(new I18n.Builder("town_shop_block.name", player)
            .withPlaceholders(placeholders)
            .build()
            .component());
        meta.lore(new I18n.Builder("town_shop_block.lore", player)
            .withPlaceholders(placeholders)
            .build()
            .children());

        final PersistentDataContainer persistentDataContainer = meta.getPersistentDataContainer();
        persistentDataContainer.set(key(plugin, PROTECTION_PLUGIN_KEY), PersistentDataType.STRING, outpost.getProtectionPlugin());
        persistentDataContainer.set(key(plugin, TOWN_IDENTIFIER_KEY), PersistentDataType.STRING, outpost.getTownIdentifier());
        persistentDataContainer.set(key(plugin, TOWN_DISPLAY_NAME_KEY), PersistentDataType.STRING, outpost.getTownDisplayName());
        persistentDataContainer.set(key(plugin, CHUNK_UUID_KEY), PersistentDataType.STRING, outpost.getChunkUuid().toString());
        persistentDataContainer.set(key(plugin, WORLD_NAME_KEY), PersistentDataType.STRING, outpost.getWorldName());
        persistentDataContainer.set(key(plugin, CHUNK_X_KEY), PersistentDataType.INTEGER, outpost.getChunkX());
        persistentDataContainer.set(key(plugin, CHUNK_Z_KEY), PersistentDataType.INTEGER, outpost.getChunkZ());
        shop.setItemMeta(meta);
        return shop;
    }

    /**
     * Returns whether the supplied item is a bound town-shop token.
     *
     * @param plugin plugin instance
     * @param item target item
     * @return {@code true} when the item is a town-shop token
     */
    public static boolean equals(final @NotNull RDS plugin, final @Nullable ItemStack item) {
        return getMetadata(plugin, item) != null;
    }

    /**
     * Parses town-shop token metadata from an item.
     *
     * @param plugin plugin instance
     * @param item target item
     * @return parsed metadata, or {@code null} when the item is not a valid town-shop token
     */
    public static @Nullable Metadata getMetadata(final @NotNull RDS plugin, final @Nullable ItemStack item) {
        if (item == null || item.getType() != Material.CHEST) {
            return null;
        }

        final ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        final PersistentDataContainer persistentDataContainer = meta.getPersistentDataContainer();
        final String protectionPlugin = persistentDataContainer.get(key(plugin, PROTECTION_PLUGIN_KEY), PersistentDataType.STRING);
        final String townIdentifier = persistentDataContainer.get(key(plugin, TOWN_IDENTIFIER_KEY), PersistentDataType.STRING);
        final String townDisplayName = persistentDataContainer.get(key(plugin, TOWN_DISPLAY_NAME_KEY), PersistentDataType.STRING);
        final String rawChunkUuid = persistentDataContainer.get(key(plugin, CHUNK_UUID_KEY), PersistentDataType.STRING);
        final String worldName = persistentDataContainer.get(key(plugin, WORLD_NAME_KEY), PersistentDataType.STRING);
        final Integer chunkX = persistentDataContainer.get(key(plugin, CHUNK_X_KEY), PersistentDataType.INTEGER);
        final Integer chunkZ = persistentDataContainer.get(key(plugin, CHUNK_Z_KEY), PersistentDataType.INTEGER);
        if (protectionPlugin == null
            || townIdentifier == null
            || townDisplayName == null
            || rawChunkUuid == null
            || worldName == null
            || chunkX == null
            || chunkZ == null) {
            return null;
        }

        try {
            return new Metadata(
                protectionPlugin,
                townIdentifier,
                townDisplayName,
                UUID.fromString(rawChunkUuid),
                worldName,
                chunkX,
                chunkZ
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static @NotNull NamespacedKey key(final @NotNull RDS plugin, final @NotNull String key) {
        return new NamespacedKey(plugin.getPlugin(), key);
    }

    /**
     * Immutable parsed town-shop token metadata.
     *
     * @param protectionPlugin source protection plugin id
     * @param townIdentifier stable town identifier
     * @param townDisplayName town display name
     * @param chunkUuid outpost chunk UUID
     * @param worldName outpost world name
     * @param chunkX outpost chunk x
     * @param chunkZ outpost chunk z
     */
    public record Metadata(
        @NotNull String protectionPlugin,
        @NotNull String townIdentifier,
        @NotNull String townDisplayName,
        @NotNull UUID chunkUuid,
        @NotNull String worldName,
        int chunkX,
        int chunkZ
    ) {
        /**
         * Creates a normalized metadata snapshot.
         *
         * @param protectionPlugin source protection plugin id
         * @param townIdentifier stable town identifier
         * @param townDisplayName town display name
         * @param chunkUuid outpost chunk UUID
         * @param worldName outpost world name
         * @param chunkX outpost chunk x
         * @param chunkZ outpost chunk z
         */
        public Metadata {
            protectionPlugin = Objects.requireNonNull(protectionPlugin, "protectionPlugin").trim();
            townIdentifier = Objects.requireNonNull(townIdentifier, "townIdentifier").trim();
            townDisplayName = Objects.requireNonNull(townDisplayName, "townDisplayName").trim();
            chunkUuid = Objects.requireNonNull(chunkUuid, "chunkUuid");
            worldName = Objects.requireNonNull(worldName, "worldName").trim();
        }
    }
}
