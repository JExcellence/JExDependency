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
import com.raindropcentral.rdt.service.ServerBankService;
import com.raindropcentral.rdt.service.TaxRuntimeService;
import com.raindropcentral.rdt.view.main.TownHubView;
import com.raindropcentral.rplatform.utility.unified.UnifiedBuilderFactory;
import com.raindropcentral.rplatform.view.BaseView;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.context.RenderContext;
import me.devnatan.inventoryframework.context.SlotClickContext;
import me.devnatan.inventoryframework.state.State;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Actionable root view for the admin-only server bank.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class ServerBankRootView extends BaseView {

    private static final List<Integer> CURRENCY_SLOTS = List.of(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25);

    private final State<RDT> plugin = initialState("plugin");

    /**
     * Creates the server-bank root view.
     */
    public ServerBankRootView() {
        super(TownHubView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "server_bank_root_ui";
    }

    @Override
    protected String[] getLayout() {
        return new String[]{
            "         ",
            "         ",
            "         ",
            "         ",
            "         ",
            "         "
        };
    }

    @Override
    public void onOpen(final @NotNull OpenContext open) {
        final RDT plugin = this.plugin.get(open);
        if (plugin.getServerBankService() != null && !plugin.getServerBankService().acquireBankAccess(open.getPlayer().getUniqueId())) {
            new I18n.Builder("server_bank_shared.messages.in_use", open.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            open.getPlayer().closeInventory();
            return;
        }
        super.onOpen(open);
    }

    @Override
    public void onClose(final @NotNull CloseContext close) {
        final RDT plugin = this.plugin.get(close);
        if (plugin.getServerBankService() != null) {
            plugin.getServerBankService().releaseBankAccess(close.getPlayer().getUniqueId());
        }
        super.onClose(close);
    }

    @Override
    public void onResume(final @NotNull Context origin, final @NotNull Context target) {
        final Map<String, Object> data = ServerBankViewSupport.copyInitialData(target) != null
            ? ServerBankViewSupport.copyInitialData(target)
            : ServerBankViewSupport.copyInitialData(origin);
        if (data == null || !(data.get(ServerBankViewSupport.TRANSACTION_STATUS_KEY) instanceof String statusName)) {
            return;
        }

        final Player player = target.getPlayer();
        final String currencyId = data.get(ServerBankViewSupport.CURRENCY_ID_KEY) instanceof String rawCurrencyId ? rawCurrencyId : "";
        final Object amount = data.getOrDefault(ServerBankViewSupport.TRANSACTION_AMOUNT_KEY, 0.0D);
        final Object balance = data.getOrDefault(ServerBankViewSupport.TRANSACTION_BALANCE_KEY, 0.0D);
        new I18n.Builder(this.getKey() + ".transaction." + statusName.toLowerCase(), player)
            .includePrefix()
            .withPlaceholders(Map.of(
                "currency", currencyId,
                "amount", amount,
                "server_balance", balance
            ))
            .build()
            .sendMessage();
    }

    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final ServerBankService bankService = this.plugin.get(render).getServerBankService();
        if (bankService == null) {
            render.slot(22).renderWith(() -> this.createMissingItem(player));
            return;
        }

        final TaxRuntimeService.TaxStatusSnapshot taxStatus = this.plugin.get(render).getTaxRuntimeService() == null
            ? null
            : this.plugin.get(render).getTaxRuntimeService().getStatusSnapshot();
        render.slot(4).renderWith(() -> this.createSummaryItem(player, bankService, taxStatus));
        this.renderCurrencyEntries(render, player, bankService);
        render.slot(31)
            .renderWith(() -> this.createSharedStorageItem(player, bankService))
            .onClick(click -> click.openForPlayer(
                ServerBankStorageView.class,
                ServerBankViewSupport.mergeInitialData(click, Map.of("plugin", this.plugin.get(click)))
            ));
        render.slot(45)
            .renderWith(() -> this.createReturnItem(player))
            .onClick(SlotClickContext::back);
    }

    @Override
    public void onClick(final @NotNull SlotClickContext click) {
        click.setCancelled(true);
    }

    private void renderCurrencyEntries(
        final @NotNull RenderContext render,
        final @NotNull Player player,
        final @NotNull ServerBankService bankService
    ) {
        int slotIndex = 0;
        for (final String currencyId : bankService.getConfiguredCurrencies()) {
            if (slotIndex >= CURRENCY_SLOTS.size()) {
                break;
            }
            final int slot = CURRENCY_SLOTS.get(slotIndex++);
            render.slot(slot)
                .renderWith(() -> this.createCurrencyItem(player, bankService, currencyId))
                .onClick(click -> this.handleCurrencyClick(click, currencyId));
        }
    }

    private void handleCurrencyClick(final @NotNull SlotClickContext click, final @NotNull String currencyId) {
        final ServerBankCurrencyAction action = click.isRightClick() ? ServerBankCurrencyAction.WITHDRAW : ServerBankCurrencyAction.DEPOSIT;
        click.openForPlayer(
            ServerBankCurrencyInputView.class,
            ServerBankViewSupport.mergeInitialData(click, Map.of(
                "plugin", this.plugin.get(click),
                ServerBankViewSupport.CURRENCY_ID_KEY, currencyId,
                ServerBankViewSupport.CURRENCY_ACTION_KEY, action.name()
            ))
        );
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final @NotNull ServerBankService bankService,
        final @Nullable TaxRuntimeService.TaxStatusSnapshot taxStatus
    ) {
        final Instant lastRun = taxStatus == null ? null : taxStatus.lastCollectionAt();
        final Instant nextRun = taxStatus == null ? null : taxStatus.nextScheduledRunAt();
        return UnifiedBuilderFactory.item(Material.ENDER_CHEST)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "stored_slots", bankService.getSharedStorage().size(),
                    "viewer_lock", bankService.isBankAccessLocked() ? "Locked" : "Open",
                    "last_tax_run", lastRun == null ? "Unknown" : lastRun.toString(),
                    "next_tax_run", nextRun == null ? "Unknown" : nextRun.toString()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createCurrencyItem(
        final @NotNull Player player,
        final @NotNull ServerBankService bankService,
        final @NotNull String currencyId
    ) {
        final boolean supported = bankService.isSupportedCurrency(currencyId);
        return UnifiedBuilderFactory.item(supported ? Material.GOLD_BLOCK : Material.BARRIER)
            .setName(this.i18n("currency.name", player)
                .withPlaceholder("currency", bankService.resolveCurrencyDisplayName(currencyId))
                .build()
                .component())
            .setLore(this.i18n("currency.lore", player)
                .withPlaceholders(Map.of(
                    "currency", bankService.resolveCurrencyDisplayName(currencyId),
                    "server_balance", bankService.getCurrencyBalance(currencyId),
                    "player_balance", bankService.getPlayerCurrencyBalance(player, currencyId),
                    "support_state", supported ? "Supported" : "Unavailable"
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createSharedStorageItem(final @NotNull Player player, final @NotNull ServerBankService bankService) {
        return UnifiedBuilderFactory.item(Material.BARREL)
            .setName(this.i18n("storage.name", player).build().component())
            .setLore(this.i18n("storage.lore", player)
                .withPlaceholders(Map.of(
                    "stored_slots", bankService.getSharedStorage().size()
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createReturnItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.ARROW)
            .setName(this.i18n("return.name", player).build().component())
            .setLore(this.i18n("return.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createMissingItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("missing.name", player).build().component())
            .setLore(this.i18n("missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
}
