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

package com.raindropcentral.core.view;

import com.raindropcentral.core.RCoreImpl;
import com.raindropcentral.core.service.central.DropletClaimService;
import com.raindropcentral.core.service.central.RCentralApiClient;
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
 * Paginated menu listing unclaimed droplet-store purchases.
 */
public final class DropletClaimsView extends APaginatedView<RCentralApiClient.DropletStorePurchaseData> {

    private final State<RCoreImpl> plugin = initialState("plugin");
    private final State<List<RCentralApiClient.DropletStorePurchaseData>> purchases = initialState("purchases");

    /**
     * Creates the claims view.
     */
    public DropletClaimsView() {
        super();
    }

    @Override
    protected @NotNull String getKey() {
        return "rc_claim_droplets_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
                "         ",
                " OOOOOOO ",
                " OOOOOOO ",
                " OOOOOOO ",
                "    S    ",
                "   <p>   "
        };
    }

    @Override
    protected @NotNull CompletableFuture<List<RCentralApiClient.DropletStorePurchaseData>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        final List<RCentralApiClient.DropletStorePurchaseData> purchaseData = this.purchases.get(context);
        return CompletableFuture.completedFuture(purchaseData == null ? List.of() : List.copyOf(purchaseData));
    }

    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull RCentralApiClient.DropletStorePurchaseData entry
    ) {
        final Player player = context.getPlayer();
        final DropletClaimService claimService = this.plugin.get(context).getDropletClaimService();
        final DropletClaimService.ClaimSupportStatus supportStatus = claimService.getSupportStatus(player, entry);

        final Material material = supportStatus == DropletClaimService.ClaimSupportStatus.SUPPORTED
                ? Material.COOKIE
                : Material.BARRIER;
        final String nameKey = switch (supportStatus) {
            case SUPPORTED -> "entry.supported.name";
            case ITEM_DISABLED -> "entry.item_disabled.name";
            case NO_SKILLS_AVAILABLE -> "entry.no_skills.name";
            case NO_JOBS_AVAILABLE -> "entry.no_jobs.name";
            case UNSUPPORTED_ITEM -> "entry.unsupported_item.name";
        };
        final String loreKey = switch (supportStatus) {
            case SUPPORTED -> "entry.supported.lore";
            case ITEM_DISABLED -> "entry.item_disabled.lore";
            case NO_SKILLS_AVAILABLE -> "entry.no_skills.lore";
            case NO_JOBS_AVAILABLE -> "entry.no_jobs.lore";
            case UNSUPPORTED_ITEM -> "entry.unsupported_item.lore";
        };

        builder.withItem(
                        UnifiedBuilderFactory.item(material)
                                .setName(this.i18n(nameKey, player)
                                        .withPlaceholders(Map.of(
                                                "item_name", entry.itemNameOrCode(),
                                                "item_code", entry.itemCodeOrBlank()
                                        ))
                                        .build()
                                        .component())
                                .setLore(this.i18n(loreKey, player)
                                        .withPlaceholders(Map.of(
                                                "item_name", entry.itemNameOrCode(),
                                                "item_code", entry.itemCodeOrBlank(),
                                                "amount_spent", entry.amountSpent()
                                        ))
                                        .build()
                                        .children())
                                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                                .build()
                )
                .onClick(clickContext -> this.handleEntryClick(clickContext, entry));
    }

    @Override
    protected void onPaginatedRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final List<RCentralApiClient.DropletStorePurchaseData> entries = this.purchases.get(render);
        final List<RCentralApiClient.DropletStorePurchaseData> source = entries == null ? List.of() : entries;
        final DropletClaimService claimService = this.plugin.get(render).getDropletClaimService();
        final long supportedCount = source.stream()
                .filter(entry -> claimService.getSupportStatus(player, entry) == DropletClaimService.ClaimSupportStatus.SUPPORTED)
                .count();

        render.slot(40).renderWith(() -> this.createSummaryItem(player, source.size(), supportedCount));
        if (source.isEmpty()) {
            render.slot(22).renderWith(() -> this.createEmptyItem(player));
        }
    }

    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private void handleEntryClick(
            final @NotNull SlotClickContext clickContext,
            final @NotNull RCentralApiClient.DropletStorePurchaseData entry
    ) {
        clickContext.setCancelled(true);
        this.plugin.get(clickContext).getDropletClaimService().handlePurchaseSelection(clickContext.getPlayer(), entry);
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player,
            final int itemCount,
            final long supportedCount
    ) {
        return UnifiedBuilderFactory.item(Material.NETHER_STAR)
                .setName(this.i18n("summary.name", player)
                        .withPlaceholders(Map.of("item_count", itemCount, "supported_count", supportedCount))
                        .build()
                        .component())
                .setLore(this.i18n("summary.lore", player)
                        .withPlaceholders(Map.of("item_count", itemCount, "supported_count", supportedCount))
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
