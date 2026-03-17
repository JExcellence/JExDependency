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

import java.util.Map;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.configs.ConfigSection;
import com.raindropcentral.rdr.database.entity.RDRPlayer;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Root storage menu for RDR players.
 *
 * <p>This view summarizes the player's current storage ownership and exposes a button to open the
 * paginated per-storage list. Players with admin access also receive a shortcut to storage-admin
 * controls.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageOverviewView extends BaseView {

    private static final String ADMIN_COMMAND_PERMISSION = "raindroprdr.command.admin";

    private final State<RDR> rdr = initialState("plugin");

    /**
     * Returns the translation namespace used by this view.
     *
     * @return storage overview translation key prefix
     */
    @Override
    protected String getKey() {
        return "storage_overview_ui";
    }

    /**
     * Returns the inventory layout used by this overview.
     *
     * @return three-row layout for the overview menu
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            "   o t a ",
            "         "
        };
    }

    /**
     * Disables automatic filler item placement for this compact menu.
     *
     * @return {@code false} so only explicit controls render
     */
    @Override
    protected boolean shouldAutoFill() {
        return false;
    }

    /**
     * Suppresses the automatic back button because this is a root menu.
     *
     * @param render render context
     * @param player player viewing the menu
     */
    @Override
    public void renderNavigationButtons(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        // Root menu; no back button.
    }

    /**
     * Renders the storage summary, list-navigation button, storage store button, and optional.
     * admin button.
     *
     * @param render render context for slot registration
     * @param player player viewing the menu
     */
    @Override
    public void onFirstRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        final RDR plugin = this.rdr.get(render);
        final RDRPlayer rdrPlayer = this.findPlayer(render);
        final ConfigSection config = plugin.getDefaultConfig();
        final int ownedStorages = rdrPlayer == null ? 0 : rdrPlayer.getStorages().size();
        final int maxStorages = plugin.getMaximumStorages(player, config);
        final String maxStoragesDisplay = this.formatMaxStorages(player, maxStorages);

        render.layoutSlot('s')
            .renderWith(() -> this.createSummaryItem(player, ownedStorages, maxStoragesDisplay));

        render.layoutSlot('o')
            .withItem(this.createOpenListItem(player, ownedStorages, maxStoragesDisplay))
            .onClick(clickContext -> clickContext.openForPlayer(
                StoragePlayerView.class,
                Map.of(
                    "plugin",
                    this.rdr.get(clickContext)
                )
            ));

        render.layoutSlot('t')
            .withItem(this.createStoreItem(player, ownedStorages, maxStoragesDisplay))
            .onClick(clickContext -> clickContext.openForPlayer(
                StorageStoreView.class,
                Map.of(
                    "plugin",
                    this.rdr.get(clickContext)
                )
            ));

        if (this.hasAdminAccess(player)) {
            render.layoutSlot('a')
                .withItem(this.createAdminItem(player))
                .onClick(clickContext -> clickContext.openForPlayer(
                    StorageAdminView.class,
                    Map.of(
                        "plugin",
                        this.rdr.get(clickContext)
                    )
                ));
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

    private @NotNull ItemStack createOpenListItem(
        final @NotNull Player player,
        final int ownedStorages,
        final @NotNull String maxStorages
    ) {
        return UnifiedBuilderFactory.item(Material.ENDER_CHEST)
            .setName(this.i18n("open.name", player).build().component())
            .setLore(this.i18n("open.lore", player)
                .withPlaceholders(Map.of(
                    "owned_storages", ownedStorages,
                    "max_storages", maxStorages
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createStoreItem(
        final @NotNull Player player,
        final int ownedStorages,
        final @NotNull String maxStorages
    ) {
        return UnifiedBuilderFactory.item(Material.GOLD_INGOT)
            .setName(this.i18n("store.name", player).build().component())
            .setLore(this.i18n("store.lore", player)
                .withPlaceholders(Map.of(
                    "owned_storages", ownedStorages,
                    "max_storages", maxStorages
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createAdminItem(
        final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.COMMAND_BLOCK)
            .setName(this.i18n("admin.name", player).build().component())
            .setLore(this.i18n("admin.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private boolean hasAdminAccess(
        final @NotNull Player player
    ) {
        return player.isOp() || player.hasPermission(ADMIN_COMMAND_PERMISSION);
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
