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
import com.raindropcentral.rdt.service.NationBankService;
import com.raindropcentral.rdt.service.TownBankService;
import com.raindropcentral.rplatform.view.AbstractAnvilView;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.state.State;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Anvil input flow for nation-bank deposits and withdrawals.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class NationBankCurrencyInputView extends AbstractAnvilView {

    private final State<RDT> plugin = initialState("plugin");
    private final State<UUID> nationUuid = initialState("nation_uuid");
    private final State<String> currencyId = initialState(NationBankViewSupport.CURRENCY_ID_KEY);
    private final State<String> currencyAction = initialState(NationBankViewSupport.CURRENCY_ACTION_KEY);

    /**
     * Creates the nation-bank currency amount input view.
     */
    public NationBankCurrencyInputView() {
        super(NationBankRootView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "nation_bank_currency_input_ui";
    }

    @Override
    protected @NotNull String getTitleKey() {
        return "title";
    }

    @Override
    public void onOpen(final @NotNull OpenContext open) {
        final RDT plugin = this.plugin.get(open);
        final UUID resolvedNationUuid = this.nationUuid.get(open);
        if (plugin.getNationBankService() != null
            && resolvedNationUuid != null
            && !plugin.getNationBankService().acquireBankAccess(resolvedNationUuid, open.getPlayer().getUniqueId())) {
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
    protected String getInitialInputText(final @NotNull OpenContext context) {
        return "";
    }

    @Override
    protected @Nullable Object processInput(final @NotNull String input, final @NotNull Context context) {
        final Double amount = this.parseAmount(input);
        if (amount == null) {
            return new TownBankService.CurrencyTransactionResult(
                TownBankService.CurrencyTransactionStatus.INVALID_AMOUNT,
                this.currencyId.get(context) == null ? "" : this.currencyId.get(context),
                0.0D,
                0.0D
            );
        }

        final RDT plugin = this.plugin.get(context);
        final NationBankService bankService = plugin.getNationBankService();
        final UUID resolvedNationUuid = this.nationUuid.get(context);
        final String resolvedCurrencyId = this.currencyId.get(context);
        if (bankService == null || resolvedNationUuid == null || resolvedCurrencyId == null) {
            return new TownBankService.CurrencyTransactionResult(
                TownBankService.CurrencyTransactionStatus.INVALID_TARGET,
                resolvedCurrencyId == null ? "" : resolvedCurrencyId,
                0.0D,
                0.0D
            );
        }

        return this.resolveAction(context) == NationBankCurrencyAction.WITHDRAW
            ? bankService.withdrawCurrency(context.getPlayer(), resolvedNationUuid, resolvedCurrencyId, amount)
            : bankService.depositCurrency(context.getPlayer(), resolvedNationUuid, resolvedCurrencyId, amount);
    }

    @Override
    protected boolean isValidInput(final @NotNull String input, final @NotNull Context context) {
        final Double amount = this.parseAmount(input);
        return amount != null && amount > 0.0D;
    }

    @Override
    protected @NotNull Map<String, Object> prepareResultData(
        final @Nullable Object result,
        final @NotNull String input,
        final @NotNull Context context
    ) {
        final TownBankService.CurrencyTransactionResult transactionResult = result instanceof TownBankService.CurrencyTransactionResult resolvedResult
            ? resolvedResult
            : new TownBankService.CurrencyTransactionResult(
                TownBankService.CurrencyTransactionStatus.INVALID_TARGET,
                this.currencyId.get(context) == null ? "" : this.currencyId.get(context),
                0.0D,
                0.0D
            );

        final Map<String, Object> resultData = new LinkedHashMap<>();
        final Map<String, Object> copiedData = NationBankViewSupport.copyInitialData(context);
        if (copiedData != null) {
            resultData.putAll(NationBankViewSupport.stripTransientData(copiedData));
        }
        resultData.put(NationBankViewSupport.CURRENCY_ID_KEY, transactionResult.currencyId());
        resultData.put(NationBankViewSupport.TRANSACTION_STATUS_KEY, transactionResult.status().name());
        resultData.put(NationBankViewSupport.TRANSACTION_AMOUNT_KEY, transactionResult.amount());
        resultData.put(NationBankViewSupport.TRANSACTION_BALANCE_KEY, transactionResult.newTownBalance());
        return resultData;
    }

    private @NotNull NationBankCurrencyAction resolveAction(final @NotNull Context context) {
        final String rawAction = this.currencyAction.get(context);
        if (rawAction == null) {
            return NationBankCurrencyAction.DEPOSIT;
        }
        try {
            return NationBankCurrencyAction.valueOf(rawAction.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (final IllegalArgumentException ignored) {
            return NationBankCurrencyAction.DEPOSIT;
        }
    }

    private @Nullable Double parseAmount(final @NotNull String input) {
        try {
            return Double.parseDouble(input.trim());
        } catch (final NumberFormatException ignored) {
            return null;
        }
    }
}
