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
import me.devnatan.inventoryframework.context.OpenContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Anvil input view for nation renaming.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class NationRenameAnvilView extends AbstractTownNationAnvilView {

    /**
     * Creates the rename input view.
     */
    public NationRenameAnvilView() {
        super();
    }

    @Override
    protected @NotNull String getKey() {
        return "town_nation_rename_ui";
    }

    @Override
    protected @NotNull String getTitleKey() {
        return "title";
    }

    @Override
    protected String getInitialInputText(final @NotNull OpenContext context) {
        final Object initialData = context.getInitialData();
        if (initialData instanceof Map<?, ?> rawMap && rawMap.get("current_nation_name") instanceof String currentNationName) {
            return currentNationName;
        }
        return "Nation Name";
    }

    @Override
    protected @Nullable Object processInput(final @NotNull String input, final @NotNull Context context) {
        return input.trim();
    }

    @Override
    protected boolean isValidInput(final @NotNull String input, final @NotNull Context context) {
        final String normalized = input.trim();
        return !normalized.isEmpty() && normalized.length() <= 32;
    }

    @Override
    protected @NotNull Map<String, Object> prepareResultData(
        final @Nullable Object result,
        final @NotNull String input,
        final @NotNull Context context
    ) {
        final Map<String, Object> resultData = new LinkedHashMap<>();
        final Map<String, Object> copiedData = TownNationViewSupport.copyInitialData(context);
        if (copiedData != null) {
            resultData.putAll(copiedData);
        }
        resultData.put(
            TownNationViewSupport.RENAMED_NATION_NAME_KEY,
            result == null ? input.trim() : result
        );
        return resultData;
    }
}
