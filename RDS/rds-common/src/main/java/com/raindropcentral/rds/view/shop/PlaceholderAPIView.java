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

package com.raindropcentral.rds.view.shop;

import java.util.Map;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Administrative PlaceholderAPI integration view for RDS.
 *
 * <p>This view checks whether PlaceholderAPI is available and lets admins install the
 * {@code Player} expansion through PlaceholderAPI commands, then trigger a PlaceholderAPI reload.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class PlaceholderAPIView extends BaseView {

    private static final String ADMIN_COMMAND_PERMISSION = "raindropshops.command.admin";

    private final State<RDS> rds = initialState("plugin");

    /**
     * Creates the PlaceholderAPI admin integration view.
     */
    public PlaceholderAPIView() {
        super(PluginIntegrationManagementView.class);
    }

    /**
     * Returns the i18n key used by this view.
     *
     * @return view i18n key
     */
    @Override
    protected @NotNull String getKey() {
        return "placeholder_api_ui";
    }

    /**
     * Returns the menu layout for this view.
     *
     * @return rendered menu layout
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
                "         ",
                "    s    ",
                "    d    ",
                "    p    ",
                "         ",
                "         "
        };
    }

    /**
     * Renders PlaceholderAPI status and command controls.
     *
     * @param render render context for this menu
     * @param player player viewing the menu
     */
    @Override
    public void onFirstRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        if (!this.hasAdminAccess(player)) {
            render.slot(22).renderWith(() -> this.createLockedItem(player));
            return;
        }

        final boolean supported = this.isPlaceholderApiSupported();

        render.layoutSlot('s', this.createSummaryItem(player, supported));
        render.layoutSlot('d', this.createDetectionItem(player, supported));
        render.layoutSlot('p', this.createInstallButton(player, supported))
                .onClick(this::handleInstallPlayerExpansionClick);
    }

    /**
     * Cancels vanilla click handling so the menu behaves as an action UI.
     *
     * @param click click context for the current inventory interaction
     */
    @Override
    public void onClick(
            final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    private void handleInstallPlayerExpansionClick(
            final @NotNull SlotClickContext clickContext
    ) {
        clickContext.setCancelled(true);

        final Player player = clickContext.getPlayer();
        if (!this.hasAdminAccess(player)) {
            this.i18n("feedback.locked_message", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        if (!this.isPlaceholderApiSupported()) {
            this.i18n("feedback.unsupported_message", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final PlaceholderApiIntegrationSupport.ExecutionResult result =
                PlaceholderApiIntegrationSupport.installPlayerExpansionAndReload(
                        command -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
                );

        if (!result.expansionDownloadSucceeded()) {
            this.i18n("feedback.download_failed", player)
                    .withPlaceholder("expansion_name", PlaceholderApiIntegrationSupport.PLAYER_EXPANSION_NAME)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        if (!result.placeholderApiReloadSucceeded()) {
            this.i18n("feedback.reload_failed", player)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        this.i18n("feedback.completed", player)
                .withPlaceholder("expansion_name", PlaceholderApiIntegrationSupport.PLAYER_EXPANSION_NAME)
                .includePrefix()
                .build()
                .sendMessage();

        clickContext.openForPlayer(
                PlaceholderAPIView.class,
                Map.of("plugin", this.rds.get(clickContext))
        );
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player,
            final boolean supported
    ) {
        return UnifiedBuilderFactory.item(Material.BOOK)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n("summary.lore", player)
                        .withPlaceholders(Map.of(
                                "supported", supported,
                                "expansion_name", PlaceholderApiIntegrationSupport.PLAYER_EXPANSION_NAME
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createDetectionItem(
            final @NotNull Player player,
            final boolean supported
    ) {
        if (supported) {
            return UnifiedBuilderFactory.item(Material.LIME_DYE)
                    .setName(this.i18n("detection.supported.name", player).build().component())
                    .setLore(this.i18n("detection.supported.lore", player).build().children())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        }

        return UnifiedBuilderFactory.item(Material.RED_DYE)
                .setName(this.i18n("detection.missing.name", player).build().component())
                .setLore(this.i18n("detection.missing.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createInstallButton(
            final @NotNull Player player,
            final boolean supported
    ) {
        if (!supported) {
            return UnifiedBuilderFactory.item(Material.BARRIER)
                    .setName(this.i18n("action.unavailable.name", player).build().component())
                    .setLore(this.i18n("action.unavailable.lore", player).build().children())
                    .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                    .build();
        }

        return UnifiedBuilderFactory.item(Material.NAME_TAG)
                .setName(this.i18n("action.install.name", player)
                        .withPlaceholder("expansion_name", PlaceholderApiIntegrationSupport.PLAYER_EXPANSION_NAME)
                        .build()
                        .component())
                .setLore(this.i18n("action.install.lore", player)
                        .withPlaceholder("expansion_name", PlaceholderApiIntegrationSupport.PLAYER_EXPANSION_NAME)
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createLockedItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("feedback.locked.name", player).build().component())
                .setLore(this.i18n("feedback.locked.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private boolean hasAdminAccess(
            final @NotNull Player player
    ) {
        return player.isOp() || player.hasPermission(ADMIN_COMMAND_PERMISSION);
    }

    private boolean isPlaceholderApiSupported() {
        return PlaceholderApiIntegrationSupport.isPlaceholderApiSupported(Bukkit.getPluginManager());
    }
}
