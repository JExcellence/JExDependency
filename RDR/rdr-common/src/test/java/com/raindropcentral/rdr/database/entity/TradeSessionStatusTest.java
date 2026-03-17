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

package com.raindropcentral.rdr.database.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests terminal-state semantics for {@link TradeSessionStatus}.
 */
class TradeSessionStatusTest {

    @Test
    void marksOnlyCompletedCanceledAndExpiredAsTerminal() {
        assertFalse(TradeSessionStatus.INVITED.isTerminal());
        assertFalse(TradeSessionStatus.ACTIVE.isTerminal());
        assertFalse(TradeSessionStatus.COMPLETING.isTerminal());
        assertTrue(TradeSessionStatus.COMPLETED.isTerminal());
        assertTrue(TradeSessionStatus.CANCELED.isTerminal());
        assertTrue(TradeSessionStatus.EXPIRED.isTerminal());
    }
}
