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

package com.raindropcentral.rds.view.shop.anvil;

import com.raindropcentral.rds.RDS;
import com.raindropcentral.rds.items.ShopItem;
import com.raindropcentral.rds.service.shop.AdminShopStockSupport;
import com.raindropcentral.rds.view.shop.ShopItemEditView;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.AbstractAnvilView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.state.State;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Renders the shop item admin stock limit anvil inventory view.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopItemAdminStockLimitAnvilView extends AbstractAnvilView {

    private final State<RDS> rds = initialState("plugin");
    private final State<Location> shopLocation = initialState("shopLocation");
    private final State<ShopItem> targetItem = initialState("shopItem");

    /**
     * Creates a new shop item admin stock limit anvil view.
     */
    public ShopItemAdminStockLimitAnvilView() {
        super(ShopItemEditView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "shop_item_admin_stock_limit_anvil_ui";
    }

    @Override
    protected @NotNull Object processInput(
            final @NotNull String input,
            final @NotNull Context context
    ) {
        final int stockLimit = Integer.parseInt(input.trim());
        return AdminShopStockSupport.configureStockLimit(
                this.rds.get(context),
                this.targetItem.get(context),
                stockLimit
        );
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(
            final @NotNull OpenContext context
    ) {
        final ShopItem item = this.targetItem.get(context);
        return Map.of(
                "stock_limit", item.hasAdminStockLimit() ? item.getAdminStockLimit() : -1,
                "item_type", item.getItem().getType().name()
        );
    }

    @Override
    protected @NotNull String getInitialInputText(
            final @NotNull OpenContext context
    ) {
        final ShopItem item = this.targetItem.get(context);
        return item.hasAdminStockLimit()
                ? String.valueOf(item.getAdminStockLimit())
                : "-1";
    }

    @Override
    protected boolean isValidInput(
            final @NotNull String input,
            final @NotNull Context context
    ) {
        try {
            final int stockLimit = Integer.parseInt(input.trim());
            return stockLimit == -1 || stockLimit > 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    @Override
    protected void setupFirstSlot(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final ShopItem item = this.targetItem.get(render);
        final String currentValue = item.hasAdminStockLimit()
                ? String.valueOf(item.getAdminStockLimit())
                : "-1";
        final ItemStack inputSlotItem = UnifiedBuilderFactory.item(Material.CHEST)
                .setName(Component.text(currentValue))
                .setLore(this.i18n("input.lore", player).build().children())
                .build();

        render.firstSlot(inputSlotItem);
    }

    @Override
    protected void onValidationFailed(
            final @Nullable String input,
            final @NotNull Context context
    ) {
        this.i18n("error.invalid_number", context.getPlayer())
                .includePrefix()
                .withPlaceholder("input", input == null ? "" : input)
                .build()
                .sendMessage();
    }

    @Override
    protected @NotNull Map<String, Object> prepareResultData(
            final @Nullable Object processingResult,
            final @NotNull String input,
            final @NotNull Context context
    ) {
        final Map<String, Object> result = super.prepareResultData(processingResult, input, context);
        result.put("plugin", this.rds.get(context));
        result.put("shopLocation", this.shopLocation.get(context));
        result.put("shopItem", processingResult);
        return result;
    }
}
