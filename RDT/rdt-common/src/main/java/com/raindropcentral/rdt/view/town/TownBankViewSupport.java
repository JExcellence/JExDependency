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
import com.raindropcentral.rdt.database.entity.RTown;
import com.raindropcentral.rdt.database.entity.RTownChunk;
import me.devnatan.inventoryframework.context.Context;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shared initial-data helpers for actionable town bank views.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
final class TownBankViewSupport {

    static final String STORAGE_MODE_KEY = "bank_storage_mode";
    static final String CURRENCY_ID_KEY = "bank_currency_id";
    static final String CURRENCY_ACTION_KEY = "bank_currency_action";
    static final String TRANSACTION_STATUS_KEY = "bank_transaction_status";
    static final String TRANSACTION_AMOUNT_KEY = "bank_transaction_amount";
    static final String TRANSACTION_BALANCE_KEY = "bank_transaction_balance";
    static final String REMOTE_PROXY_BACKED_KEY = "bank_remote_proxy_backed";
    static final String REMOTE_HOST_SERVER_ID_KEY = "bank_remote_host_server_id";

    private TownBankViewSupport() {
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
        final Map<String, Object> mergedData = copiedData == null
            ? new LinkedHashMap<>()
            : new LinkedHashMap<>(copiedData);
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
        sanitized.remove(REMOTE_PROXY_BACKED_KEY);
        sanitized.remove(REMOTE_HOST_SERVER_ID_KEY);
        return sanitized;
    }

    static @Nullable RDT plugin(final @NotNull Context context) {
        final Map<String, Object> data = copyInitialData(context);
        return data != null && data.get("plugin") instanceof RDT plugin ? plugin : null;
    }

    static @Nullable RTown resolveTown(final @NotNull Context context) {
        final RDT plugin = plugin(context);
        if (plugin == null || plugin.getTownBankService() == null) {
            return null;
        }

        final Map<String, Object> data = copyInitialData(context);
        if (data == null || !(data.get("town_uuid") instanceof UUID townUuid)) {
            return null;
        }
        return plugin.getTownBankService().getTown(townUuid);
    }

    static @Nullable RTownChunk resolveChunk(final @NotNull Context context) {
        final RDT plugin = plugin(context);
        if (plugin == null || plugin.getTownRuntimeService() == null) {
            return null;
        }

        final Map<String, Object> data = copyInitialData(context);
        if (data == null
            || !(data.get("town_uuid") instanceof UUID townUuid)
            || !(data.get("world_name") instanceof String worldName)
            || !(data.get("chunk_x") instanceof Number chunkX)
            || !(data.get("chunk_z") instanceof Number chunkZ)) {
            return null;
        }
        return plugin.getTownRuntimeService().getTownChunk(townUuid, worldName, chunkX.intValue(), chunkZ.intValue());
    }

    static @Nullable UUID resolveTownUuid(final @NotNull Context context) {
        final Map<String, Object> data = copyInitialData(context);
        return data != null && data.get("town_uuid") instanceof UUID townUuid ? townUuid : null;
    }
}

enum TownBankStorageMode {
    SHARED_STORAGE,
    LOCAL_CACHE,
    REMOTE_CACHE_DEPOSIT
}

enum TownBankCurrencyAction {
    DEPOSIT,
    WITHDRAW
}
