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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests parsing and default input behavior for {@link TownColorAnvilView}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class TownColorAnvilViewTest {

    @Test
    void initialInputUsesCurrentTownColorWhenProvided() {
        final TownColorAnvilView view = new TownColorAnvilView();
        final OpenContext openContext = Mockito.mock(OpenContext.class);
        Mockito.when(openContext.getInitialData()).thenReturn(Map.of("current_town_color", "#AA11CC"));

        assertEquals("#AA11CC", view.getInitialInputText(openContext));
    }

    @Test
    void initialInputDefaultsToTownCreationColorWhenMissing() {
        final TownColorAnvilView view = new TownColorAnvilView();
        final OpenContext openContext = Mockito.mock(OpenContext.class);

        assertEquals("#55CDFC", view.getInitialInputText(openContext));
    }

    @Test
    void acceptsAndNormalizesValidTownColors() {
        final TownColorAnvilView view = new TownColorAnvilView();
        final Context context = Mockito.mock(Context.class);

        assertTrue(view.isValidInput("ff00aa", context));
        assertEquals("#FF00AA", view.processInput("ff00aa", context));
    }

    @Test
    void rejectsInvalidTownColors() {
        final TownColorAnvilView view = new TownColorAnvilView();
        final Context context = Mockito.mock(Context.class);

        assertFalse(view.isValidInput("not-a-color", context));
        assertThrows(IllegalArgumentException.class, () -> view.processInput("not-a-color", context));
    }
}
