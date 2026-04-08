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

import me.devnatan.inventoryframework.context.OpenContext;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests anvil input defaults for {@link TownLevelCurrencyContributionAnvilView}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class TownLevelCurrencyContributionAnvilViewTest {

    @Test
    void initialInputIsBlankByDefault() {
        final TownLevelCurrencyContributionAnvilView view = new TownLevelCurrencyContributionAnvilView();
        final OpenContext openContext = Mockito.mock(OpenContext.class);

        assertEquals("", view.getInitialInputText(openContext));
    }
}
