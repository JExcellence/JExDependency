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

import com.raindropcentral.rdt.utils.TownColorUtil;
import com.raindropcentral.rplatform.view.AbstractAnvilView;
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Nexus-governed anvil input view for updating a town's stored color.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public final class TownColorAnvilView extends AbstractAnvilView {

    /**
     * Creates the town-color anvil view.
     */
    public TownColorAnvilView() {
        super(TownOverviewView.class);
    }

    @Override
    protected @NotNull String getKey() {
        return "town_color_ui";
    }

    @Override
    protected @NotNull String getTitleKey() {
        return "title";
    }

    @Override
    protected @Nullable Object processInput(final @NotNull String input, final @NotNull Context context) {
        return TownColorUtil.parseTownColor(input);
    }

    @Override
    protected boolean isValidInput(final @NotNull String input, final @NotNull Context context) {
        try {
            TownColorUtil.parseTownColor(input);
            return true;
        } catch (final IllegalArgumentException ignored) {
            return false;
        }
    }

    @Override
    protected String getInitialInputText(final @NotNull OpenContext context) {
        final Object initialData = context.getInitialData();
        if (initialData instanceof Map<?, ?> rawMap && rawMap.get("current_town_color") instanceof String currentTownColor) {
            return currentTownColor;
        }
        return "#55CDFC";
    }

    @Override
    protected @NotNull Map<String, Object> prepareResultData(
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
        resultData.put("updated_town_color", result == null ? TownColorUtil.parseTownColor(input) : result);
        return resultData;
    }
}
