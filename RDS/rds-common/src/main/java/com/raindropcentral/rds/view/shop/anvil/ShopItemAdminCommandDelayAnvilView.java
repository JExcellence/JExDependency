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
import com.raindropcentral.rds.view.shop.ShopItemAdminCommandView;
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
 * Renders the command-delay anvil editor for admin-shop item purchase commands.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class ShopItemAdminCommandDelayAnvilView extends AbstractAnvilView {

    private final State<RDS> rds = initialState("plugin");
    private final State<Location> shopLocation = initialState("shopLocation");
    private final State<ShopItem> targetItem = initialState("shopItem");
    private final State<ShopItem.CommandExecutionMode> executionMode = initialState("commandExecutionMode");
    private final State<Long> commandDelayTicks = initialState("commandDelayTicks");

    /**
     * Creates a new command-delay anvil view.
     */
    public ShopItemAdminCommandDelayAnvilView() {
        super(ShopItemAdminCommandView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "shop_item_admin_command_delay_anvil_ui";
    }

    @Override
    protected @NotNull Object processInput(
            final @NotNull String input,
            final @NotNull Context context
    ) {
        return Math.max(0L, Long.parseLong(input.trim()));
    }

    @Override
    protected @NotNull Map<String, Object> getTitlePlaceholders(
            final @NotNull OpenContext context
    ) {
        return Map.of(
                "item_type", this.targetItem.get(context).getItem().getType().name(),
                "command_mode", this.getModeLabel(context.getPlayer(), this.resolveExecutionMode(context)),
                "command_delay_ticks", this.resolveDelayTicks(context)
        );
    }

    @Override
    protected @NotNull String getInitialInputText(
            final @NotNull OpenContext context
    ) {
        return String.valueOf(this.resolveDelayTicks(context));
    }

    @Override
    protected boolean isValidInput(
            final @NotNull String input,
            final @NotNull Context context
    ) {
        try {
            return Long.parseLong(input.trim()) >= 0L;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    @Override
    protected void setupFirstSlot(
            final @NotNull RenderContext render,
            final @NotNull Player player
    ) {
        final ItemStack inputSlotItem = UnifiedBuilderFactory.item(Material.CLOCK)
                .setName(Component.text(String.valueOf(this.resolveDelayTicks(render))))
                .setLore(this.i18n("input.lore", player)
                        .withPlaceholder("command_mode", this.getModeLabel(player, this.resolveExecutionMode(render)))
                        .build()
                        .children())
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
        final long resolvedDelay = processingResult instanceof Number number
                ? Math.max(0L, number.longValue())
                : this.resolveDelayTicks(context);
        result.put("plugin", this.rds.get(context));
        result.put("shopLocation", this.shopLocation.get(context));
        result.put("shopItem", this.targetItem.get(context));
        result.put("commandExecutionMode", this.resolveExecutionMode(context));
        result.put("commandDelayTicks", resolvedDelay);
        return result;
    }

    private @NotNull ShopItem.CommandExecutionMode resolveExecutionMode(
            final @NotNull Context context
    ) {
        final ShopItem.CommandExecutionMode mode = this.executionMode.get(context);
        return mode == null ? ShopItem.CommandExecutionMode.SERVER : mode;
    }

    private long resolveDelayTicks(
            final @NotNull Context context
    ) {
        final Long delayTicks = this.commandDelayTicks.get(context);
        return delayTicks == null ? 0L : Math.max(0L, delayTicks);
    }

    private @NotNull String getModeLabel(
            final @NotNull Player player,
            final @NotNull ShopItem.CommandExecutionMode mode
    ) {
        final String key = mode == ShopItem.CommandExecutionMode.PLAYER
                ? "mode.player"
                : "mode.server";
        return this.i18n(key, player)
                .build()
                .getI18nVersionWrapper()
                .asPlaceholder();
    }
}
