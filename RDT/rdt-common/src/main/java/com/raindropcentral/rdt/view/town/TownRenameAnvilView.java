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

import com.raindropcentral.rplatform.view.AbstractAnvilView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Anvil input view for nexus-locked town renaming.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class TownRenameAnvilView extends AbstractAnvilView {

    /**
     * Creates the rename input view.
     */
    public TownRenameAnvilView() {
        super(TownOverviewView.class);
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_rename_ui";
    }

    /**
     * Returns the title suffix used by {@link AbstractAnvilView}.
     *
     * @return localized title suffix
     */
    @Override
    protected @NotNull String getTitleKey() {
        return "title";
    }

    /**
     * Returns the initial town name shown in the input.
     *
     * @param context open context
     * @return current town name or a blank template
     */
    @Override
    protected String getInitialInputText(final @NotNull OpenContext context) {
        final Object initialData = context.getInitialData();
        if (initialData instanceof Map<?, ?> rawMap && rawMap.get("current_town_name") instanceof String currentTownName) {
            return currentTownName;
        }
        return "Town Name";
    }

    /**
     * Processes the entered replacement town name.
     *
     * @param input player input
     * @param context current context
     * @return normalized replacement name
     */
    @Override
    protected Object processInput(final @NotNull String input, final @NotNull Context context) {
        return input.trim();
    }

    /**
     * Returns whether the entered replacement name is valid.
     *
     * @param input input to validate
     * @param context current context
     * @return {@code true} when the input is valid
     */
    @Override
    protected boolean isValidInput(final @NotNull String input, final @NotNull Context context) {
        final String normalized = input.trim();
        return !normalized.isEmpty() && normalized.length() <= 32;
    }

    /**
     * Flattens the rename result into the parent data map.
     *
     * @param result processed result
     * @param input original input
     * @param context current context
     * @return merged parent data
     */
    @Override
    protected Map<String, Object> prepareResultData(
        final @Nullable Object result,
        final @NotNull String input,
        final @NotNull Context context
    ) {
        final Map<String, Object> resultData = new LinkedHashMap<>();
        final Object initialData = context.getInitialData();
        if (initialData instanceof Map<?, ?> rawMap) {
            for (final Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    resultData.put(key, entry.getValue());
                }
            }
        }
        resultData.put("renamed_town_name", result == null ? input.trim() : result);
        return resultData;
    }
}
