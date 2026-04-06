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

import java.util.Objects;
import java.util.UUID;

/**
 * Bound nexus-block item payload used to finalize GUI-created towns.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class Nexus {

    private static final String ITEM_KIND = "nexus";
    private static final String ITEM_TYPE_KEY = "rdt_item_type";
    private static final String TOWN_UUID_KEY = "rdt_town_uuid";
    private static final String MAYOR_UUID_KEY = "rdt_mayor_uuid";
    private static final String TOWN_NAME_KEY = "rdt_town_name";
    private static final String TOWN_COLOR_KEY = "rdt_town_color";

    private Nexus() {
    }

    /**
     * Creates a bound nexus item for a pending town.
     *
     * @param plugin active plugin runtime
     * @param player receiving player
     * @param townUuid pre-generated town UUID
     * @param townName chosen town name
     * @param townColor chosen canonical town color
     * @return bound nexus item
     */
    public static @NotNull ItemStack getNexusItem(
        final @NotNull RDT plugin,
        final @NotNull Player player,
        final @NotNull UUID townUuid,
        final @NotNull String townName,
        final @NotNull String townColor
    ) {
        final ItemStack nexus = new ItemStack(plugin.getDefaultConfig().getChunkTypeIconMaterial(ChunkType.NEXUS));
        final ItemMeta meta = Objects.requireNonNull(nexus.getItemMeta(), "nexus item meta");
        meta.displayName(new I18n.Builder("nexus_block.name", player)
            .withPlaceholder("town", townName)
            .build()
            .component());
        meta.lore(new I18n.Builder("nexus_block.lore", player)
            .withPlaceholders(java.util.Map.of(
                "town", townName,
                "town_color", townColor
            ))
            .build()
            .children());
        final PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(key(plugin, ITEM_TYPE_KEY), PersistentDataType.STRING, ITEM_KIND);
        data.set(key(plugin, TOWN_UUID_KEY), PersistentDataType.STRING, townUuid.toString());
        data.set(key(plugin, MAYOR_UUID_KEY), PersistentDataType.STRING, player.getUniqueId().toString());
        data.set(key(plugin, TOWN_NAME_KEY), PersistentDataType.STRING, townName);
        data.set(key(plugin, TOWN_COLOR_KEY), PersistentDataType.STRING, townColor);
        nexus.setItemMeta(meta);
        return nexus;
    }

    /**
     * Returns whether an item is a bound nexus payload.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return {@code true} when the item is an RDT nexus
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
     * Returns the bound town UUID stored on the item.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return stored town UUID, or {@code null} when unavailable
     */
    public static @Nullable UUID getTownUUID(final @NotNull RDT plugin, final @Nullable ItemStack item) {
        return parseUuid(readString(plugin, item, TOWN_UUID_KEY));
    }

    /**
     * Returns the bound mayor UUID stored on the item.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return stored mayor UUID, or {@code null} when unavailable
     */
    public static @Nullable UUID getMayorUUID(final @NotNull RDT plugin, final @Nullable ItemStack item) {
        return parseUuid(readString(plugin, item, MAYOR_UUID_KEY));
    }

    /**
     * Returns the bound town name stored on the item.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return stored town name, or {@code null} when unavailable
     */
    public static @Nullable String getTownName(final @NotNull RDT plugin, final @Nullable ItemStack item) {
        return readString(plugin, item, TOWN_NAME_KEY);
    }

    /**
     * Returns the bound town color stored on the item.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return stored town color, or {@code null} when unavailable
     */
    public static @Nullable String getTownColor(final @NotNull RDT plugin, final @Nullable ItemStack item) {
        return readString(plugin, item, TOWN_COLOR_KEY);
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
