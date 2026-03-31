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
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.APaginatedView;
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Paginated admin shop-control browser.
 *
 * <p>Depending on mode, this browser either opens shops in owner-override mode or force-closes
 * selected shops.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopAdminShopControlView extends APaginatedView<ShopAdminShopControlView.ShopControlEntry> {

    private static final String ADMIN_COMMAND_PERMISSION = "raindropshops.command.admin";

    private final State<RDS> rds = initialState("plugin");
    private final State<String> actionMode = initialState("actionMode");

    /**
     * Creates the shop-control browser view.
     */
    public ShopAdminShopControlView() {
        super(ShopAdminPlayerView.class);
    }

    /**
     * Returns the i18n key used by this view.
     *
     * @return view i18n key
     */
    @Override
    protected @NotNull String getKey() {
        return "shop_admin_shop_control_ui";
    }

    /**
     * Returns title placeholders.
     *
     * @param context open context
     * @return placeholder map
     */
    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(
            final @NotNull OpenContext context
    ) {
        final String modeLabelKey = this.resolveMode(context) == ShopAdminShopControlMode.FORCE_CLOSE_SHOP
                ? "mode.force_close"
                : "mode.open_as_owner";
        return Map.of(
                "action_mode", this.i18n(modeLabelKey, context.getPlayer()).build().getI18nVersionWrapper().asPlaceholder()
        );
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
                " OOOOOOO ",
                " OOOOOOO ",
                " OOOOOOO ",
                " OOOOOOO ",
                "  < p >  "
        };
    }

    /**
     * Loads all shops for pagination.
     *
     * @param context current context
     * @return async shop list
     */
    @Override
    protected @NotNull CompletableFuture<List<ShopControlEntry>> getAsyncPaginationSource(
            final @NotNull Context context
    ) {
        if (!this.hasAdminAccess(context.getPlayer())) {
            return CompletableFuture.completedFuture(List.of());
        }

        final List<ShopControlEntry> entries = new ArrayList<>();
        for (final Shop shop : this.rds.get(context).getShopRepository().findAllShops()) {
            final Location location = shop.getShopLocation();
            if (location == null) {
                continue;
            }

            entries.add(new ShopControlEntry(
                    location,
                    shop.getOwner(),
                    shop.isAdminShop(),
                    shop.getStoredItemCount(),
                    shop.getBankCurrencyCount()
            ));
        }

        entries.sort(Comparator.comparing(entry -> this.formatLocation(entry.shopLocation())));
        return CompletableFuture.completedFuture(entries);
    }

    /**
     * Renders one shop control entry.
     *
     * @param context current context
     * @param builder slot builder
     * @param index zero-based index
     * @param entry entry payload
     */
    @Override
    protected void renderEntry(
            final @NotNull Context context,
            final @NotNull BukkitItemComponentBuilder builder,
            final int index,
            final @NotNull ShopControlEntry entry
    ) {
        builder.withItem(this.createEntryItem(context, entry))
                .onClick(clickContext -> this.handleShopClick(clickContext, entry));
    }

    /**
     * Renders summary/empty cards.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    protected void onPaginatedRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        if (!this.hasAdminAccess(player)) {
            render.slot(22).renderWith(() -> this.createLockedItem(player));
            return;
        }

        final int shopCount = this.getPagination(render).source() == null
                ? 0
                : this.getPagination(render).source().size();
        render.layoutSlot('s', this.createSummaryItem(player, shopCount, this.resolveMode(render)));
        if (shopCount < 1) {
            render.slot(22).renderWith(() -> this.createEmptyItem(player));
        }
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

    private void handleShopClick(
            final @NotNull SlotClickContext clickContext,
            final @NotNull ShopControlEntry entry
    ) {
        clickContext.setCancelled(true);

        if (!this.hasAdminAccess(clickContext.getPlayer())) {
            this.i18n("feedback.access_denied_message", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RDS plugin = this.rds.get(clickContext);
        final Shop liveShop = plugin.getShopRepository().findByLocation(entry.shopLocation());
        if (liveShop == null) {
            this.i18n("feedback.shop_missing_message", clickContext.getPlayer())
                    .withPlaceholder("location", this.formatLocation(entry.shopLocation()))
                    .includePrefix()
                    .build()
                    .sendMessage();
            this.openFreshView(clickContext);
            return;
        }

        final ShopAdminShopControlMode mode = this.resolveMode(clickContext);
        if (mode == ShopAdminShopControlMode.OPEN_AS_OWNER) {
            clickContext.openForPlayer(
                    ShopOverviewView.class,
                    Map.of(
                            "plugin", plugin,
                            "shopLocation", liveShop.getShopLocation(),
                            ShopAdminAccessSupport.ADMIN_OWNER_OVERRIDE_KEY, true
                    )
            );
            return;
        }

        final String ownerName = this.getOwnerName(liveShop.getOwner());
        final String locationLabel = this.formatLocation(liveShop.getShopLocation());
        final int closedViews = ShopAdminForceCloseSupport.forceCloseShop(plugin, liveShop);

        this.i18n("feedback.force_closed", clickContext.getPlayer())
                .withPlaceholders(Map.of(
                        "owner", ownerName,
                        "location", locationLabel,
                        "closed_views", closedViews
                ))
                .includePrefix()
                .build()
                .sendMessage();

        final Player ownerPlayer = Bukkit.getPlayer(liveShop.getOwner());
        if (ownerPlayer != null && !ownerPlayer.getUniqueId().equals(clickContext.getPlayer().getUniqueId())) {
            this.i18n("feedback.force_closed_owner", ownerPlayer)
                    .withPlaceholders(Map.of(
                            "location", locationLabel,
                            "admin", clickContext.getPlayer().getName()
                    ))
                    .includePrefix()
                    .build()
                    .sendMessage();
        }

        this.openFreshView(clickContext);
    }

    private void openFreshView(
            final @NotNull Context context
    ) {
        context.openForPlayer(
                ShopAdminShopControlView.class,
                Map.of(
                        "plugin", this.rds.get(context),
                        "actionMode", this.resolveMode(context).name()
                )
        );
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player,
            final int shopCount,
            final @NotNull ShopAdminShopControlMode mode
    ) {
        final String modeLabel = this.i18n(
                        mode == ShopAdminShopControlMode.FORCE_CLOSE_SHOP ? "mode.force_close" : "mode.open_as_owner",
                        player
                )
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();

        return UnifiedBuilderFactory.item(Material.BOOK)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n("summary.lore", player)
                        .withPlaceholders(Map.of(
                                "shop_count", shopCount,
                                "action_mode", modeLabel
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createEntryItem(
            final @NotNull Context context,
            final @NotNull ShopControlEntry entry
    ) {
        final Player player = context.getPlayer();
        final ShopAdminShopControlMode mode = this.resolveMode(context);
        final String actionLabel = this.i18n(
                        mode == ShopAdminShopControlMode.FORCE_CLOSE_SHOP ? "entry.action.force_close" : "entry.action.open_owner",
                        player
                )
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
        final String shopTypeLabel = this.i18n(
                        entry.adminShop() ? "entry.type.admin" : "entry.type.player",
                        player
                )
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();

        return UnifiedBuilderFactory.item(mode == ShopAdminShopControlMode.FORCE_CLOSE_SHOP ? Material.BARRIER : Material.SPYGLASS)
                .setName(this.i18n("entry.name", player)
                        .withPlaceholders(Map.of(
                                "owner", this.getOwnerName(entry.ownerId()),
                                "location", this.formatLocation(entry.shopLocation())
                        ))
                        .build()
                        .component())
                .setLore(this.i18n("entry.lore", player)
                        .withPlaceholders(Map.of(
                                "owner", this.getOwnerName(entry.ownerId()),
                                "location", this.formatLocation(entry.shopLocation()),
                                "shop_type", shopTypeLabel,
                                "stored_count", entry.storedItems(),
                                "bank_count", entry.bankCurrencies(),
                                "action", actionLabel
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createEmptyItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("feedback.empty.name", player).build().component())
                .setLore(this.i18n("feedback.empty.lore", player).build().children())
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

    private @NotNull ShopAdminShopControlMode resolveMode(
            final @NotNull Context context
    ) {
        final String rawMode = this.actionMode.get(context);
        return rawMode == null
                ? ShopAdminShopControlMode.OPEN_AS_OWNER
                : ShopAdminShopControlMode.fromRaw(rawMode);
    }

    private @NotNull String getOwnerName(
            final @NotNull UUID ownerId
    ) {
        final String ownerName = Bukkit.getOfflinePlayer(ownerId).getName();
        return ownerName == null || ownerName.isBlank() ? ownerId.toString() : ownerName;
    }

    private @NotNull String formatLocation(
            final @NotNull Location location
    ) {
        final String worldName = location.getWorld() == null ? "unknown_world" : location.getWorld().getName();
        return worldName
                + " ("
                + location.getBlockX() + ", "
                + location.getBlockY() + ", "
                + location.getBlockZ() + ")";
    }

    private boolean hasAdminAccess(
            final @NotNull Player player
    ) {
        return player.isOp() || player.hasPermission(ADMIN_COMMAND_PERMISSION);
    }

    /**
     * Immutable shop-control list entry.
     *
     * @param shopLocation primary shop location
     * @param ownerId owner UUID
     * @param adminShop whether the shop is an admin shop
     * @param storedItems total stored entries
     * @param bankCurrencies tracked bank currency count
     */
    protected record ShopControlEntry(
            @NotNull Location shopLocation,
            @NotNull UUID ownerId,
            boolean adminShop,
            int storedItems,
            int bankCurrencies
    ) {
    }
}
