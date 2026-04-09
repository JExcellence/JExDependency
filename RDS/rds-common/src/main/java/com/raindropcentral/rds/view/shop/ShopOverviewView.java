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
import com.raindropcentral.rds.database.entity.RDSPlayer;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.database.entity.ShopLedgerType;
import com.raindropcentral.rds.items.ShopBlock;
import com.raindropcentral.rds.items.TownShopBlock;
import com.raindropcentral.rds.service.shop.ShopOwnershipSupport;
import com.raindropcentral.rds.service.tax.ShopTaxScheduler;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders the shop overview inventory view.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopOverviewView extends BaseView {

    private static final String ADMIN_SHOPS_PERMISSION = "raindropshops.admin.shops";
    private static final int PAY_TAXES_SLOT = 14;

    private final State<RDS> rds = initialState("plugin");
    private final State<Location> shopLocation = initialState("shopLocation");

    @Override
    protected String getKey() {
        return "shop_overview_ui";
    }

    @Override
    protected int getSize() {
        return 2;
    }

    @Override
    protected boolean shouldAutoFill() {
        return false;
    }

    @Override
    protected Map<String, Object> getTitlePlaceholders(
            final @NotNull OpenContext context
    ) {
        final Shop shop = this.getCurrentShop(context);
        final Location location = this.shopLocation.get(context);

        return Map.of(
                "location", formatLocation(location),
                "owner", shop == null ? "Unknown" : getOwnerName(shop)
        );
    }

    /**
     * Executes onFirstRender.
     */
    @Override
    public void onFirstRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final Shop shop = this.getCurrentShop(render);
        final boolean canClose = shop != null && this.canClose(render, shop);
        final boolean canSupply = shop != null && this.canSupply(render, shop);
        final boolean canManage = shop != null && this.canManage(render, shop);
        final boolean hasTaxDebt = shop != null && shop.hasTaxDebt();

        if (shop == null) {
            render.slot(4).renderWith(() -> createMissingShopItem(player));
            return;
        }

        render.slot(2).renderWith(() -> createSummaryItem(shop, player));
        render.slot(3)
                .renderWith(() -> createCustomerViewItem(shop, player))
                .onClick(clickContext -> {
                    final Shop currentShop = this.getCurrentShop(clickContext);
                    if (currentShop == null) {
                        this.i18n("feedback.shop_missing.message", clickContext.getPlayer())
                                .includePrefix()
                                .build()
                                .sendMessage();
                        return;
                    }

                    clickContext.openForPlayer(
                            ShopCustomerView.class,
                            this.createShopViewData(clickContext, currentShop)
                    );
                });
        if (canClose) {
            render.slot(1)
                    .renderWith(() -> createCloseShopItem(shop, player))
                    .onClick(this::handleCloseShopClick);

            if (shop.isPlayerShop()) {
            render.slot(13)
                    .renderWith(() -> createTrustedPlayersItem(shop, player))
                    .onClick(clickContext -> clickContext.openForPlayer(
                            ShopTrustedView.class,
                            Map.of(
                                    "plugin", this.rds.get(clickContext),
                                    "shopLocation", shop.getShopLocation()
                            )
                    ));
            }
        }
        if (canClose && shop.isPlayerShop() && player.hasPermission(ADMIN_SHOPS_PERMISSION)) {
            render.slot(0)
                    .renderWith(() -> createAdminToggleItem(shop, player))
                    .onClick(clickContext -> {
                        final Shop currentShop = this.getCurrentShop(clickContext);
                        if (currentShop == null) {
                            this.i18n("feedback.shop_missing.message", clickContext.getPlayer())
                                    .includePrefix()
                                    .build()
                                    .sendMessage();
                            return;
                        }

                        if (!ShopAdminAccessSupport.canActAsOwner(clickContext, currentShop, this.rds.get(clickContext))) {
                            this.i18n("feedback.not_owner", clickContext.getPlayer())
                                    .includePrefix()
                                    .build()
                                    .sendMessage();
                            return;
                        }

                        if (!clickContext.getPlayer().hasPermission(ADMIN_SHOPS_PERMISSION)) {
                            this.i18n("feedback.no_admin_permission", clickContext.getPlayer())
                                    .includePrefix()
                                    .build()
                                    .sendMessage();
                            return;
                        }

                        final RDS plugin = this.rds.get(clickContext);
                        final RDSPlayer playerData = this.getOrCreatePlayer(plugin, currentShop.getOwner());
                        final boolean enablingAdmin = !currentShop.isAdminShop();

                        if (enablingAdmin) {
                            final int maxAdminShops = plugin.getMaximumAdminShops();
                            final int currentAdminShops = ShopOwnershipSupport.countAdminShops(plugin);
                            if (maxAdminShops > 0 && currentAdminShops >= maxAdminShops) {
                                this.i18n("feedback.admin_limit_reached", clickContext.getPlayer())
                                        .withPlaceholders(Map.of(
                                                "admin_shops", currentAdminShops,
                                                "max_admin_shops", maxAdminShops
                                        ))
                                        .includePrefix()
                                        .build()
                                        .sendMessage();
                                return;
                            }
                        } else {
                            final int maxShops = plugin.getMaximumShops(
                                    currentShop.getOwner(),
                                    plugin.getDefaultConfig()
                            );
                            final int activeOwnedShops = ShopOwnershipSupport.countOwnedPlayerShops(plugin, currentShop.getOwner());
                            if (maxShops > 0 && activeOwnedShops >= maxShops) {
                                this.i18n("feedback.admin_disable_limit_reached", clickContext.getPlayer())
                                        .withPlaceholders(Map.of(
                                                "owned_shops", activeOwnedShops,
                                                "max_shops", maxShops
                                        ))
                                        .includePrefix()
                                        .build()
                                        .sendMessage();
                                return;
                            }
                        }

                        currentShop.setAdminShop(enablingAdmin);
                        plugin.getShopRepository().update(currentShop);
                        plugin.getPlayerRepository().update(playerData);
                        final int activeOwnedShops = ShopOwnershipSupport.countOwnedPlayerShops(plugin, currentShop.getOwner());

                        final String feedbackKey = currentShop.isAdminShop()
                                ? "feedback.admin_enabled"
                                : "feedback.admin_disabled";
                        this.i18n(feedbackKey, clickContext.getPlayer())
                                .withPlaceholder("owned_shops", activeOwnedShops)
                                .includePrefix()
                                .build()
                                .sendMessage();

                        clickContext.openForPlayer(
                                ShopOverviewView.class,
                                this.createShopViewData(clickContext, currentShop)
                        );
                    });
        }
        if (canManage) {
            render.slot(4)
                    .renderWith(() -> createFinanceItem(shop, player))
                    .onClick(clickContext -> {
                        final Shop currentShop = this.getCurrentShop(clickContext);
                        if (currentShop == null) {
                            return;
                        }

                        if (!this.canManage(clickContext, currentShop)) {
                            this.i18n("feedback.not_owner", clickContext.getPlayer())
                                    .includePrefix()
                                    .build()
                                    .sendMessage();
                            return;
                        }

                        clickContext.openForPlayer(
                                ShopBankView.class,
                                this.createShopViewData(clickContext, currentShop)
                        );
                    });

            render.slot(5)
                    .renderWith(() -> createLedgerItem(shop, player))
                    .onClick(clickContext -> {
                        final Shop currentShop = this.getCurrentShop(clickContext);
                        if (currentShop == null) {
                            return;
                        }

                        if (!this.canManage(clickContext, currentShop)) {
                            this.i18n("feedback.not_owner", clickContext.getPlayer())
                                    .includePrefix()
                                    .build()
                                    .sendMessage();
                            return;
                        }

                        clickContext.openForPlayer(
                                ShopLedgerView.class,
                                this.createShopViewData(clickContext, currentShop)
                        );
                    });
        }
        if (canSupply) {
            if (hasTaxDebt) {
                render.slot(6)
                        .renderWith(() -> this.createTaxLockedManageItem(shop, player, this.rds.get(render)))
                        .onClick(clickContext -> this.i18n("feedback.tax_locked", clickContext.getPlayer())
                                .withPlaceholder("debt_summary", this.formatDebtSummary(this.getCurrentShop(clickContext), this.rds.get(clickContext)))
                                .includePrefix()
                                .build()
                                .sendMessage());
            } else {
                render.slot(6)
                        .renderWith(() -> createManageItem(shop, player))
                        .onClick(clickContext -> {
                            final Shop currentShop = this.getCurrentShop(clickContext);
                            if (currentShop == null || !this.canSupply(clickContext, currentShop)) {
                                this.i18n("feedback.not_owner", clickContext.getPlayer())
                                        .includePrefix()
                                        .build()
                                        .sendMessage();
                                return;
                            }

                            if (currentShop.hasTaxDebt()) {
                                this.i18n("feedback.tax_locked", clickContext.getPlayer())
                                        .withPlaceholder("debt_summary", this.formatDebtSummary(currentShop, this.rds.get(clickContext)))
                                        .includePrefix()
                                        .build()
                                        .sendMessage();
                                return;
                            }

                            final Map<String, Object> viewData = this.createShopViewData(clickContext, currentShop);
                            viewData.put("shop", currentShop);
                            clickContext.openForPlayer(ShopInputView.class, viewData);
                        });
            }
        }

        if (canManage) {
            if (hasTaxDebt) {
                render.slot(7)
                        .renderWith(() -> this.createTaxLockedEditItem(shop, player, this.rds.get(render)))
                        .onClick(clickContext -> this.i18n("feedback.tax_locked", clickContext.getPlayer())
                                .withPlaceholder("debt_summary", this.formatDebtSummary(this.getCurrentShop(clickContext), this.rds.get(clickContext)))
                                .includePrefix()
                                .build()
                                .sendMessage());
            } else {
                render.slot(7)
                        .renderWith(() -> createEditItem(shop, player))
                        .onClick(clickContext -> {
                            final Shop currentShop = this.getCurrentShop(clickContext);
                            if (currentShop == null) {
                                return;
                            }

                            clickContext.openForPlayer(
                                    ShopEditView.class,
                                    this.createShopViewData(clickContext, currentShop)
                            );
                        });
            }

            render.slot(8)
                    .renderWith(() -> createStorageItem(shop, player))
                    .onClick(clickContext -> {
                        final Shop currentShop = this.getCurrentShop(clickContext);
                        if (currentShop == null) {
                            return;
                        }

                        clickContext.openForPlayer(
                                ShopStorageView.class,
                                this.createShopViewData(clickContext, currentShop)
                        );
                    });
            if (hasTaxDebt) {
                render.slot(PAY_TAXES_SLOT)
                        .renderWith(() -> this.createPayTaxesItem(shop, player, this.rds.get(render)))
                        .onClick(this::handlePayTaxesClick);
            }
        }
    }

    /**
     * Executes onClick.
     */
    @Override
    public void onClick(
            final @NotNull SlotClickContext click
    ) {
        click.setCancelled(true);
    }

    private Shop getCurrentShop(final @NotNull Context context) {
        return this.rds.get(context).getShopRepository().findByLocation(this.shopLocation.get(context));
    }

    private @NotNull RDSPlayer getOrCreatePlayer(
            final @NotNull RDS plugin,
            final @NotNull java.util.UUID playerId
    ) {
        final RDSPlayer existingPlayer = plugin.getPlayerRepository().findByPlayer(playerId);
        if (existingPlayer != null) {
            return existingPlayer;
        }

        final RDSPlayer newPlayer = new RDSPlayer(playerId);
        plugin.getPlayerRepository().create(newPlayer);
        return newPlayer;
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Shop shop,
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.CHEST)
                .setName(this.i18n("summary.name", player).build().component())
                .setLore(this.i18n(this.getSummaryLoreSuffix(shop), player)
                        .withPlaceholders(Map.of(
                                "owner", getOwnerName(shop),
                                "location", formatLocation(shop.getShopLocation()),
                                "item_count", shop.getStoredItemCount()
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createAdminToggleItem(
            final @NotNull Shop shop,
            final @NotNull Player player
    ) {
        final String suffix = shop.isAdminShop()
                ? "actions.admin_toggle.enabled"
                : "actions.admin_toggle.disabled";
        final Material material = shop.isAdminShop()
                ? Material.COMMAND_BLOCK
                : Material.LEVER;

        return UnifiedBuilderFactory.item(material)
                .setName(this.i18n(suffix + ".name", player).build().component())
                .setLore(this.i18n(suffix + ".lore", player)
                        .withPlaceholder("item_count", shop.getStoredItemCount())
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createCustomerViewItem(
            final @NotNull Shop shop,
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.SPYGLASS)
                .setName(this.i18n("actions.customer_view.name", player).build().component())
                .setLore(this.i18n("actions.customer_view.lore", player)
                        .withPlaceholder("item_count", shop.getStoredItemCount())
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createTrustedPlayersItem(
            final @NotNull Shop shop,
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.PLAYER_HEAD)
                .setName(this.i18n("actions.trusted.name", player).build().component())
                .setLore(this.i18n("actions.trusted.lore", player)
                        .withPlaceholders(Map.of(
                                "associate_count", shop.getTrustedPlayerCount(com.raindropcentral.rds.database.entity.ShopTrustStatus.ASSOCIATE),
                                "trusted_count", shop.getTrustedPlayerCount(com.raindropcentral.rds.database.entity.ShopTrustStatus.TRUSTED)
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private void handleCloseShopClick(
            final @NotNull SlotClickContext clickContext
    ) {
        final Shop currentShop = this.getCurrentShop(clickContext);
        if (currentShop == null) {
            this.i18n("feedback.shop_missing.message", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        if (!this.canClose(clickContext, currentShop)) {
            this.i18n("feedback.not_owner", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        if (currentShop.getStoredItemCount() > 0) {
            this.i18n("feedback.close_not_empty", clickContext.getPlayer())
                    .withPlaceholder("item_count", currentShop.getStoredItemCount())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        if (currentShop.getBankCurrencyCount() > 0) {
            this.i18n("feedback.close_bank_not_empty", clickContext.getPlayer())
                    .withPlaceholder("bank_currency_count", currentShop.getBankCurrencyCount())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RDS plugin = this.rds.get(clickContext);
        plugin.getShopRepository().deleteEntity(currentShop);
        this.clearShopBlock(currentShop.getShopLocation());
        this.clearShopBlock(currentShop.getSecondaryShopLocation());
        final int activeOwnedShops = ShopOwnershipSupport.countOwnedPlayerShops(plugin, currentShop.getOwner());

        final int returnedBlocks = currentShop.getShopBlockCount();
        final ItemStack[] returnedItems = new ItemStack[returnedBlocks];
        for (int index = 0; index < returnedBlocks; index++) {
            if (currentShop.isTownShop() && plugin.getTownShopService() != null) {
                final var outpost = plugin.getTownShopService().getOutpost(currentShop);
                returnedItems[index] = outpost == null
                        ? ShopBlock.getShopBlock(plugin, clickContext.getPlayer())
                        : TownShopBlock.getTownShopBlock(plugin, clickContext.getPlayer(), outpost);
            } else {
                returnedItems[index] = ShopBlock.getShopBlock(plugin, clickContext.getPlayer());
            }
        }

        clickContext.getPlayer().getInventory().addItem(returnedItems)
                .forEach((slot, item) -> clickContext.getPlayer().getWorld().dropItem(
                        clickContext.getPlayer().getLocation().clone().add(0, 0.5, 0),
                        item
                ));

        this.i18n(currentShop.isTownShop() ? "feedback.closed_town" : "feedback.closed", clickContext.getPlayer())
                .withPlaceholders(Map.of(
                        "owned_shops", activeOwnedShops,
                        "returned_blocks", returnedBlocks
                ))
                .includePrefix()
                .build()
                .sendMessage();
        clickContext.getPlayer().closeInventory();
    }

    private void clearShopBlock(
            final Location location
    ) {
        if (location != null && location.getWorld() != null) {
            location.getBlock().setType(Material.AIR);
        }
    }

    private @NotNull ItemStack createFinanceItem(
            final @NotNull Shop shop,
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.GOLD_INGOT)
                .setName(this.i18n("finance.name", player).build().component())
                .setLore(this.i18n("finance.lore", player)
                        .withPlaceholders(Map.of(
                                "bank_currency_count", shop.getBankCurrencyCount()
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createLedgerItem(
            final @NotNull Shop shop,
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
                .setName(this.i18n("actions.ledger.name", player).build().component())
                .setLore(this.i18n("actions.ledger.lore", player)
                        .withPlaceholders(Map.of(
                                "ledger_count", shop.getLedgerEntryCount(),
                                "purchase_count", shop.getLedgerEntryCount(ShopLedgerType.PURCHASE),
                                "tax_count", shop.getLedgerEntryCount(ShopLedgerType.TAXATION)
                                        + shop.getLedgerEntryCount(ShopLedgerType.OUTPOST_TAX)
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createManageItem(
            final @NotNull Shop shop,
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.HOPPER)
                .setName(this.i18n("actions.manage.name", player).build().component())
                .setLore(this.i18n("actions.manage.lore", player)
                        .withPlaceholder("item_count", shop.getStoredItemCount())
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createCloseShopItem(
            final @NotNull Shop shop,
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("actions.close.name", player).build().component())
                .setLore(this.i18n("actions.close.lore", player)
                        .withPlaceholders(Map.of(
                                "item_count", shop.getStoredItemCount(),
                                "bank_currency_count", shop.getBankCurrencyCount()
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createEditItem(
            final @NotNull Shop shop,
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
                .setName(this.i18n("actions.edit.name", player).build().component())
                .setLore(this.i18n("actions.edit.lore", player)
                        .withPlaceholder("item_count", shop.getStoredItemCount())
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createStorageItem(
            final @NotNull Shop shop,
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARREL)
                .setName(this.i18n("actions.storage.name", player).build().component())
                .setLore(this.i18n("actions.storage.lore", player)
                        .withPlaceholder("item_count", shop.getStoredItemCount())
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createTaxLockedManageItem(
            final @NotNull Shop shop,
            final @NotNull Player player,
            final @NotNull RDS plugin
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("actions.manage_locked.name", player).build().component())
                .setLore(this.i18n("actions.manage_locked.lore", player)
                        .withPlaceholders(Map.of(
                                "item_count", shop.getStoredItemCount(),
                                "debt_summary", this.formatDebtSummary(shop, plugin)
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createTaxLockedEditItem(
            final @NotNull Shop shop,
            final @NotNull Player player,
            final @NotNull RDS plugin
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("actions.edit_locked.name", player).build().component())
                .setLore(this.i18n("actions.edit_locked.lore", player)
                        .withPlaceholders(Map.of(
                                "item_count", shop.getStoredItemCount(),
                                "debt_summary", this.formatDebtSummary(shop, plugin)
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createPayTaxesItem(
            final @NotNull Shop shop,
            final @NotNull Player player,
            final @NotNull RDS plugin
    ) {
        return UnifiedBuilderFactory.item(Material.CLOCK)
                .setName(this.i18n("actions.pay_taxes.name", player).build().component())
                .setLore(this.i18n("actions.pay_taxes.lore", player)
                        .withPlaceholders(Map.of(
                                "debt_summary", this.formatDebtSummary(shop, plugin),
                                "debt_currency_count", shop.getTaxDebtEntries().size()
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private void handlePayTaxesClick(
            final @NotNull SlotClickContext clickContext
    ) {
        final Shop shop = this.getCurrentShop(clickContext);
        if (shop == null) {
            this.i18n("feedback.shop_missing.message", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        if (!this.canManage(clickContext, shop)) {
            this.i18n("feedback.not_owner", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        if (!shop.hasTaxDebt()) {
            this.i18n("feedback.tax_not_due", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final RDS plugin = this.rds.get(clickContext);
        final ShopTaxScheduler scheduler = plugin.getShopTaxScheduler();
        if (scheduler == null) {
            this.i18n("feedback.tax_payment_unavailable", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final ShopTaxScheduler.TaxDebtPaymentResult paymentResult = scheduler.payOutstandingDebt(
                shop,
                clickContext.getPlayer()
        );
        final String paidSummary = scheduler.formatCurrencySummary(paymentResult.paidDebts());
        final String remainingSummary = scheduler.formatCurrencySummary(paymentResult.remainingDebts());

        if (!paymentResult.hasPaidAny()) {
            this.i18n("feedback.tax_payment_failed", clickContext.getPlayer())
                    .withPlaceholder("remaining_debt", remainingSummary)
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        if (paymentResult.isFullyPaid()) {
            this.i18n("feedback.tax_payment_cleared", clickContext.getPlayer())
                    .withPlaceholder("paid_amount", paidSummary)
                    .includePrefix()
                    .build()
                    .sendMessage();
        } else {
            this.i18n("feedback.tax_payment_partial", clickContext.getPlayer())
                    .withPlaceholders(Map.of(
                            "paid_amount", paidSummary,
                            "remaining_debt", remainingSummary
                    ))
                    .includePrefix()
                    .build()
                    .sendMessage();
        }

        clickContext.openForPlayer(
                ShopOverviewView.class,
                this.createShopViewData(clickContext, shop)
        );
    }

    private @NotNull ItemStack createMissingShopItem(
            final @NotNull Player player
    ) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
                .setName(this.i18n("feedback.shop_missing.name", player).build().component())
                .setLore(this.i18n("feedback.shop_missing.lore", player).build().children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull String getSummaryLoreSuffix(
            final @NotNull Shop shop
    ) {
        if (shop.isAdminShop()) {
            return "summary.admin.lore";
        }
        return shop.isTownShop()
                ? "summary.town.lore"
                : "summary.player.lore";
    }

    private @NotNull String formatLocation(final @NotNull Location location) {
        return "("
                + location.getBlockX() + ", "
                + location.getBlockY() + ", "
                + location.getBlockZ() + ")";
    }

    private @NotNull String getOwnerName(final @NotNull Shop shop) {
        return ShopAdminAccessSupport.resolveOwnerName(shop);
    }

    private @NotNull String formatDebtSummary(
            final Shop shop,
            final @NotNull RDS plugin
    ) {
        if (shop == null || !shop.hasTaxDebt()) {
            return "None";
        }

        final ShopTaxScheduler scheduler = plugin.getShopTaxScheduler();
        if (scheduler == null) {
            return "None";
        }

        return scheduler.formatCurrencySummary(shop.getTaxDebtEntries());
    }

    private boolean canManage(
            final @NotNull Context context,
            final @NotNull Shop shop
    ) {
        return ShopAdminAccessSupport.canManage(context, shop, this.rds.get(context));
    }

    private boolean canSupply(
            final @NotNull Context context,
            final @NotNull Shop shop
    ) {
        return ShopAdminAccessSupport.canSupply(context, shop, this.rds.get(context));
    }

    private boolean canClose(
            final @NotNull Context context,
            final @NotNull Shop shop
    ) {
        return ShopAdminAccessSupport.canActAsOwner(context, shop, this.rds.get(context));
    }

    private @NotNull Map<String, Object> createShopViewData(
            final @NotNull Context context,
            final @NotNull Shop shop
    ) {
        final Map<String, Object> viewData = new HashMap<>();
        viewData.put("plugin", this.rds.get(context));
        viewData.put("shopLocation", shop.getShopLocation());
        if (ShopAdminAccessSupport.hasOwnerOverride(context)) {
            viewData.put(ShopAdminAccessSupport.ADMIN_OWNER_OVERRIDE_KEY, true);
        }
        return viewData;
    }
}
