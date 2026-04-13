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
import com.raindropcentral.rdt.database.entity.RNation;
import com.raindropcentral.rdt.service.NationBankService;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Actionable root view for nation-bank currency storage and shared item storage.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class NationBankRootView extends BaseView {

    private static final List<Integer> CURRENCY_SLOTS = List.of(10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25);

    private final State<RDT> plugin = initialState("plugin");
    private final State<UUID> nationUuid = initialState("nation_uuid");

    /**
     * Creates the nation-bank root view.
     */
    public NationBankRootView() {
        super(TownNationView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "nation_bank_root_ui";
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
        final UUID resolvedNationUuid = this.nationUuid.get(open);
        final NationBankService bankService = plugin.getNationBankService();
        final RNation nation = resolvedNationUuid == null || bankService == null ? null : bankService.getNation(resolvedNationUuid);
        if (bankService == null || nation == null || !bankService.canViewBank(open.getPlayer(), nation)) {
            new I18n.Builder("nation_bank_shared.messages.no_permission", open.getPlayer())
                .includePrefix()
                .build()
                .sendMessage();
            open.getPlayer().closeInventory();
            return;
        }
        if (!bankService.acquireBankAccess(resolvedNationUuid, open.getPlayer().getUniqueId())) {
            new I18n.Builder("nation_bank_shared.messages.in_use", open.getPlayer())
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
        final UUID resolvedNationUuid = this.nationUuid.get(close);
        if (plugin.getNationBankService() != null && resolvedNationUuid != null) {
            plugin.getNationBankService().releaseBankAccess(resolvedNationUuid, close.getPlayer().getUniqueId());
        }
        super.onClose(close);
    }

    @Override
    public void onResume(final @NotNull Context origin, final @NotNull Context target) {
        final Map<String, Object> data = NationBankViewSupport.copyInitialData(target) != null
            ? NationBankViewSupport.copyInitialData(target)
            : NationBankViewSupport.copyInitialData(origin);
        if (data == null || !(data.get(NationBankViewSupport.TRANSACTION_STATUS_KEY) instanceof String statusName)) {
            return;
        }

        final Player player = target.getPlayer();
        final String currencyId = data.get(NationBankViewSupport.CURRENCY_ID_KEY) instanceof String rawCurrencyId ? rawCurrencyId : "";
        final Object amount = data.getOrDefault(NationBankViewSupport.TRANSACTION_AMOUNT_KEY, 0.0D);
        final Object balance = data.getOrDefault(NationBankViewSupport.TRANSACTION_BALANCE_KEY, 0.0D);
        new I18n.Builder(this.getKey() + ".transaction." + statusName.toLowerCase(), player)
            .includePrefix()
            .withPlaceholders(Map.of(
                "currency", currencyId,
                "amount", amount,
                "nation_balance", balance
            ))
            .build()
            .sendMessage();
    }

    @Override
    public void onFirstRender(final @NotNull RenderContext render, final @NotNull Player player) {
        final RNation nation = this.resolveNation(render);
        final NationBankService bankService = this.plugin.get(render).getNationBankService();
        if (nation == null || bankService == null) {
            render.slot(22).renderWith(() -> this.createMissingNationItem(player));
            return;
        }

        render.slot(4).renderWith(() -> this.createSummaryItem(player, nation, bankService));
        this.renderCurrencyEntries(render, player, nation, bankService);
        render.slot(31)
            .renderWith(() -> this.createSharedStorageItem(player, nation))
            .onClick(click -> click.openForPlayer(
                NationBankStorageView.class,
                NationBankViewSupport.mergeInitialData(click, Map.of(
                    "plugin", this.plugin.get(click),
                    "nation_uuid", nation.getNationUuid()
                ))
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
        final @NotNull RNation nation,
        final @NotNull NationBankService bankService
    ) {
        int slotIndex = 0;
        for (final String currencyId : bankService.getConfiguredCurrencies()) {
            if (slotIndex >= CURRENCY_SLOTS.size()) {
                break;
            }
            final int slot = CURRENCY_SLOTS.get(slotIndex++);
            render.slot(slot)
                .renderWith(() -> this.createCurrencyItem(player, nation, bankService, currencyId))
                .onClick(click -> this.handleCurrencyClick(click, nation, currencyId));
        }
    }

    private void handleCurrencyClick(
        final @NotNull SlotClickContext click,
        final @NotNull RNation nation,
        final @NotNull String currencyId
    ) {
        final NationBankCurrencyAction action = click.isRightClick() ? NationBankCurrencyAction.WITHDRAW : NationBankCurrencyAction.DEPOSIT;
        click.openForPlayer(
            NationBankCurrencyInputView.class,
            NationBankViewSupport.mergeInitialData(click, Map.of(
                "plugin", this.plugin.get(click),
                "nation_uuid", nation.getNationUuid(),
                NationBankViewSupport.CURRENCY_ID_KEY, currencyId,
                NationBankViewSupport.CURRENCY_ACTION_KEY, action.name()
            ))
        );
    }

    private @Nullable RNation resolveNation(final @NotNull Context context) {
        final UUID resolvedNationUuid = this.nationUuid.get(context);
        return resolvedNationUuid == null || this.plugin.get(context).getNationBankService() == null
            ? null
            : this.plugin.get(context).getNationBankService().getNation(resolvedNationUuid);
    }

    private @NotNull ItemStack createSummaryItem(
        final @NotNull Player player,
        final @NotNull RNation nation,
        final @NotNull NationBankService bankService
    ) {
        return UnifiedBuilderFactory.item(Material.CHEST)
            .setName(this.i18n("summary.name", player).build().component())
            .setLore(this.i18n("summary.lore", player)
                .withPlaceholders(Map.of(
                    "nation_name", nation.getNationName(),
                    "nation_level", nation.getNationLevel(),
                    "viewer_lock", bankService.isBankAccessLocked(nation.getNationUuid()) ? "Locked" : "Open"
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createCurrencyItem(
        final @NotNull Player player,
        final @NotNull RNation nation,
        final @NotNull NationBankService bankService,
        final @NotNull String currencyId
    ) {
        final boolean supported = bankService.isSupportedCurrency(currencyId);
        final boolean canDeposit = bankService.canDepositCurrency(player, nation);
        final boolean canWithdraw = bankService.canWithdrawCurrency(player, nation);
        return UnifiedBuilderFactory.item(supported ? Material.GOLD_INGOT : Material.BARRIER)
            .setName(this.i18n("currency.name", player)
                .withPlaceholder("currency", bankService.resolveCurrencyDisplayName(currencyId))
                .build()
                .component())
            .setLore(this.i18n("currency.lore", player)
                .withPlaceholders(Map.of(
                    "currency", bankService.resolveCurrencyDisplayName(currencyId),
                    "nation_balance", bankService.getNationCurrencyBalance(nation, currencyId),
                    "player_balance", bankService.getPlayerCurrencyBalance(player, currencyId),
                    "deposit_state", canDeposit ? "Enabled" : "No Access",
                    "withdraw_state", canWithdraw ? "Enabled" : "No Access",
                    "support_state", supported ? "Supported" : "Unavailable"
                ))
                .build()
                .children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }

    private @NotNull ItemStack createSharedStorageItem(final @NotNull Player player, final @NotNull RNation nation) {
        return UnifiedBuilderFactory.item(Material.BARREL)
            .setName(this.i18n("storage.name", player).build().component())
            .setLore(this.i18n("storage.lore", player)
                .withPlaceholders(Map.of(
                    "stored_slots", nation.getSharedBankStorage().size()
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

    private @NotNull ItemStack createMissingNationItem(final @NotNull Player player) {
        return UnifiedBuilderFactory.item(Material.BARRIER)
            .setName(this.i18n("missing.name", player).build().component())
            .setLore(this.i18n("missing.lore", player).build().children())
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .build();
    }
}
