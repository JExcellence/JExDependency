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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link RStoreRequirementProgress} progress normalization and defensive-copy behavior.
 */
class RStoreRequirementProgressTest {

    @Test
    void linksToPlayerAndNormalizesProgressKeys() {
        final RDRPlayer player = new RDRPlayer(UUID.randomUUID());

        final RStoreRequirementProgress progress = new RStoreRequirementProgress(player, "  storage-3:coins  ");

        assertEquals("storage-3:coins", progress.getProgressKey());
        assertEquals(player, progress.getPlayer());
    }

    @Test
    void rejectsBlankProgressKeys() {
        final RDRPlayer player = new RDRPlayer(UUID.randomUUID());

        assertThrows(IllegalArgumentException.class, () -> new RStoreRequirementProgress(player, "   "));
    }

    @Test
    void clampsCurrencyProgressToNonNegativeAmounts() {
        final RDRPlayer player = new RDRPlayer(UUID.randomUUID());
        final RStoreRequirementProgress progress = new RStoreRequirementProgress(player, "storage-1:vault");

        progress.setCurrencyAmount(-30.0D);

        assertEquals(0.0D, progress.getCurrencyAmount());
        assertFalse(progress.hasCurrencyProgress());

        progress.setCurrencyAmount(12.5D);

        assertEquals(12.5D, progress.getCurrencyAmount());
        assertTrue(progress.hasCurrencyProgress());
    }

    @Test
    void keepsItemProgressEmptyWhenNullItemStackIsProvided() {
        final RDRPlayer player = new RDRPlayer(UUID.randomUUID());
        final RStoreRequirementProgress progress = new RStoreRequirementProgress(player, "storage-1:item");

        progress.setItemStack(null);

        assertNull(progress.getItemStack());
        assertFalse(progress.hasItemProgress());
        assertTrue(progress.isEmpty());
    }

    @Test
    void becomesEmptyAfterCurrencyProgressIsCleared() {
        final RDRPlayer player = new RDRPlayer(UUID.randomUUID());
        final RStoreRequirementProgress progress = new RStoreRequirementProgress(player, "storage-2:item");

        progress.setCurrencyAmount(15.0D);
        assertFalse(progress.isEmpty());

        progress.setCurrencyAmount(0.0D);

        assertTrue(progress.isEmpty());
    }
}
