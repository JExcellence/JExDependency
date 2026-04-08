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
import com.raindropcentral.rds.configs.AdminShopRestockMode;
import com.raindropcentral.rds.database.entity.Shop;
import com.raindropcentral.rds.items.AbstractItem;
import com.raindropcentral.rds.items.ShopItem;
import com.raindropcentral.rds.service.shop.AdminShopStockSupport;
import com.raindropcentral.rds.service.tax.ShopTaxScheduler;
import com.raindropcentral.rds.view.shop.anvil.ShopItemAdminResetTimerAnvilView;
import com.raindropcentral.rds.view.shop.anvil.ShopItemAdminStockLimitAnvilView;
import com.raindropcentral.rds.view.shop.anvil.ShopItemAvailabilityMinutesAnvilView;
import com.raindropcentral.rds.view.shop.anvil.ShopItemCurrencyTypeAnvilView;
import com.raindropcentral.rds.view.shop.anvil.ShopItemPurchaseLimitAnvilView;
import com.raindropcentral.rds.view.shop.anvil.ShopItemPurchaseLimitMinutesAnvilView;
import com.raindropcentral.rds.view.shop.anvil.ShopItemValueAnvilView;
import com.raindropcentral.rplatform.utility.heads.view.Proceed;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.MutableState;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders the shop item edit inventory view.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopItemEditView extends BaseView {

    private static final DateTimeFormatter RESTOCK_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final State<RDS> rds = initialState("plugin");
    private final State<Location> shopLocation = initialState("shopLocation");
    private final State<ShopItem> sourceItem = initialState("shopItem");
    private final MutableState<ShopItem> editedItem = mutableState((ShopItem) null);

    /**
     * Creates a new shop item edit view.
     */
    public ShopItemEditView() {
        super(ShopEditView.class);
    }

    @Override
    protected String getKey() {
        return "shop_item_edit_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
                "lrm s    ",
                "an tvdpwk",
                "        c"
        };
    }

    @Override
    protected boolean shouldAutoFill() {
        return false;
    }

    @Override
    protected Map<String, Object> getTitlePlaceholders(
            final @NotNull OpenContext context
    ) {
        final ShopItem item = this.getEditedItem(context);
        return Map.of(
                "item_type", item.getItem().getType().name(),
                "currency_type", item.getCurrencyType(),
                "value", item.getValue()
        );
    }

    /**
     * Executes onResume.
     */
    @Override
    public void onResume(
            final @NotNull Context origin,
            final @NotNull Context target
    ) {
        this.restoreEditedItem(target);
        if (this.editedItem.get(target) == null) {
            this.restoreEditedItem(origin, target);
        }

        target.update();
    }

    /**
     * Executes onFirstRender.
     */
    @Override
    public void onFirstRender(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final RDS plugin = this.rds.get(render);
        final Shop currentShop = this.getCurrentShop(render);
        if (this.editedItem.get(render) == null) {
            this.restoreEditedItem(render);
        }
        if (this.editedItem.get(render) == null) {
            this.editedItem.set(this.sourceItem.get(render), render);
        }

        render.layoutSlot('s')
                .renderWith(() -> this.createSummaryItem(player, plugin, currentShop, this.getEditedItem(render)))
                .updateOnStateChange(this.editedItem);

        render.layoutSlot('t')
                .renderWith(() -> this.createCurrencyTypeItem(player, this.getEditedItem(render)))
                .updateOnStateChange(this.editedItem)
                .onClick(clickContext -> {
                    if (!this.ensureTaxUnlocked(clickContext)) {
                        return;
                    }

                    clickContext.openForPlayer(
                            ShopItemCurrencyTypeAnvilView.class,
                            this.createItemViewData(clickContext, this.getEditedItem(clickContext))
                    );
                });

        render.layoutSlot('v')
                .renderWith(() -> this.createValueItem(player, this.getEditedItem(render)))
                .updateOnStateChange(this.editedItem)
                .onClick(clickContext -> {
                    if (!this.ensureTaxUnlocked(clickContext)) {
                        return;
                    }

                    clickContext.openForPlayer(
                            ShopItemValueAnvilView.class,
                            this.createItemViewData(clickContext, this.getEditedItem(clickContext))
                    );
                });

        render.layoutSlot('d')
                .renderWith(() -> this.createDynamicPricingItem(player, plugin, this.getEditedItem(render)))
                .updateOnStateChange(this.editedItem)
                .onClick(this::handleDynamicPricingToggleClick);

        render.layoutSlot('a')
                .renderWith(() -> this.createAvailabilityModeItem(player, this.getEditedItem(render)))
                .updateOnStateChange(this.editedItem)
                .onClick(this::handleAvailabilityModeClick);

        render.layoutSlot('n')
                .renderWith(() -> this.createAvailabilityWindowItem(player, this.getEditedItem(render)))
                .updateOnStateChange(this.editedItem)
                .onClick(clickContext -> {
                    if (!this.ensureTaxUnlocked(clickContext)) {
                        return;
                    }

                    clickContext.openForPlayer(
                            ShopItemAvailabilityMinutesAnvilView.class,
                            this.createItemViewData(clickContext, this.getEditedItem(clickContext))
                    );
                });

        render.layoutSlot('p')
                .renderWith(() -> this.createPurchaseLimitAmountItem(player, this.getEditedItem(render)))
                .updateOnStateChange(this.editedItem)
                .onClick(clickContext -> {
                    if (!this.ensureTaxUnlocked(clickContext)) {
                        return;
                    }

                    clickContext.openForPlayer(
                            ShopItemPurchaseLimitAnvilView.class,
                            this.createItemViewData(clickContext, this.getEditedItem(clickContext))
                    );
                });

        render.layoutSlot('w')
                .renderWith(() -> this.createPurchaseLimitWindowItem(player, this.getEditedItem(render)))
                .updateOnStateChange(this.editedItem)
                .onClick(clickContext -> {
                    if (!this.ensureTaxUnlocked(clickContext)) {
                        return;
                    }

                    clickContext.openForPlayer(
                            ShopItemPurchaseLimitMinutesAnvilView.class,
                            this.createItemViewData(clickContext, this.getEditedItem(clickContext))
                    );
                });

        render.layoutSlot(
                'c',
                UnifiedBuilderFactory.item(new Proceed().getHead(player))
                        .setName(this.i18n("confirm.name", player).build().component())
                        .setLore(this.i18n("confirm.lore", player).build().children())
                        .build()
        ).onClick(clickContext -> this.handleConfirm(clickContext));

        if (currentShop != null && currentShop.isAdminShop()) {
            render.layoutSlot('k')
                    .renderWith(() -> this.createAdminCommandsItem(player, this.getEditedItem(render)))
                    .updateOnStateChange(this.editedItem)
                    .onClick(this::handleAdminCommandClick);

            render.layoutSlot('l')
                    .renderWith(() -> this.createStockLimitItem(player, this.getEditedItem(render)))
                    .updateOnStateChange(this.editedItem)
                    .onClick(clickContext -> {
                        if (!this.ensureTaxUnlocked(clickContext)) {
                            return;
                        }

                        clickContext.openForPlayer(
                                ShopItemAdminStockLimitAnvilView.class,
                                this.createItemViewData(clickContext, this.getEditedItem(clickContext))
                        );
                    });

            render.layoutSlot('r')
                    .renderWith(() -> this.createResetTimerItem(player, plugin, this.getEditedItem(render)))
                    .updateOnStateChange(this.editedItem)
                    .onClick(clickContext -> {
                        if (!this.ensureTaxUnlocked(clickContext)) {
                            return;
                        }

                        clickContext.openForPlayer(
                                ShopItemAdminResetTimerAnvilView.class,
                                this.createItemViewData(clickContext, this.getEditedItem(clickContext))
                        );
                    });

            render.layoutSlot('m')
                    .renderWith(() -> this.createRestockModeItem(player, plugin))
                    .updateOnStateChange(this.editedItem)
                    .onClick(this::handleRestockModeClick);
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

    private void handleConfirm(final @NotNull SlotClickContext clickContext) {
        clickContext.setCancelled(true);
        clickContext.getPlayer().setItemOnCursor(null);

        final Shop shop = this.getCurrentShop(clickContext);
        if (shop == null) {
            this.i18n("feedback.shop_missing", clickContext.getPlayer())
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

        if (shop.hasTaxDebt()) {
            this.sendTaxLockedFeedback(clickContext, shop);
            return;
        }

        final ShopItem edited = this.getEditedItem(clickContext);
        final ShopItem original = this.sourceItem.get(clickContext);
        final List<AbstractItem> items = new ArrayList<>(shop.getItems());
        final int matchingIndex = this.findMatchingItemIndex(items, original, edited);

        if (matchingIndex >= 0) {
            final ShopItem matchingItem = (ShopItem) items.get(matchingIndex);
            items.set(matchingIndex, this.mergeEditedItem(matchingItem, edited));
            shop.setItems(items);
            this.rds.get(clickContext).getShopRepository().update(shop);

            this.i18n("feedback.saved", clickContext.getPlayer())
                    .withPlaceholders(Map.of(
                            "currency_type", edited.getCurrencyType(),
                            "value", edited.getValue()
                    ))
                    .build()
                    .sendMessage();

            clickContext.openForPlayer(
                    ShopEditView.class,
                    this.createBaseViewData(clickContext)
            );
            return;
        }

        this.i18n("feedback.item_missing", clickContext.getPlayer())
                .build()
                .sendMessage();
    }

    private void handleAvailabilityModeClick(
            final @NotNull SlotClickContext clickContext
    ) {
        clickContext.setCancelled(true);

        final Shop shop = this.getCurrentShop(clickContext);
        if (shop == null) {
            this.i18n("feedback.shop_missing", clickContext.getPlayer())
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

        if (shop.hasTaxDebt()) {
            this.sendTaxLockedFeedback(clickContext, shop);
            return;
        }

        final ShopItem current = this.getEditedItem(clickContext);
        final ShopItem.AvailabilityMode updatedMode = current.getAvailabilityMode().next();
        this.editedItem.set(current.withAvailabilityMode(updatedMode), clickContext);
        clickContext.update();

        this.i18n("feedback.availability_mode_updated", clickContext.getPlayer())
                .withPlaceholder("availability_mode", this.getAvailabilityModeLabel(clickContext.getPlayer(), updatedMode))
                .build()
                .sendMessage();
    }

    private void handleDynamicPricingToggleClick(
            final @NotNull SlotClickContext clickContext
    ) {
        clickContext.setCancelled(true);

        final Shop shop = this.getCurrentShop(clickContext);
        if (shop == null) {
            this.i18n("feedback.shop_missing", clickContext.getPlayer())
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

        if (shop.hasTaxDebt()) {
            this.sendTaxLockedFeedback(clickContext, shop);
            return;
        }

        final ShopItem current = this.getEditedItem(clickContext);
        final boolean updatedEnabled = !current.isDynamicPricingEnabled();
        this.editedItem.set(current.withDynamicPricingEnabled(updatedEnabled), clickContext);
        clickContext.update();

        this.i18n("feedback.dynamic_pricing_updated", clickContext.getPlayer())
                .withPlaceholder("dynamic_pricing", this.getDynamicPricingStateLabel(clickContext.getPlayer(), updatedEnabled))
                .build()
                .sendMessage();
    }

    private void handleAdminCommandClick(
            final @NotNull SlotClickContext clickContext
    ) {
        clickContext.setCancelled(true);

        final Shop shop = this.getCurrentShop(clickContext);
        if (shop == null) {
            this.i18n("feedback.shop_missing", clickContext.getPlayer())
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

        if (shop.hasTaxDebt()) {
            this.sendTaxLockedFeedback(clickContext, shop);
            return;
        }

        clickContext.openForPlayer(
                ShopItemAdminCommandView.class,
                this.createItemViewData(clickContext, this.getEditedItem(clickContext))
        );
    }

    private void handleRestockModeClick(
            final @NotNull SlotClickContext clickContext
    ) {
        clickContext.setCancelled(true);

        final Shop shop = this.getCurrentShop(clickContext);
        if (shop == null) {
            this.i18n("feedback.shop_missing", clickContext.getPlayer())
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

        if (shop.hasTaxDebt()) {
            this.sendTaxLockedFeedback(clickContext, shop);
            return;
        }

        final RDS plugin = this.rds.get(clickContext);
        if (!plugin.canChangeConfigs()) {
            this.i18n("feedback.config_locked", clickContext.getPlayer())
                    .includePrefix()
                    .build()
                    .sendMessage();
            return;
        }

        final AdminShopRestockMode updatedMode = this.toggleAdminRestockMode(plugin);
        if (updatedMode == null) {
            this.i18n("feedback.restock_mode_save_failed", clickContext.getPlayer())
                    .build()
                    .sendMessage();
            return;
        }

        final long updatedReferenceTime = AdminShopStockSupport.resolveReferenceTimeForMode(
                plugin.getDefaultConfig().getAdminShops(),
                updatedMode
        );
        this.rebaseAllAdminShopReferenceTimes(plugin, updatedReferenceTime);
        this.editedItem.set(
                this.rebaseEditedItemReferenceTime(this.getEditedItem(clickContext), updatedReferenceTime),
                clickContext
        );

        clickContext.update();

        this.i18n("feedback.restock_mode_updated", clickContext.getPlayer())
                .withPlaceholder("restock_mode", this.getAdminRestockModeLabel(clickContext.getPlayer(), plugin))
                .build()
                .sendMessage();
    }

    private Shop getCurrentShop(final @NotNull Context context) {
        final RDS plugin = this.rds.get(context);
        final Shop shop = plugin.getShopRepository().findByLocation(this.shopLocation.get(context));
        if (shop != null && shop.isAdminShop()) {
            plugin.getAdminShopRestockScheduler().restockShop(shop);
        }
        return shop;
    }

    private boolean canManage(
            final @NotNull Context context,
            final @NotNull Shop shop
    ) {
        return shop.canManage(context.getPlayer().getUniqueId()) || ShopAdminAccessSupport.hasOwnerOverride(context);
    }

    private @NotNull Map<String, Object> createBaseViewData(
            final @NotNull Context context
    ) {
        final Map<String, Object> viewData = new HashMap<>();
        viewData.put("plugin", this.rds.get(context));
        viewData.put("shopLocation", this.shopLocation.get(context));
        if (ShopAdminAccessSupport.hasOwnerOverride(context)) {
            viewData.put(ShopAdminAccessSupport.ADMIN_OWNER_OVERRIDE_KEY, true);
        }
        return viewData;
    }

    private @NotNull Map<String, Object> createItemViewData(
            final @NotNull Context context,
            final @NotNull ShopItem shopItem
    ) {
        final Map<String, Object> viewData = this.createBaseViewData(context);
        viewData.put("shopItem", shopItem);
        return viewData;
    }

    private @Nullable AdminShopRestockMode toggleAdminRestockMode(
            final @NotNull RDS plugin
    ) {
        this.syncAllAdminShops(plugin);

        final AdminShopRestockMode currentMode = plugin.getDefaultConfig().getAdminShops().getRestockMode();
        final AdminShopRestockMode updatedMode = currentMode == AdminShopRestockMode.FULL_AT_TIME
                ? AdminShopRestockMode.GRADUAL
                : AdminShopRestockMode.FULL_AT_TIME;
        final File configFile = new File(new File(plugin.getDataFolder(), "config"), "config.yml");
        final YamlConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
        configuration.set(
                "admin_shops.restock_mode",
                updatedMode == AdminShopRestockMode.FULL_AT_TIME ? "full_at_time" : "gradual"
        );

        try {
            configuration.save(configFile);
            return updatedMode;
        } catch (IOException exception) {
            plugin.getLogger().warning("Failed to save admin restock mode toggle: " + exception.getMessage());
            return null;
        }
    }

    private void syncAllAdminShops(
            final @NotNull RDS plugin
    ) {
        for (final Shop adminShop : plugin.getShopRepository().findAllShops()) {
            if (adminShop.isAdminShop()) {
                plugin.getAdminShopRestockScheduler().restockShop(adminShop);
            }
        }
    }

    private void rebaseAllAdminShopReferenceTimes(
            final @NotNull RDS plugin,
            final long updatedReferenceTime
    ) {
        for (final Shop adminShop : plugin.getShopRepository().findAllShops()) {
            if (!adminShop.isAdminShop()) {
                continue;
            }

            boolean changed = false;
            final List<AbstractItem> updatedItems = new ArrayList<>(adminShop.getItems().size());
            for (final AbstractItem item : adminShop.getItems()) {
                if (item instanceof ShopItem shopItem && shopItem.hasAdminStockLimit()) {
                    final ShopItem updatedItem = this.rebaseItemReferenceTime(shopItem, updatedReferenceTime);
                    if (updatedItem.getAdminStockReferenceTime() != shopItem.getAdminStockReferenceTime()) {
                        changed = true;
                    }
                    updatedItems.add(updatedItem);
                    continue;
                }

                updatedItems.add(item);
            }

            if (changed) {
                adminShop.setItems(updatedItems);
                plugin.getShopRepository().update(adminShop);
            }
        }
    }

    private @NotNull ShopItem rebaseEditedItemReferenceTime(
            final @NotNull ShopItem item,
            final long updatedReferenceTime
    ) {
        if (!item.hasAdminStockLimit()) {
            return item;
        }
        return this.rebaseItemReferenceTime(item, updatedReferenceTime);
    }

    private @NotNull ShopItem rebaseItemReferenceTime(
            final @NotNull ShopItem item,
            final long updatedReferenceTime
    ) {
        return item.withAdminStockSettings(
                item.hasAdminStockLimit() ? item.getAdminStockLimit() : null,
                this.getStoredRestockInterval(item),
                updatedReferenceTime
        );
    }

    private @NotNull ShopItem getEditedItem(final @NotNull Context context) {
        final ShopItem current = this.editedItem.get(context);
        return current == null ? this.sourceItem.get(context) : current;
    }

    private void restoreEditedItem(final @NotNull Context context) {
        this.restoreEditedItem(context, context);
    }

    private void restoreEditedItem(
            final @NotNull Context dataContext,
            final @NotNull Context stateContext
    ) {
        final Object initialData = dataContext.getInitialData();
        if (!(initialData instanceof Map<?, ?> data)) {
            return;
        }

        final Object updatedItem = data.get("shopItem");
        if (updatedItem instanceof ShopItem shopItem) {
            this.editedItem.set(shopItem, stateContext);
        }
    }

    private boolean ensureTaxUnlocked(
            final @NotNull SlotClickContext clickContext
    ) {
        final Shop shop = this.getCurrentShop(clickContext);
        if (shop == null || !shop.hasTaxDebt()) {
            return true;
        }

        this.sendTaxLockedFeedback(clickContext, shop);
        return false;
    }

    private void sendTaxLockedFeedback(
            final @NotNull SlotClickContext clickContext,
            final @NotNull Shop shop
    ) {
        this.i18n("feedback.tax_locked", clickContext.getPlayer())
                .withPlaceholder("debt_summary", this.formatDebtSummary(shop, this.rds.get(clickContext)))
                .includePrefix()
                .build()
                .sendMessage();
    }


    private int findMatchingItemIndex(
            final @NotNull List<AbstractItem> items,
            final @NotNull ShopItem original,
            final @NotNull ShopItem edited
    ) {
        for (int index = 0; index < items.size(); index++) {
            final AbstractItem item = items.get(index);
            if (!(item instanceof ShopItem shopItem)) {
                continue;
            }

            if (shopItem.getEntryId().equals(edited.getEntryId())) {
                return index;
            }
        }

        for (int index = 0; index < items.size(); index++) {
            final AbstractItem item = items.get(index);
            if (!(item instanceof ShopItem shopItem)) {
                continue;
            }

            if (this.isLegacyMatch(shopItem, original)) {
                return index;
            }
        }

        return -1;
    }

    private boolean isLegacyMatch(
            final @NotNull ShopItem candidate,
            final @NotNull ShopItem original
    ) {
        return candidate.getAmount() == original.getAmount()
                && candidate.getItem().isSimilar(original.getItem());
    }

    private @NotNull ItemStack createSummaryItem(
            final @NotNull Player player,
            final @NotNull RDS plugin,
            final Shop currentShop,
            final @NotNull ShopItem item
    ) {
        final ItemStack displayItem = item.getItem();
        displayItem.setAmount(1);

        final String loreKey;
        final Map<String, Object> placeholders;
        if (currentShop != null && currentShop.isAdminShop() && item.hasAdminStockLimit()) {
            loreKey = "summary.admin_limited.lore";
            final Map<String, Object> limitedPlaceholders = new HashMap<>();
            limitedPlaceholders.put("amount", item.getAmount());
            limitedPlaceholders.put("stock_limit", item.getAdminStockLimit());
            limitedPlaceholders.put("currency_type", item.getCurrencyType());
            limitedPlaceholders.put("value", item.getValue());
            limitedPlaceholders.put("restock_mode", this.getAdminRestockModeLabel(player, plugin));
            limitedPlaceholders.put("restock_schedule", this.getAdminRestockSchedule(player, item, plugin));
            limitedPlaceholders.put("availability_mode", this.getAvailabilityModeLabel(player, item));
            limitedPlaceholders.put("availability_state", this.getAvailabilityStateLabel(player, item));
            limitedPlaceholders.put("rotation_minutes", item.getAvailabilityRotationMinutes());
            limitedPlaceholders.put("purchase_limit", this.getPurchaseLimitLabel(player, item));
            limitedPlaceholders.put("command_count", item.getAdminPurchaseCommands().size());
            placeholders = limitedPlaceholders;
        } else if (currentShop != null && currentShop.isAdminShop()) {
            loreKey = "summary.admin_unlimited.lore";
            placeholders = Map.of(
                    "amount", item.getAmount(),
                    "currency_type", item.getCurrencyType(),
                    "value", item.getValue(),
                    "restock_mode", this.getAdminRestockModeLabel(player, plugin),
                    "availability_mode", this.getAvailabilityModeLabel(player, item),
                    "availability_state", this.getAvailabilityStateLabel(player, item),
                    "rotation_minutes", item.getAvailabilityRotationMinutes(),
                    "purchase_limit", this.getPurchaseLimitLabel(player, item),
                    "command_count", item.getAdminPurchaseCommands().size()
            );
        } else {
            loreKey = "summary.player.lore";
            placeholders = Map.of(
                    "amount", item.getAmount(),
                    "currency_type", item.getCurrencyType(),
                    "value", item.getValue(),
                    "availability_mode", this.getAvailabilityModeLabel(player, item),
                    "availability_state", this.getAvailabilityStateLabel(player, item),
                    "rotation_minutes", item.getAvailabilityRotationMinutes(),
                    "purchase_limit", this.getPurchaseLimitLabel(player, item)
            );
        }

        final List<Component> lore = new ArrayList<>(this.i18n(loreKey, player)
                .withPlaceholders(placeholders)
                .build()
                .children());

        final var itemLore = displayItem.lore();
        if (itemLore != null && !itemLore.isEmpty()) {
            lore.add(Component.empty());
            lore.addAll(itemLore);
        }

        return UnifiedBuilderFactory.item(displayItem)
                .setLore(lore)
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createCurrencyTypeItem(
            final @NotNull Player player,
            final @NotNull ShopItem item
    ) {
        return UnifiedBuilderFactory.item(Material.EMERALD)
                .setName(this.i18n("currency_type.name", player).build().component())
                .setLore(this.i18n("currency_type.lore", player)
                        .withPlaceholder("currency_type", item.getCurrencyType())
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createValueItem(
            final @NotNull Player player,
            final @NotNull ShopItem item
    ) {
        return UnifiedBuilderFactory.item(Material.GOLD_INGOT)
                .setName(this.i18n("value.name", player).build().component())
                .setLore(this.i18n("value.lore", player)
                        .withPlaceholders(Map.of(
                                "currency_type", item.getCurrencyType(),
                                "value", item.getValue()
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createDynamicPricingItem(
            final @NotNull Player player,
            final @NotNull RDS plugin,
            final @NotNull ShopItem item
    ) {
        final boolean itemEnabled = item.isDynamicPricingEnabled();
        final boolean globalEnabled = plugin.getDefaultConfig().getDynamicPricing().isEnabled();
        final Material material = itemEnabled
                ? (globalEnabled ? Material.LIME_DYE : Material.YELLOW_DYE)
                : Material.GRAY_DYE;
        return UnifiedBuilderFactory.item(material)
                .setName(this.i18n("dynamic_pricing.name", player).build().component())
                .setLore(this.i18n("dynamic_pricing.lore", player)
                        .withPlaceholders(Map.of(
                                "dynamic_pricing", this.getDynamicPricingStateLabel(player, itemEnabled),
                                "global_dynamic_pricing", this.getGlobalDynamicPricingStateLabel(player, globalEnabled)
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createAvailabilityModeItem(
            final @NotNull Player player,
            final @NotNull ShopItem item
    ) {
        final Material material = switch (item.getAvailabilityMode()) {
            case ALWAYS -> Material.LIME_DYE;
            case ROTATE -> Material.CLOCK;
            case NEVER -> Material.BARRIER;
        };
        return UnifiedBuilderFactory.item(material)
                .setName(this.i18n("availability_mode.name", player).build().component())
                .setLore(this.i18n("availability_mode.lore", player)
                        .withPlaceholders(Map.of(
                                "availability_mode", this.getAvailabilityModeLabel(player, item),
                                "availability_state", this.getAvailabilityStateLabel(player, item),
                                "rotation_minutes", item.getAvailabilityRotationMinutes()
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createAvailabilityWindowItem(
            final @NotNull Player player,
            final @NotNull ShopItem item
    ) {
        return UnifiedBuilderFactory.item(Material.CLOCK)
                .setName(this.i18n("availability_window.name", player).build().component())
                .setLore(this.i18n("availability_window.lore", player)
                        .withPlaceholders(Map.of(
                                "rotation_minutes", item.getAvailabilityRotationMinutes(),
                                "availability_mode", this.getAvailabilityModeLabel(player, item),
                                "rotate_mode", this.getAvailabilityModeLabel(player, ShopItem.AvailabilityMode.ROTATE)
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createPurchaseLimitAmountItem(
            final @NotNull Player player,
            final @NotNull ShopItem item
    ) {
        return UnifiedBuilderFactory.item(Material.PAPER)
                .setName(this.i18n("purchase_limit_amount.name", player).build().component())
                .setLore(this.i18n("purchase_limit_amount.lore", player)
                        .withPlaceholders(Map.of(
                                "purchase_limit", this.getPurchaseLimitLabel(player, item),
                                "limit_amount", item.getPurchaseLimitAmount(),
                                "window_minutes", item.getPurchaseLimitWindowMinutes()
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createPurchaseLimitWindowItem(
            final @NotNull Player player,
            final @NotNull ShopItem item
    ) {
        return UnifiedBuilderFactory.item(Material.CLOCK)
                .setName(this.i18n("purchase_limit_window.name", player).build().component())
                .setLore(this.i18n("purchase_limit_window.lore", player)
                        .withPlaceholders(Map.of(
                                "purchase_limit", this.getPurchaseLimitLabel(player, item),
                                "limit_amount", item.hasPurchaseLimit() ? item.getPurchaseLimitAmount() : 1,
                                "window_minutes", item.hasPurchaseLimit() ? item.getPurchaseLimitWindowMinutes() : 60
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createAdminCommandsItem(
            final @NotNull Player player,
            final @NotNull ShopItem item
    ) {
        return UnifiedBuilderFactory.item(Material.COMMAND_BLOCK)
                .setName(this.i18n("admin_commands.name", player).build().component())
                .setLore(this.i18n("admin_commands.lore", player)
                        .withPlaceholders(Map.of(
                                "command_count", item.getAdminPurchaseCommands().size(),
                                "latest_command", this.getLatestCommandPreview(player, item)
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createStockLimitItem(
            final @NotNull Player player,
            final @NotNull ShopItem item
    ) {
        final String stockLimit = item.hasAdminStockLimit()
                ? String.valueOf(item.getAdminStockLimit())
                : this.i18n("stock_limit.unlimited", player)
                        .build()
                        .getI18nVersionWrapper()
                        .asPlaceholder();
        return UnifiedBuilderFactory.item(Material.CHEST)
                .setName(this.i18n("stock_limit.name", player).build().component())
                .setLore(this.i18n("stock_limit.lore", player)
                        .withPlaceholders(Map.of(
                                "stock_limit", stockLimit,
                                "current_stock", item.getAmount()
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createResetTimerItem(
            final @NotNull Player player,
            final @NotNull RDS plugin,
            final @NotNull ShopItem item
    ) {
        final long resetTimerTicks = item.getAdminRestockIntervalTicks() > 0L
                ? item.getAdminRestockIntervalTicks()
                : plugin.getDefaultConfig().getAdminShops().getDefaultResetTimerTicks();
        return UnifiedBuilderFactory.item(Material.CLOCK)
                .setName(this.i18n("reset_timer.name", player).build().component())
                .setLore(this.i18n("reset_timer.lore", player)
                        .withPlaceholder("reset_timer_ticks", resetTimerTicks)
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ItemStack createRestockModeItem(
            final @NotNull Player player,
            final @NotNull RDS plugin
    ) {
        return UnifiedBuilderFactory.item(Material.REPEATER)
                .setName(this.i18n("restock_mode.name", player).build().component())
                .setLore(this.i18n("restock_mode.lore", player)
                        .withPlaceholders(Map.of(
                                "restock_mode", this.getAdminRestockModeLabel(player, plugin),
                                "restock_schedule", this.getAdminSummaryRestockSchedule(player, plugin)
                        ))
                        .build()
                        .children())
                .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
                .build();
    }

    private @NotNull ShopItem mergeEditedItem(
            final @NotNull ShopItem liveItem,
            final @NotNull ShopItem edited
    ) {
        final Integer stockLimit = edited.hasAdminStockLimit() ? edited.getAdminStockLimit() : null;
        final Long restockInterval = this.getStoredRestockInterval(edited);
        final Long referenceTime = edited.getAdminStockReferenceTime() >= 0L
                ? edited.getAdminStockReferenceTime()
                : null;
        final int mergedAmount = stockLimit == null
                ? liveItem.getAmount()
                : Math.min(stockLimit, Math.max(0, edited.getAmount()));

        return new ShopItem(
                liveItem.getEntryId(),
                liveItem.getItem(),
                mergedAmount,
                edited.getCurrencyType(),
                edited.getValue(),
                stockLimit,
                restockInterval,
                referenceTime,
                edited.getAvailabilityMode(),
                edited.getAvailabilityRotationMinutes(),
                edited.hasPurchaseLimit() ? edited.getPurchaseLimitAmount() : null,
                edited.hasPurchaseLimit() ? edited.getPurchaseLimitWindowMinutes() : null,
                edited.getAdminPurchaseCommands(),
                edited.isDynamicPricingEnabled()
        );
    }

    private Long getStoredRestockInterval(
            final @NotNull ShopItem item
    ) {
        return item.getAdminRestockIntervalTicks() > 0L
                ? item.getAdminRestockIntervalTicks()
                : null;
    }

    private @NotNull String getAvailabilityModeLabel(
            final @NotNull Player player,
            final @NotNull ShopItem item
    ) {
        return this.getAvailabilityModeLabel(player, item.getAvailabilityMode());
    }

    private @NotNull String getAvailabilityModeLabel(
            final @NotNull Player player,
            final @NotNull ShopItem.AvailabilityMode mode
    ) {
        final String key = switch (mode) {
            case ALWAYS -> "availability_mode.always";
            case ROTATE -> "availability_mode.rotate";
            case NEVER -> "availability_mode.never";
        };
        return this.i18n(key, player)
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
    }

    private @NotNull String getAvailabilityStateLabel(
            final @NotNull Player player,
            final @NotNull ShopItem item
    ) {
        final String key = item.isAvailableNow()
                ? "availability_mode.state.available"
                : "availability_mode.state.unavailable";
        return this.i18n(key, player)
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
    }

    private @NotNull String getPurchaseLimitLabel(
            final @NotNull Player player,
            final @NotNull ShopItem item
    ) {
        if (!item.hasPurchaseLimit()) {
            return this.i18n("purchase_limit.unlimited", player)
                    .build()
                    .getI18nVersionWrapper()
                    .asPlaceholder();
        }

        return this.i18n("purchase_limit.format", player)
                .withPlaceholders(Map.of(
                        "limit_amount", item.getPurchaseLimitAmount(),
                        "window_minutes", item.getPurchaseLimitWindowMinutes()
                ))
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
    }

    private @NotNull String getDynamicPricingStateLabel(
            final @NotNull Player player,
            final boolean enabled
    ) {
        final String key = enabled
                ? "dynamic_pricing.enabled"
                : "dynamic_pricing.disabled";
        return this.i18n(key, player)
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
    }

    private @NotNull String getGlobalDynamicPricingStateLabel(
            final @NotNull Player player,
            final boolean enabled
    ) {
        final String key = enabled
                ? "dynamic_pricing.global_enabled"
                : "dynamic_pricing.global_disabled";
        return this.i18n(key, player)
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
    }

    private @NotNull String getLatestCommandPreview(
            final @NotNull Player player,
            final @NotNull ShopItem item
    ) {
        if (!item.hasAdminPurchaseCommands()) {
            return this.i18n("admin_commands.none", player)
                    .build()
                    .getI18nVersionWrapper()
                    .asPlaceholder();
        }

        final ShopItem.AdminPurchaseCommand latest = item.getAdminPurchaseCommands()
                .get(item.getAdminPurchaseCommands().size() - 1);
        return latest.command();
    }

    private @NotNull String getAdminRestockModeLabel(
            final @NotNull Player player,
            final @NotNull RDS plugin
    ) {
        final var adminConfig = plugin.getDefaultConfig().getAdminShops();
        final String key = adminConfig.getRestockMode() == AdminShopRestockMode.FULL_AT_TIME
                ? "restock_mode.full"
                : "restock_mode.gradual";
        return this.i18n(key, player)
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
    }

    private @NotNull String getAdminRestockSchedule(
            final @NotNull Player player,
            final @NotNull ShopItem item,
            final @NotNull RDS plugin
    ) {
        final var adminConfig = plugin.getDefaultConfig().getAdminShops();
        final String key = adminConfig.getRestockMode() == AdminShopRestockMode.FULL_AT_TIME
                ? "restock_schedule.full"
                : "restock_schedule.gradual";
        return this.i18n(key, player)
                .withPlaceholders(Map.of(
                        "reset_timer_ticks", item.getAdminRestockIntervalTicks() > 0L
                                ? item.getAdminRestockIntervalTicks()
                                : adminConfig.getDefaultResetTimerTicks(),
                        "restock_time", adminConfig.getFullRestockTime().format(RESTOCK_TIME_FORMAT),
                        "time_zone", adminConfig.getTimeZoneId().getId()
                ))
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
    }

    private @NotNull String getAdminSummaryRestockSchedule(
            final @NotNull Player player,
            final @NotNull RDS plugin
    ) {
        final var adminConfig = plugin.getDefaultConfig().getAdminShops();
        final String key = adminConfig.getRestockMode() == AdminShopRestockMode.FULL_AT_TIME
                ? "restock_schedule.full"
                : "restock_schedule.gradual";
        return this.i18n(key, player)
                .withPlaceholders(Map.of(
                        "reset_timer_ticks", adminConfig.getDefaultResetTimerTicks(),
                        "restock_time", adminConfig.getFullRestockTime().format(RESTOCK_TIME_FORMAT),
                        "time_zone", adminConfig.getTimeZoneId().getId()
                ))
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
    }

    private @NotNull String formatDebtSummary(
            final @NotNull Shop shop,
            final @NotNull RDS plugin
    ) {
        final ShopTaxScheduler scheduler = plugin.getShopTaxScheduler();
        if (scheduler == null) {
            return "None";
        }

        return scheduler.formatCurrencySummary(shop.getTaxDebtEntries());
    }
}
