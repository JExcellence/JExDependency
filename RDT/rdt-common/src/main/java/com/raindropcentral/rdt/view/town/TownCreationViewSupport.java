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
import com.raindropcentral.rdt.service.TownCreationProgressSnapshot;
import me.devnatan.inventoryframework.context.Context;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared initial-data and snapshot helpers for the dedicated town-creation flow.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
final class TownCreationViewSupport {

    static final String ENTRY_KEY = "creation_entry_key";
    static final String CONTRIBUTION_STATUS_KEY = "creation_contribution_status";
    static final String CONTRIBUTION_AMOUNT_KEY = "creation_contribution_amount";
    static final String CONTRIBUTION_COMPLETED_KEY = "creation_requirement_completed";
    static final String READY_TO_CREATE_KEY = "creation_ready";

    private TownCreationViewSupport() {
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
        final Map<String, Object> sanitizedData = new LinkedHashMap<>(data);
        sanitizedData.remove(ENTRY_KEY);
        sanitizedData.remove(CONTRIBUTION_STATUS_KEY);
        sanitizedData.remove(CONTRIBUTION_AMOUNT_KEY);
        sanitizedData.remove(CONTRIBUTION_COMPLETED_KEY);
        sanitizedData.remove(READY_TO_CREATE_KEY);
        return sanitizedData;
    }

    static @Nullable RDT plugin(final @NotNull Context context) {
        final Map<String, Object> data = copyInitialData(context);
        return data != null && data.get("plugin") instanceof RDT plugin ? plugin : null;
    }

    static @Nullable TownCreationProgressSnapshot resolveSnapshot(final @NotNull Context context) {
        final RDT plugin = plugin(context);
        return plugin == null || plugin.getTownRuntimeService() == null
            ? null
            : plugin.getTownRuntimeService().getTownCreationProgress(context.getPlayer());
    }
}
