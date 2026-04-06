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
import me.devnatan.inventoryframework.context.Context;
import me.devnatan.inventoryframework.context.OpenContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Second anvil step of the GUI-only town-creation flow.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public class CreateTownColorAnvilView extends AbstractTownCreationAnvilView {

    private static final String DEFAULT_TOWN_COLOR_INPUT = "color";

    /**
     * Creates the town-color input view.
     */
    public CreateTownColorAnvilView() {
        super();
    }

    /**
     * Returns the translation namespace for this view.
     *
     * @return translation key prefix
     */
    @Override
    protected @NotNull String getKey() {
        return "town_create_color_ui";
    }

    /**
     * Returns the title suffix used by {@link com.raindropcentral.rplatform.view.AbstractAnvilView}.
     *
     * @return localized title suffix
     */
    @Override
    protected @NotNull String getTitleKey() {
        return "title";
    }

    /**
     * Processes the entered town color.
     *
     * @param input player input
     * @param context current context
     * @return canonical town color
     */
    @Override
    protected Object processInput(final @NotNull String input, final @NotNull Context context) {
        return TownColorUtil.parseTownColor(input);
    }

    /**
     * Returns whether the entered town color is valid.
     *
     * @param input input to validate
     * @param context current context
     * @return {@code true} when the color is valid
     */
    @Override
    protected boolean isValidInput(final @NotNull String input, final @NotNull Context context) {
        try {
            TownColorUtil.parseTownColor(input);
            return true;
        } catch (final IllegalArgumentException ignored) {
            return false;
        }
    }

    /**
     * Returns the default input shown in the anvil.
     *
     * @param context open context
     * @return initial input text
     */
    @Override
    protected String getInitialInputText(final @NotNull OpenContext context) {
        return DEFAULT_TOWN_COLOR_INPUT;
    }

    /**
     * Flattens the anvil result into the parent view data map.
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
        resultData.put("draftTownColor", result == null ? TownColorUtil.parseTownColor(input) : result);
        return resultData;
    }
}
