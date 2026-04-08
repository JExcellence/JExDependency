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
 * Landing view for storage-related plugin integrations.
 *
 * <p>This view groups access to currency, skill, and job requirement integrations,
 * and exposes PlaceholderAPI management tools.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class PluginIntegrationManagementView extends BaseView {

    private static final String ADMIN_COMMAND_PERMISSION = "raindroprdr.command.admin";

    private final State<RDR> rdr = initialState("plugin");

    /**
     * Creates the plugin integration management view.
     */
    public PluginIntegrationManagementView() {
        super(StorageAdminView.class);
    }

    /**
     * Returns the translation namespace used by this view.
     *
     * @return plugin integration translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "plugin_integration_management_ui";
    }

    /**
     * Returns the menu layout for this view.
     *
     * @return rendered layout with integration controls
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            "  c k j  ",
            "    p    "
        };
    }

    /**
     * Renders the plugin integration controls.
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
        render.layoutSlot('c', this.createCurrencyButton(player))
            .onClick(clickContext -> clickContext.openForPlayer(
                AdminCurrencyView.class,
                Map.of("plugin", this.rdr.get(clickContext))
            ));
        render.layoutSlot('k', this.createSkillsButton(player))
            .onClick(clickContext -> clickContext.openForPlayer(
                StorageSkillsView.class,
                Map.of("plugin", this.rdr.get(clickContext))
            ));
        render.layoutSlot('j', this.createJobsButton(player))
            .onClick(clickContext -> clickContext.openForPlayer(
                StorageJobsView.class,
                Map.of("plugin", this.rdr.get(clickContext))
            ));
        render.layoutSlot('p', this.createPlaceholderApiButton(player))
            .onClick(clickContext -> clickContext.openForPlayer(
                PlaceholderAPIView.class,
                Map.of("plugin", this.rdr.get(clickContext))
            ));
    }

    /**
     * Cancels vanilla click handling so this menu behaves as an action UI.
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
        return UnifiedBuilderFactory.item(Material.COMPASS)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player).build().children())
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

    private @NotNull ItemStack createSkillsButton(
        final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.EXPERIENCE_BOTTLE)
            .setName(this.i18n("actions.skills.name", player).build().component())
            .setLore(this.i18n("actions.skills.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createJobsButton(
        final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.DIAMOND_PICKAXE)
            .setName(this.i18n("actions.jobs.name", player).build().component())
            .setLore(this.i18n("actions.jobs.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createPlaceholderApiButton(
        final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.NAME_TAG)
            .setName(this.i18n("actions.placeholder_api.name", player).build().component())
            .setLore(this.i18n("actions.placeholder_api.lore", player).build().children())
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
}
