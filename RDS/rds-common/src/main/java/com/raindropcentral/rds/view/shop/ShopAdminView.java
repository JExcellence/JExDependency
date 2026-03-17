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

import com.raindropcentral.rds.RDS;
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

/**
 * Renders the RDS admin landing view.
 *
 * <p>This view centralizes server-bank, config, tax, and integration-management controls
 * for privileged shop administration.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopAdminView extends BaseView {

    private static final String ADMIN_COMMAND_PERMISSION = "raindropshops.command.admin";

    private final State<RDS> rds = initialState("plugin");

    /**
     * Creates the admin landing view.
     */
    public ShopAdminView() {
        super();
    }

    /**
     * Returns the i18n key used by this view.
     *
     * @return view i18n key
     */
    @Override
    protected @NotNull String getKey() {
        return "shop_admin_ui";
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
                "  b c u  ",
                "  p i t  ",
                "         ",
                "         "
        };
    }

    /**
     * Renders the admin landing controls.
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

        render.layoutSlot('s', this.createSummaryItem(player));

        render.layoutSlot('b', this.createServerBankButton(player))
                .onClick(clickContext -> clickContext.openForPlayer(
                        ServerBankView.class,
                        java.util.Map.of("plugin", this.rds.get(clickContext))
                ));

        render.layoutSlot('c', this.createConfigButton(player))
                .onClick(clickContext -> clickContext.openForPlayer(
                        ShopConfigView.class,
                        java.util.Map.of("plugin", this.rds.get(clickContext))
                ));

        render.layoutSlot('u', this.createCurrencyButton(player))
                .onClick(clickContext -> clickContext.openForPlayer(
                        AdminCurrencyView.class,
                        java.util.Map.of("plugin", this.rds.get(clickContext))
                ));

        render.layoutSlot('p', this.createPlayerAdminButton(player))
                .onClick(clickContext -> clickContext.openForPlayer(
                        ShopAdminPlayerView.class,
                        java.util.Map.of("plugin", this.rds.get(clickContext))
                ));

        render.layoutSlot('i', this.createIntegrationsButton(player))
                .onClick(clickContext -> clickContext.openForPlayer(
                        PluginIntegrationManagementView.class,
                        java.util.Map.of("plugin", this.rds.get(clickContext))
                ));

        render.layoutSlot('t', this.createTaxRunButton(player))
                .onClick(this::handleTaxRunClick);
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

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.COMMAND_BLOCK)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n("summary.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createServerBankButton(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.GOLD_BLOCK)
                .setName(this.i18n("actions.server_bank.name", player).build().component())
                .setLore(this.i18n("actions.server_bank.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createConfigButton(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
                .setName(this.i18n("actions.config.name", player).build().component())
                .setLore(this.i18n("actions.config.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createCurrencyButton(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.PRISMARINE_CRYSTALS)
                .setName(this.i18n("actions.currency.name", player).build().component())
                .setLore(this.i18n("actions.currency.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createIntegrationsButton(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.ENDER_CHEST)
                .setName(this.i18n("actions.integrations.name", player).build().component())
                .setLore(this.i18n("actions.integrations.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createPlayerAdminButton(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.PLAYER_HEAD)
                .setName(this.i18n("actions.players.name", player).build().component())
                .setLore(this.i18n("actions.players.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createTaxRunButton(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.CLOCK)
                .setName(this.i18n("actions.tax_run.name", player).build().component())
                .setLore(this.i18n("actions.tax_run.lore", player).build().children())
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

    private void handleTaxRunClick(
            final @NotNull SlotClickContext clickContext
    ) {
        clickContext.setCancelled(true);

        final RDS plugin = this.rds.get(clickContext);
        final var taxScheduler = plugin.getShopTaxScheduler();
        if (taxScheduler == null || plugin.getScheduler() == null) {
            this.i18n("feedback.tax_run_unavailable", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        plugin.getScheduler().runSync(taxScheduler::collectTaxesNow);
        this.i18n("feedback.tax_run_triggered", clickContext.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
    }
}
