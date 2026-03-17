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

import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import me.devnatan.inventoryframework.View;
import me.devnatan.inventoryframework.component.Pagination;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.RenderContext;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Renders the full paginated shop directory previously exposed by the shop search command.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopListView extends AbstractShopBrowserView {

    /**
     * Creates the paginated all-shops directory view.
     */
    public ShopListView() {
        super(ShopSearchView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "shop_list_ui";
    }

    @Override
    protected @NotNull CompletableFuture<List<ShopBrowserSupport.ShopBrowserEntry>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        final var plugin = this.getPlugin(context);
        return CompletableFuture.supplyAsync(
                () -> ShopBrowserSupport.loadEntries(plugin, null),
                plugin.getExecutor()
        );
    }

    @Override
    protected @NotNull ItemStack createEntryItem(
            final @NotNull Context context,
            final @NotNull Player player,
            final @NotNull ShopBrowserSupport.ShopBrowserEntry entry
    ) {
        return UnifiedBuilderFactory.item(Material.CHEST)
                .setName(this.i18n("entry.name", player)
                        .withPlaceholders(Map.of(
                                "owner", ShopBrowserSupport.getOwnerName(entry.ownerId())
                        ))
                        .build()
                        .component())
                .setLore(this.i18n("entry.lore", player)
                        .withPlaceholders(Map.of(
                                "owner", ShopBrowserSupport.getOwnerName(entry.ownerId()),
                                "location", ShopBrowserSupport.formatLocation(entry.shopLocation()),
                                "available_count", entry.availableItemCount()
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    @Override
    protected @NotNull ItemStack createHeaderItem(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final Pagination pagination = this.getPagination(render);
        final List<ShopBrowserSupport.ShopBrowserEntry> entries = this.getEntries(pagination);

        if (pagination.source() != null && entries.isEmpty()) {
            return this.createEmptyItem(player);
        }

        int totalAvailableItems = 0;
        for (final ShopBrowserSupport.ShopBrowserEntry entry : entries) {
            totalAvailableItems += entry.availableItemCount();
        }

        return UnifiedBuilderFactory.item(Material.COMPASS)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n("summary.lore", player)
                        .withPlaceholders(Map.of(
                                "shop_count", entries.size(),
                                "available_count", totalAvailableItems
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    @Override
    protected @NotNull Map<String, Object> createViewData(
            final @NotNull Context context
    ) {
        return Map.of("plugin", this.getPlugin(context));
    }

    @Override
    protected @NotNull Class<? extends View> getCurrentViewClass() {
        return ShopListView.class;
    }
}
