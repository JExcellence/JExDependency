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

import java.util.Map;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.service.TradeService;
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

/**
 * Root trade hub view for the DB-first cross-server trade subsystem.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class TradeHubView extends BaseView {

    private final State<RDR> rdr = initialState("plugin");

    /**
     * Creates a root trade hub view.
     */
    public TradeHubView() {
        super(StorageOverviewView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return trade hub translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "trade_hub_ui";
    }

    /**
     * Returns the inventory layout for this menu.
     *
     * @return three-row trade hub layout
     */
    @Override
    protected String[] getLayout() {
        return new String[]{
            "    s    ",
            "   n c i ",
            "    t    "
        };
    }

    /**
     * Renders summary and navigation actions for trade menus.
     *
     * @param render render context
     * @param player viewing player
     */
    @Override
    public void onFirstRender(
        final @NotNull RenderContext render,
        final @NotNull Player player
    ) {
        final RDR plugin = this.rdr.get(render);
        final TradeService tradeService = plugin.getTradeService();
        final int pendingInvites = tradeService == null ? 0 : tradeService.findPendingInvites(player.getUniqueId()).size();
        final int activeSessions = tradeService == null
            ? 0
            : (int) tradeService.findNonTerminalSessions(player.getUniqueId()).stream()
                .filter(session -> session.getStatus() != com.raindropcentral.rdr.database.entity.TradeSessionStatus.INVITED)
                .count();
        final int pendingDeliveries = tradeService == null ? 0 : tradeService.findPendingDeliveries(player.getUniqueId()).size();

        render.layoutSlot('s', this.createSummaryItem(player, pendingInvites, activeSessions, pendingDeliveries));
        render.layoutSlot('n', this.createNewTradeItem(player))
            .onClick(clickContext -> clickContext.openForPlayer(
                TradeTargetSelectView.class,
                Map.of("plugin", plugin)
            ));
        render.layoutSlot('c', this.createCurrentTradesItem(player))
            .onClick(clickContext -> clickContext.openForPlayer(
                TradeCurrentTradesView.class,
                Map.of("plugin", plugin)
            ));
        render.layoutSlot('i', this.createInboxItem(player))
            .onClick(clickContext -> clickContext.openForPlayer(
                TradeInboxView.class,
                Map.of("plugin", plugin)
            ));
        render.layoutSlot('t', this.createTaxInfoItem(player))
            .onClick(clickContext -> clickContext.openForPlayer(
                TradeTaxView.class,
                Map.of("plugin", plugin)
            ));
    }

    /**
     * Cancels default inventory interaction for this menu.
     *
     * @param click click context
     */
    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final int pendingInvites,
        final int activeSessions,
        final int pendingDeliveries
    ) {
        return UnifiedBuilderFactory.item(Material.WRITABLE_BOOK)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "pending_invites", pendingInvites,
                    "active_sessions", activeSessions,
                    "pending_deliveries", pendingDeliveries
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createNewTradeItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.NAME_TAG)
            .setName(this.i18n("actions.new_trade.name", player).build().component())
            .setLore(this.i18n("actions.new_trade.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createCurrentTradesItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.CLOCK)
            .setName(this.i18n("actions.current_trades.name", player).build().component())
            .setLore(this.i18n("actions.current_trades.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createInboxItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.CHEST)
            .setName(this.i18n("actions.inbox.name", player).build().component())
            .setLore(this.i18n("actions.inbox.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createTaxInfoItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.PAPER)
            .setName(this.i18n("actions.tax_info.name", player).build().component())
            .setLore(this.i18n("actions.tax_info.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
}
