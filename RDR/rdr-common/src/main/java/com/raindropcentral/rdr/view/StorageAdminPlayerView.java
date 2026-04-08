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
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * RDR admin player-management landing view.
 *
 * <p>This panel links player/group override editors, storage drain/reset controls, and the global
 * force-close action for open RDR views.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class StorageAdminPlayerView extends BaseView {

    private static final String ADMIN_COMMAND_PERMISSION = "raindroprdr.command.admin";

    private final State<RDR> rdr = initialState("plugin");

    /**
     * Creates the admin player-management landing view.
     */
    public StorageAdminPlayerView() {
        super(StorageAdminView.class);
    }

    /**
     * Returns the i18n key used by this view.
     *
     * @return view i18n key
     */
    @Override
    protected @NotNull String getKey() {
        return "storage_admin_player_ui";
    }

    /**
     * Returns the menu layout for this view.
     *
     * @return rendered menu layout
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            "  p g d  ",
            "    r a  ",
            "         "
        };
    }

    /**
     * Renders admin player-management controls.
     *
     * @param render render context
     * @param player player viewing the menu
     */
    @Override
    public void onFirstRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        if (!this.hasAdminAccess(player)) {
            render.slot(13).renderWith(() -> this.createLockedItem(player));
            return;
        }

        render.layoutSlot('s', this.createSummaryItem(player));

        render.layoutSlot('p', this.createPlayerOverridesItem(player))
            .onClick(clickContext -> clickContext.openForPlayer(
                StorageAdminPlayerSelectView.class,
                Map.of("plugin", this.rdr.get(clickContext))
            ));

        render.layoutSlot('g', this.createGroupOverridesItem(player))
            .onClick(clickContext -> clickContext.openForPlayer(
                StorageAdminGroupEditView.class,
                Map.of("plugin", this.rdr.get(clickContext))
            ));

        render.layoutSlot('d', this.createForceDrainStorageItem(player))
            .onClick(clickContext -> clickContext.openForPlayer(
                StorageAdminStorageControlView.class,
                Map.of(
                    "plugin", this.rdr.get(clickContext),
                    "actionMode", StorageAdminStorageControlMode.FORCE_DRAIN_STORAGE.name()
                )
            ));

        render.layoutSlot('r', this.createForceResetStorageItem(player))
            .onClick(clickContext -> clickContext.openForPlayer(
                StorageAdminStorageControlView.class,
                Map.of(
                    "plugin", this.rdr.get(clickContext),
                    "actionMode", StorageAdminStorageControlMode.FORCE_RESET_STORAGE.name()
                )
            ));

        render.layoutSlot('a', this.createForceCloseAllViewsItem(player))
            .onClick(this::handleForceCloseAllViewsClick);
    }

    /**
     * Cancels vanilla click handling so the menu behaves as an action UI.
     *
     * @param click click context
     */
    @Override
    public void onClick(
        final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    private void handleForceCloseAllViewsClick(
        final @NotNull SlotClickContext clickContext
    ) {
        clickContext.setCancelled(true);
        if (!this.hasAdminAccess(clickContext.getPlayer())) {
            this.i18n("feedback.access_denied_message", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            return;
        }

        final int closedViews = StorageAdminForceCloseSupport.closeAllRdrViews(this.rdr.get(clickContext));
        this.i18n("feedback.force_close_all_views", clickContext.getPlayer())
            .withPlaceholder("closed_views", closedViews)
            .includePrefix()
            .build()
            .sendMessage();
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.COMMAND_BLOCK)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createPlayerOverridesItem(
        final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.PLAYER_HEAD)
            .setName(this.i18n("actions.player_overrides.name", player).build().component())
            .setLore(this.i18n("actions.player_overrides.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createGroupOverridesItem(
        final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.NAME_TAG)
            .setName(this.i18n("actions.group_overrides.name", player).build().component())
            .setLore(this.i18n("actions.group_overrides.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createForceDrainStorageItem(
        final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BUCKET)
            .setName(this.i18n("actions.force_drain_storage.name", player).build().component())
            .setLore(this.i18n("actions.force_drain_storage.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createForceResetStorageItem(
        final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("actions.force_reset_storage.name", player).build().component())
            .setLore(this.i18n("actions.force_reset_storage.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createForceCloseAllViewsItem(
        final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.REDSTONE_BLOCK)
            .setName(this.i18n("actions.force_close_all_views.name", player).build().component())
            .setLore(this.i18n("actions.force_close_all_views.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createLockedItem(
        final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("feedback.access_denied.name", player).build().component())
            .setLore(this.i18n("feedback.access_denied.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private boolean hasAdminAccess(
        final @NotNull Player player
    ) {
        return player.isOp() || player.hasPermission(ADMIN_COMMAND_PERMISSION);
    }
}
