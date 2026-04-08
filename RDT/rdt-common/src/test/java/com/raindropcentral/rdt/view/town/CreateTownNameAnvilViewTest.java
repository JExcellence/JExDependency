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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests name-only result handling for {@link CreateTownNameAnvilView}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class CreateTownNameAnvilViewTest {

    @Test
    void prepareResultDataCarriesTownNameWithoutColorDraft() {
        final CreateTownNameAnvilView view = new CreateTownNameAnvilView();
        final Context context = Mockito.mock(Context.class);
        Mockito.when(context.getInitialData()).thenReturn(Map.of("plugin", "plugin-ref"));

        final Map<String, Object> resultData = view.prepareResultData("Founders", "Founders", context);

        assertEquals("plugin-ref", resultData.get("plugin"));
        assertEquals("Founders", resultData.get("draftTownName"));
        assertFalse(resultData.containsKey("draftTownColor"));
    }

    @Test
    void validationAcceptsReasonableTownNamesOnly() {
        final CreateTownNameAnvilView view = new CreateTownNameAnvilView();
        final Context context = Mockito.mock(Context.class);

        assertTrue(view.isValidInput("Founders", context));
        assertFalse(view.isValidInput("   ", context));
        assertFalse(view.isValidInput("ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567", context));
    }
}
