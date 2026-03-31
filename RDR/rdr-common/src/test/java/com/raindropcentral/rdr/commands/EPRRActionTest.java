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

package com.raindropcentral.rdr.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link EPRRAction} command action definitions.
 */
class EPRRActionTest {

    @Test
    void exposesExpectedActionsInStableOrder() {
        assertArrayEquals(
            new EPRRAction[]{
                EPRRAction.ADMIN,
                EPRRAction.INFO,
                EPRRAction.SCOREBOARD,
                EPRRAction.STORAGE,
                EPRRAction.TRADE,
                EPRRAction.TAXES
            },
            EPRRAction.values()
        );
    }

    @Test
    void resolvesEnumValueByCanonicalName() {
        assertEquals(EPRRAction.STORAGE, EPRRAction.valueOf("STORAGE"));
    }
}
