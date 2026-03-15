package com.raindropcentral.rdt.database.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests bank and town-level progress bookkeeping on {@link RTown}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class RTownBankAndLevelProgressTest {

    @Test
    void depositAndWithdrawMaintainNonNegativeBankState() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Alpha", null);

        assertEquals(0.0D, town.getBankAmount("vault"), 0.000_1D);
        assertEquals(100.0D, town.depositBank(" Vault ", 100.0D), 0.000_1D);
        assertEquals(125.5D, town.depositBank("vault", 25.5D), 0.000_1D);
        assertEquals(125.5D, town.getBankAmount("vault"), 0.000_1D);

        assertFalse(town.withdrawBank("vault", 130.0D));
        assertEquals(125.5D, town.getBankAmount("vault"), 0.000_1D);

        assertTrue(town.withdrawBank("vault", 25.5D));
        assertEquals(100.0D, town.getBankAmount("vault"), 0.000_1D);
        assertTrue(town.withdrawBank("vault", 100.0D));
        assertEquals(0.0D, town.getBankAmount("vault"), 0.000_1D);
        assertEquals(0, town.getBankCurrencyCount());
    }

    @Test
    void levelCurrencyProgressIsCreatedAndRemovedWhenEmpty() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Alpha", null);

        town.setLevelCurrencyProgress(" level.2.currency ", 75.0D);
        assertEquals(75.0D, town.getLevelCurrencyProgress("level.2.currency"), 0.000_1D);
        assertEquals(1, town.getLevelCurrencyProgress().size());

        town.setLevelCurrencyProgress("level.2.currency", -1.0D);
        assertEquals(0.0D, town.getLevelCurrencyProgress("level.2.currency"), 0.000_1D);
        assertTrue(town.getLevelCurrencyProgress().isEmpty());
    }

    @Test
    void levelItemProgressCanBeClearedWithoutAStoredItem() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Alpha", null);

        town.setLevelItemProgress("level.2.item", null);
        assertNull(town.getLevelItemProgress("level.2.item"));
        assertTrue(town.getLevelItemProgress().isEmpty());
    }
}
