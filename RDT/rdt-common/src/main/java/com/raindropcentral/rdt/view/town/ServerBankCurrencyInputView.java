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
import com.raindropcentral.rdt.service.TownBankService;
import com.raindropcentral.rplatform.view.AbstractAnvilView;
import me.devnatan.inventoryframework.context.CloseContext;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import me.devnatan.inventoryframework.state.State;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Anvil input flow for admin server-bank deposits and withdrawals.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class ServerBankCurrencyInputView extends AbstractAnvilView {

    private final State<RDT> plugin = initialState("plugin");
    private final State<String> currencyId = initialState(ServerBankViewSupport.CURRENCY_ID_KEY);
    private final State<String> currencyAction = initialState(ServerBankViewSupport.CURRENCY_ACTION_KEY);

    /**
     * Creates the server-bank currency amount input view.
     */
    public ServerBankCurrencyInputView() {
        super(ServerBankRootView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "server_bank_currency_input_ui";
    }

    @Override
    protected @NotNull String getTitleKey() {
        return "title";
    }

    @Override
    public void onOpen(final @NotNull OpenContext open) {
        final ServerBankService bankService = this.plugin.get(open).getServerBankService();
        if (bankService != null && !bankService.acquireBankAccess(open.getPlayer().getUniqueId())) {
            open.getPlayer().closeInventory();
            return;
        }
        super.onOpen(open);
    }

    @Override
    public void onClose(final @NotNull CloseContext close) {
        final ServerBankService bankService = this.plugin.get(close).getServerBankService();
        if (bankService != null) {
            bankService.releaseBankAccess(close.getPlayer().getUniqueId());
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

        final ServerBankService bankService = this.plugin.get(context).getServerBankService();
        final String resolvedCurrencyId = this.currencyId.get(context);
        if (bankService == null || resolvedCurrencyId == null) {
            return new TownBankService.CurrencyTransactionResult(
                TownBankService.CurrencyTransactionStatus.INVALID_TARGET,
                resolvedCurrencyId == null ? "" : resolvedCurrencyId,
                0.0D,
                0.0D
            );
        }

        return this.resolveAction(context) == ServerBankCurrencyAction.WITHDRAW
            ? bankService.withdrawCurrency(context.getPlayer(), resolvedCurrencyId, amount)
            : bankService.depositCurrency(context.getPlayer(), resolvedCurrencyId, amount);
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
        final Map<String, Object> copiedData = ServerBankViewSupport.copyInitialData(context);
        if (copiedData != null) {
            resultData.putAll(ServerBankViewSupport.stripTransientData(copiedData));
        }
        resultData.put(ServerBankViewSupport.CURRENCY_ID_KEY, transactionResult.currencyId());
        resultData.put(ServerBankViewSupport.TRANSACTION_STATUS_KEY, transactionResult.status().name());
        resultData.put(ServerBankViewSupport.TRANSACTION_AMOUNT_KEY, transactionResult.amount());
        resultData.put(ServerBankViewSupport.TRANSACTION_BALANCE_KEY, transactionResult.newTownBalance());
        return resultData;
    }

    private @NotNull ServerBankCurrencyAction resolveAction(final @NotNull Context context) {
        final String rawAction = this.currencyAction.get(context);
        if (rawAction == null) {
            return ServerBankCurrencyAction.DEPOSIT;
        }
        try {
            return ServerBankCurrencyAction.valueOf(rawAction.trim().toUpperCase(Locale.ROOT));
        } catch (final IllegalArgumentException ignored) {
            return ServerBankCurrencyAction.DEPOSIT;
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
