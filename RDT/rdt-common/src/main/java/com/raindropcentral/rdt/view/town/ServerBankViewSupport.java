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

import me.devnatan.inventoryframework.context.Context;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared initial-data helpers for server-bank views.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
final class ServerBankViewSupport {

    static final String CURRENCY_ID_KEY = "server_bank_currency_id";
    static final String CURRENCY_ACTION_KEY = "server_bank_currency_action";
    static final String TRANSACTION_STATUS_KEY = "server_bank_transaction_status";
    static final String TRANSACTION_AMOUNT_KEY = "server_bank_transaction_amount";
    static final String TRANSACTION_BALANCE_KEY = "server_bank_transaction_balance";

    private ServerBankViewSupport() {
    }

    static @Nullable Map<String, Object> copyInitialData(final @NotNull Context context) {
        final Object initialData = context.getInitialData();
        if (!(initialData instanceof Map<?, ?> rawMap)) {
            return null;
        }

        final Map<String, Object> copied = new LinkedHashMap<>();
        for (final Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                copied.put(key, entry.getValue());
            }
        }
        return copied;
    }

    static @NotNull Map<String, Object> mergeInitialData(
        final @NotNull Context context,
        final @NotNull Map<String, Object> extraData
    ) {
        final Map<String, Object> copiedData = copyInitialData(context);
        final Map<String, Object> mergedData = copiedData == null ? new LinkedHashMap<>() : new LinkedHashMap<>(copiedData);
        mergedData.putAll(extraData);
        return mergedData;
    }

    static @NotNull Map<String, Object> stripTransientData(final @NotNull Map<String, Object> data) {
        final Map<String, Object> sanitized = new LinkedHashMap<>(data);
        sanitized.remove(CURRENCY_ID_KEY);
        sanitized.remove(CURRENCY_ACTION_KEY);
        sanitized.remove(TRANSACTION_STATUS_KEY);
        sanitized.remove(TRANSACTION_AMOUNT_KEY);
        sanitized.remove(TRANSACTION_BALANCE_KEY);
        return sanitized;
    }
}

enum ServerBankCurrencyAction {
    DEPOSIT,
    WITHDRAW
}
