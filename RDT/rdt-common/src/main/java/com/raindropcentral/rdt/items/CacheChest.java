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
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.block.TileState;
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
 * Bound town cache-chest item and placed chest payload helper.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class CacheChest {

    private static final String ITEM_KIND = "bank_cache";
    private static final String ITEM_TYPE_KEY = "rdt_item_type";
    private static final String TOWN_UUID_KEY = "rdt_town_uuid";

    private CacheChest() {
    }

    /**
     * Creates a bound cache-chest item for one town.
     *
     * @param plugin active plugin runtime
     * @param player receiving player
     * @param townUuid owning town UUID
     * @param townName owning town name
     * @return bound cache-chest item
     */
    public static @NotNull ItemStack getCacheChestItem(
        final @NotNull RDT plugin,
        final @NotNull Player player,
        final @NotNull UUID townUuid,
        final @NotNull String townName
    ) {
        final ItemStack cacheChest = createUnlocalizedBoundItem(plugin, townUuid, townName);
        final ItemMeta meta = Objects.requireNonNull(cacheChest.getItemMeta(), "cache chest meta");
        meta.displayName(new I18n.Builder("cache_chest.name", player)
            .withPlaceholder("town_name", townName)
            .build()
            .component());
        meta.lore(new I18n.Builder("cache_chest.lore", player)
            .withPlaceholder("town_name", townName)
            .build()
            .children());
        cacheChest.setItemMeta(meta);
        return cacheChest;
    }

    /**
     * Creates a bound cache-chest item without player-localized display text.
     *
     * @param plugin active plugin runtime
     * @param townUuid owning town UUID
     * @param townName owning town name
     * @return bound cache-chest item
     */
    public static @NotNull ItemStack createUnlocalizedBoundItem(
        final @NotNull RDT plugin,
        final @NotNull UUID townUuid,
        final @NotNull String townName
    ) {
        final ItemStack cacheChest = new ItemStack(plugin.getBankConfig().getCache().itemMaterial());
        final ItemMeta meta = Objects.requireNonNull(cacheChest.getItemMeta(), "cache chest meta");
        meta.displayName(Component.text("Town Cache Chest"));
        meta.lore(java.util.List.of(
            Component.text("Town: " + townName),
            Component.text("Place this inside an eligible Bank plot.")
        ));
        writeBinding(plugin, meta.getPersistentDataContainer(), townUuid);
        cacheChest.setItemMeta(meta);
        return cacheChest;
    }

    /**
     * Returns whether an item is a bound cache-chest payload.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return {@code true} when the item is a bound cache chest
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
     * Returns whether a placed chest state is a bound cache chest.
     *
     * @param plugin active plugin runtime
     * @param tileState placed storage block to inspect
     * @return {@code true} when the block is a bound cache chest
     */
    public static boolean isPlacedCacheChest(final @NotNull RDT plugin, final @Nullable TileState tileState) {
        return tileState != null && ITEM_KIND.equalsIgnoreCase(
            tileState.getPersistentDataContainer().get(key(plugin, ITEM_TYPE_KEY), PersistentDataType.STRING)
        );
    }

    /**
     * Binds a placed chest to one town cache using the chest state PDC.
     *
     * @param plugin active plugin runtime
     * @param tileState placed storage block to update
     * @param townUuid owning town UUID
     */
    public static void bindPlacedCacheChest(
        final @NotNull RDT plugin,
        final @NotNull TileState tileState,
        final @NotNull UUID townUuid
    ) {
        writeBinding(plugin, tileState.getPersistentDataContainer(), townUuid);
        tileState.update(true, false);
    }

    /**
     * Returns the stored owning town UUID from an item or chest state.
     *
     * @param plugin active plugin runtime
     * @param item item to inspect
     * @return stored town UUID, or {@code null} when unavailable
     */
    public static @Nullable UUID getTownUuid(final @NotNull RDT plugin, final @Nullable ItemStack item) {
        return parseUuid(readString(plugin, item, TOWN_UUID_KEY));
    }

    /**
     * Returns the stored owning town UUID from a placed chest state.
     *
     * @param plugin active plugin runtime
     * @param tileState storage block to inspect
     * @return stored town UUID, or {@code null} when unavailable
     */
    public static @Nullable UUID getTownUuid(final @NotNull RDT plugin, final @Nullable TileState tileState) {
        return parseUuid(readString(plugin, tileState, TOWN_UUID_KEY));
    }

    private static void writeBinding(
        final @NotNull RDT plugin,
        final @NotNull PersistentDataContainer data,
        final @NotNull UUID townUuid
    ) {
        data.set(key(plugin, ITEM_TYPE_KEY), PersistentDataType.STRING, ITEM_KIND);
        data.set(key(plugin, TOWN_UUID_KEY), PersistentDataType.STRING, townUuid.toString());
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
        final @Nullable TileState tileState,
        final @NotNull String key
    ) {
        if (tileState == null) {
            return null;
        }
        return tileState.getPersistentDataContainer().get(
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
        try {
            return new NamespacedKey(plugin.getPlugin(), key);
        } catch (final IllegalArgumentException | NullPointerException exception) {
            return new NamespacedKey("rdt", key);
        }
    }
}
