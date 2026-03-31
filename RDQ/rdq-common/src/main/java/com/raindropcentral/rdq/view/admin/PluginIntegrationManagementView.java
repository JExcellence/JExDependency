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

package com.raindropcentral.rdq.view.admin;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Landing view for RDQ plugin integration controls.
 *
 * <p>This view groups access to currency, skill, and job plugin integration dashboards under
 * the admin area so related tooling is managed from one location.</p>
 *
 * @author ItsRainingHP
 * @since 6.0.0
 * @version 6.0.0
 */
public class PluginIntegrationManagementView extends BaseView {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");

    private final State<RDQ> rdq = initialState("plugin");

    /**
     * Creates the integration management landing view.
     */
    public PluginIntegrationManagementView() {
        super(AdminOverviewView.class);
    }

    /**
     * Returns the translation key used by this view.
     *
     * @return i18n key root
     */
    @Override
    protected @NotNull String getKey() {
        return "plugin_integration_management_ui";
    }

    /**
     * Returns the inventory size for this view.
     *
     * @return inventory row count
     */
    @Override
    protected int getSize() {
        return 3;
    }

    /**
     * Renders summary and integration action buttons.
     *
     * @param render render context for this view
     * @param player player viewing the interface
     */
    @Override
    public void onFirstRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        render.slot(1, 5).withItem(this.createSummaryItem(player));
        this.initializeCurrencyViewButton(render, player);
        this.initializeSkillsViewButton(render, player);
        this.initializeJobsViewButton(render, player);
        this.initializePlaceholderApiViewButton(render, player);
    }

    private void initializeCurrencyViewButton(
        final @NotNull RenderContext context,
        final @NotNull Player player
    ) {
        context.slot(2, 4)
            .withItem(this.createCurrencyButton(player))
            .onClick(clickContext -> {
                try {
                    clickContext.openForPlayer(
                        AdminCurrencyView.class,
                        Map.of("plugin", this.rdq.get(clickContext))
                    );
                } catch (final Exception exception) {
                    LOGGER.log(
                        Level.SEVERE,
                        "Failed to open currency integration view for player: " + player.getName(),
                        exception
                    );
                    this.i18n("actions.currency.error", player)
                        .includePrefix()
                        .build()
                        .sendMessage();
                }
            });
    }

    private void initializeSkillsViewButton(
        final @NotNull RenderContext context,
        final @NotNull Player player
    ) {
        context.slot(2, 5)
            .withItem(this.createSkillsButton(player))
            .onClick(clickContext -> {
                try {
                    clickContext.openForPlayer(
                        AdminSkillsView.class,
                        Map.of("plugin", this.rdq.get(clickContext))
                    );
                } catch (final Exception exception) {
                    LOGGER.log(
                        Level.SEVERE,
                        "Failed to open skills integration view for player: " + player.getName(),
                        exception
                    );
                    this.i18n("actions.skills.error", player)
                        .includePrefix()
                        .build()
                        .sendMessage();
                }
            });
    }

    private void initializeJobsViewButton(
        final @NotNull RenderContext context,
        final @NotNull Player player
    ) {
        context.slot(2, 6)
            .withItem(this.createJobsButton(player))
            .onClick(clickContext -> {
                try {
                    clickContext.openForPlayer(
                        AdminJobsView.class,
                        Map.of("plugin", this.rdq.get(clickContext))
                    );
                } catch (final Exception exception) {
                    LOGGER.log(
                        Level.SEVERE,
                        "Failed to open jobs integration view for player: " + player.getName(),
                        exception
                    );
                    this.i18n("actions.jobs.error", player)
                        .includePrefix()
                        .build()
                        .sendMessage();
                }
            });
    }

    private void initializePlaceholderApiViewButton(
        final @NotNull RenderContext context,
        final @NotNull Player player
    ) {
        context.slot(3, 5)
            .withItem(this.createPlaceholderApiButton(player))
            .onClick(clickContext -> {
                try {
                    clickContext.openForPlayer(
                        PlaceholderAPIView.class,
                        Map.of("plugin", this.rdq.get(clickContext))
                    );
                } catch (final Exception exception) {
                    LOGGER.log(
                        Level.SEVERE,
                        "Failed to open PlaceholderAPI integration view for player: " + player.getName(),
                        exception
                    );
                    this.i18n("actions.placeholder_api.error", player)
                        .includePrefix()
                        .build()
                        .sendMessage();
                }
            });
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
}
