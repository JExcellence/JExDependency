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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class CreateTownColorAnvilViewTest {

    @Mock
    private OpenContext openContext;

    @Mock
    private Context context;

    @Test
    void initialInputUsesInvalidPlaceholder() {
        final CreateTownColorAnvilView view = new CreateTownColorAnvilView();

        assertEquals("color", view.getInitialInputText(this.openContext));
    }

    @Test
    void placeholderInputIsRejected() {
        final CreateTownColorAnvilView view = new CreateTownColorAnvilView();

        assertFalse(view.isValidInput("color", this.context));
    }

    @Test
    void lightBlueAliasRemainsValidInput() {
        final CreateTownColorAnvilView view = new CreateTownColorAnvilView();

        assertTrue(view.isValidInput("LIGHT_BLUE", this.context));
        assertEquals("#55FFFF", view.processInput("LIGHT_BLUE", this.context));
    }
}
