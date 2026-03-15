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
