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

package com.raindropcentral.rdr.view;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.configs.ConfigSection;
import com.raindropcentral.rdr.database.entity.RDRPlayer;
import com.raindropcentral.rdr.database.entity.RStorage;
import com.raindropcentral.rdr.database.entity.StorageTrustStatus;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated storage list for the current player.
 *
 * <p>The view renders every persisted {@link RStorage} the current viewer may access, summarizes the
 * currently visible storage count relative to the configured maximum, and supports right-click storage
 * settings alongside normal left-click storage opens.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StoragePlayerView extends APaginatedView<RStorage> {

    private final State<RDR> rdr = initialState("plugin");

    /**
     * Creates the paginated storage list view.
     */
    public StoragePlayerView() {
        super(StorageOverviewView.class);
    }

    /**
     * Returns the translation namespace used by this view.
     *
     * @return storage player view translation key prefix
     */
    @Override
    protected String getKey() {
        return "storage_player_ui";
    }

    /**
     * Returns the layout used by the paginated storage list.
     *
     * @return six-row layout with summary, pagination, and back navigation support
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            " OOOOOOO ",
            "  < p >  "
        };
    }

    /**
     * Loads the current player's storages for pagination.
     *
     * @param context current view context
     * @return completed future containing the player's storages
     */
    @Override
    protected CompletableFuture<List<RStorage>> getAsyncPaginationSource(final @NotNull Context context) {
        return CompletableFuture.completedFuture(this.findStorages(context));
    }

    /**
     * Renders a single storage entry in the paginated grid.
     *
     * @param context current view context
     * @param builder item component builder used for the entry slot
     * @param index zero-based entry index on the full source list
     * @param entry storage entry being rendered
     */
    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull RStorage entry
    ) {
        final RDR plugin = this.rdr.get(context);
        builder.withItem(this.createStorageEntryItem(context.getPlayer(), entry))
            .onClick(clickContext -> this.handleStorageClick(clickContext, plugin, entry));
    }

    /**
     * Renders the view summary and empty-state cards when needed.
     *
     * @param render render context for slot registration
     * @param player player viewing the storage list
     */
    @Override
    protected void onPaginatedRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        final RDR plugin = this.rdr.get(render);
        final ConfigSection config = plugin.getDefaultConfig();
        final List<RStorage> storages = this.findStorages(render);
        final RDRPlayer rdrPlayer = this.findPlayer(render);
        final int ownedStorages = storages.size();
        final int maxStorages = plugin.getMaximumStorages(player, config);
        final String maxStoragesDisplay = this.formatMaxStorages(player, maxStorages);

        render.layoutSlot('s', this.createSummaryItem(player, ownedStorages, maxStoragesDisplay));

        if (rdrPlayer == null) {
            render.slot(22).renderWith(() -> this.createMissingProfileItem(player));
            return;
        }

        if (ownedStorages == 0) {
            render.slot(22).renderWith(() -> this.createEmptyStateItem(player));
        }
    }

    /**
     * Cancels item interaction so GUI items cannot be moved.
     *
     * @param click slot click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private RDRPlayer findPlayer(final @NotNull Context context) {
        final RDR plugin = this.rdr.get(context);
        return plugin.getPlayerRepository() == null
            ? null
            : plugin.getPlayerRepository().findByPlayer(context.getPlayer().getUniqueId());
    }

    private @NotNull List<RStorage> findStorages(final @NotNull Context context) {
        final RDR plugin = this.rdr.get(context);
        return plugin.getStorageRepository() == null
            ? List.of()
            : plugin.getStorageRepository().findAccessibleByPlayerUuid(context.getPlayer().getUniqueId());
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final int ownedStorages,
        final @NotNull String maxStorages
    ) {
        return UnifiedBuilderFactory.item(Material.BARREL)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "owned_storages", ownedStorages,
                    "max_storages", maxStorages
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createStorageEntryItem(
        final @NotNull Player player,
        final @NotNull RStorage storage
    ) {
        final String ownerName = storage.getPlayer() == null
            ? "Unknown"
            : this.resolveOwnerName(storage, player);
        final String accessLevel = storage.isOwner(player.getUniqueId())
            ? this.i18n("entry.access.owner", player).build().getI18nVersionWrapper().asPlaceholder()
            : this.resolveAccessPlaceholder(player, storage.getTrustStatus(player.getUniqueId()));

        return UnifiedBuilderFactory.item(Material.CHEST)
            .setName(this.i18n("entry.name", player)
                .withPlaceholders(Map.of(
                    "storage_key", storage.getStorageKey(),
                    "storage_number", storage.getId() == null ? "New" : storage.getId()
                ))
                .build()
                .component())
            .setLore(this.i18n("entry.lore", player)
                .withPlaceholders(Map.of(
                    "storage_key", storage.getStorageKey(),
                    "owner_name", ownerName,
                    "access_level", accessLevel,
                    "hotkey_display", storage.getHotkey() == null ? "-" : storage.getHotkey(),
                    "stored_slots", storage.getStoredSlotCount(),
                    "inventory_size", storage.getInventorySize(),
                    "available_slots", storage.getInventorySize() - storage.getStoredSlotCount()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEmptyStateItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("empty.name", player).build().component())
            .setLore(this.i18n("empty.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMissingProfileItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("missing.name", player).build().component())
            .setLore(this.i18n("missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private void handleStorageClick(
        final @NotNull SlotClickContext clickContext,
        final @NotNull RDR plugin,
        final @NotNull RStorage storage
    ) {
        if (clickContext.isRightClick()) {
            this.openStorageSettings(clickContext, plugin, storage);
            return;
        }

        StorageViewLauncher.openStorage(clickContext.getPlayer(), plugin, storage);
    }

    private void openStorageSettings(
        final @NotNull SlotClickContext clickContext,
        final @NotNull RDR plugin,
        final @NotNull RStorage storage
    ) {
        final Map<String, Object> initialData = new HashMap<>();
        initialData.put("plugin", plugin);
        initialData.put("storage_id", storage.getId());
        initialData.put("storage_key", storage.getStorageKey());

        clickContext.openForPlayer(StorageSettingsView.class, initialData);
    }

    private @NotNull String resolveAccessPlaceholder(
        final @NotNull Player player,
        final @NotNull StorageTrustStatus status
    ) {
        return this.i18n("entry.access." + status.name().toLowerCase(), player)
            .build()
            .getI18nVersionWrapper()
            .asPlaceholder();
    }

    private @NotNull String resolveOwnerName(
        final @NotNull RStorage storage,
        final @NotNull Player viewer
    ) {
        if (storage.getPlayer() == null) {
            return "Unknown";
        }

        if (storage.getPlayer().getIdentifier().equals(viewer.getUniqueId())) {
            return viewer.getName();
        }

        final String offlineName = Bukkit.getOfflinePlayer(storage.getPlayer().getIdentifier()).getName();
        return offlineName == null || offlineName.isBlank()
            ? storage.getPlayer().getIdentifier().toString()
            : offlineName;
    }

    private @NotNull String formatMaxStorages(
        final @NotNull Player player,
        final int maxStorages
    ) {
        if (maxStorages > 0) {
            return Integer.toString(maxStorages);
        }
        return this.i18n("summary.unlimited", player).build().getI18nVersionWrapper().asPlaceholder();
    }
}
