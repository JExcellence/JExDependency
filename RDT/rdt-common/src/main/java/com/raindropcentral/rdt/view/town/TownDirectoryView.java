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

package com.raindropcentral.rdt.view.town;

import com.raindropcentral.rdt.RDT;
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.utils.TownOverviewAccessMode;
import com.raindropcentral.rdt.view.main.TownHubView;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated directory of every persisted town on the server.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class TownDirectoryView extends APaginatedView<RTown> {

    private final State<RDT> plugin = initialState("plugin");

    /**
     * Creates the paginated town directory.
     */
    public TownDirectoryView() {
        super(TownHubView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_directory_ui";
    }

    /**
     * Loads the directory entries for pagination.
     *
     * @param context current view context
     * @return async town list
     */
    @Override
    protected @NotNull CompletableFuture<List<RTown>> getAsyncPaginationSource(final @NotNull Context context) {
        final RDT rdt = this.plugin.get(context);
        return CompletableFuture.completedFuture(rdt.getTownRuntimeService() == null ? List.of() : rdt.getTownRuntimeService().getTowns());
    }

    /**
     * Renders one town directory card.
     *
     * @param context current context
     * @param builder item builder
     * @param index entry index
     * @param entry town entry
     */
    @Override
    protected void renderEntry(
        final @NotNull Context context,
        final @NotNull BukkitItemComponentBuilder builder,
        final int index,
        final @NotNull RTown entry
    ) {
        builder.withItem(this.createTownItem(context.getPlayer(), entry))
            .onClick(clickContext -> clickContext.openForPlayer(
                TownOverviewView.class,
                Map.of(
                    "plugin", this.plugin.get(clickContext),
                    "town_uuid", entry.getTownUUID(),
                    "access_mode", TownOverviewAccessMode.REMOTE
                )
            ));
    }

    /**
     * Renders the directory summary item.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    protected void onPaginatedRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final int townCount = this.getPagination(render).source() == null ? 0 : this.getPagination(render).source().size();
        render.slot(4).renderWith(() -> this.createSummaryItem(player, townCount));
        if (townCount < 1) {
            render.slot(22).renderWith(() -> this.createEmptyItem(player));
        }
    }

    /**
     * Cancels default inventory interaction for the menu.
     *
     * @param click click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private @NotNull ItemStack createSummaryItem(final @NotNull Player player, final int townCount) {
        return UnifiedBuilderFactory.item(Material.MAP)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholder("town_count", townCount)
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createTownItem(final @NotNull Player player, final @NotNull RTown town) {
        return UnifiedBuilderFactory.item(Material.PAPER)
            .setName(this.i18n("entry.name", player)
                .withPlaceholder("town_name", town.getTownName())
                .build()
                .component())
            .setLore(this.i18n("entry.lore", player)
                .withPlaceholders(Map.of(
                    "town_name", town.getTownName(),
                    "member_count", town.getMembers().size(),
                    "chunk_count", town.getChunks().size(),
                    "town_level", town.getTownLevel(),
                    "archetype", town.getArchetype() == null ? "unassigned" : town.getArchetype().getDisplayName()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createEmptyItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("empty.name", player).build().component())
            .setLore(this.i18n("empty.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
}
