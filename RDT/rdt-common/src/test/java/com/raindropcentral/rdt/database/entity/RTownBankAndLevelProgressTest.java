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

package com.raindropcentral.rdt.database.entity;

import com.raindropcentral.rdt.utils.ChunkType;
import com.raindropcentral.rdt.utils.TownProtections;
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

    @Test
    void archetypeChangeTimestampDefaultsToZeroAndClampsNegativeValues() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Alpha", null);

        assertEquals(0L, town.getLastArchetypeChangeAt());

        town.setLastArchetypeChangeAt(-50L);
        assertEquals(0L, town.getLastArchetypeChangeAt());

        town.setLastArchetypeChangeAt(1234L);
        assertEquals(1234L, town.getLastArchetypeChangeAt());
    }

    @Test
    void compositeTownLevelUsesPersistedNexusLevelPlusNonNexusChunkLevels() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Alpha", null);
        final RTownChunk nexusChunk = new RTownChunk(town, "world", 0, 0, ChunkType.NEXUS);
        final RTownChunk securityChunk = new RTownChunk(town, "world", 1, 0, ChunkType.SECURITY);
        final RTownChunk bankChunk = new RTownChunk(town, "world", 2, 0, ChunkType.BANK);

        town.addChunk(nexusChunk);
        town.addChunk(securityChunk);
        town.addChunk(bankChunk);

        town.setNexusLevel(3);
        securityChunk.setChunkLevel(4);
        bankChunk.setChunkLevel(2);

        assertEquals(9, town.getTownLevel());
        assertEquals(3, town.getNexusLevel());
    }

    @Test
    void bufferedFuelUnitsClampNegativeValuesToZero() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Alpha", null);

        town.setBufferedFuelUnits(18.5D);
        assertEquals(18.5D, town.getBufferedFuelUnits(), 0.000_1D);

        town.setBufferedFuelUnits(-10.0D);
        assertEquals(0.0D, town.getBufferedFuelUnits(), 0.000_1D);
    }

    @Test
    void alliedProtectionDefaultsToRestrictedAndFallsBackThroughParentProtections() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Alpha", null);

        assertNull(town.getConfiguredAlliedProtectionAllowed(TownProtections.ENDER_PEARL));
        assertFalse(town.isAlliedProtectionAllowed(TownProtections.ENDER_PEARL));

        town.setAlliedProtectionAllowed(TownProtections.ITEM_USE, true);

        assertTrue(town.isAlliedProtectionAllowed(TownProtections.ENDER_PEARL));
        assertTrue(town.getConfiguredAlliedProtectionAllowed(TownProtections.ITEM_USE));
    }

    @Test
    void nexusHealthBackfillsToFullAndClampsWithinBounds() {
        final RTown town = new RTown(UUID.randomUUID(), UUID.randomUUID(), "Alpha", null);

        assertFalse(town.hasPersistedCurrentNexusHealth());
        assertEquals(1000.0D, town.getCurrentNexusHealth(1000.0D), 0.000_1D);

        assertTrue(town.backfillLegacyCurrentNexusHealthIfNeeded(1500.0D));
        assertTrue(town.hasPersistedCurrentNexusHealth());
        assertEquals(1500.0D, town.getCurrentNexusHealth(1500.0D), 0.000_1D);

        town.setCurrentNexusHealth(2200.0D, 1500.0D);
        assertEquals(1500.0D, town.getCurrentNexusHealth(1500.0D), 0.000_1D);

        town.setCurrentNexusHealth(-25.0D, 1500.0D);
        assertEquals(0.0D, town.getCurrentNexusHealth(1500.0D), 0.000_1D);

        town.setCurrentNexusHealth(900.0D, 1500.0D);
        assertTrue(town.clampCurrentNexusHealth(750.0D));
        assertEquals(750.0D, town.getCurrentNexusHealth(750.0D), 0.000_1D);
    }
}
