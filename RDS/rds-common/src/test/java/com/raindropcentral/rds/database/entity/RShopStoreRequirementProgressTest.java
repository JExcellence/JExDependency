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

package com.raindropcentral.rds.database.entity;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link RShopStoreRequirementProgress} normalization and lifecycle behavior.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class RShopStoreRequirementProgressTest {

    @Test
    void linksToPlayerAndNormalizesProgressKey() {
        final RDSPlayer player = new RDSPlayer(UUID.randomUUID());

        final RShopStoreRequirementProgress progress = new RShopStoreRequirementProgress(player, "  shop-3:vault  ");

        assertEquals(player, progress.getPlayer());
        assertEquals("shop-3:vault", progress.getProgressKey());
    }

    @Test
    void rejectsBlankProgressKeys() {
        final RDSPlayer player = new RDSPlayer(UUID.randomUUID());

        assertThrows(IllegalArgumentException.class, () -> new RShopStoreRequirementProgress(player, "   "));
    }

    @Test
    void clampsCurrencyAmountsToNonNegativeValues() {
        final RDSPlayer player = new RDSPlayer(UUID.randomUUID());
        final RShopStoreRequirementProgress progress = new RShopStoreRequirementProgress(player, "shop-1:vault");

        progress.setCurrencyAmount(-25.0D);
        assertEquals(0.0D, progress.getCurrencyAmount());
        assertFalse(progress.hasCurrencyProgress());

        progress.setCurrencyAmount(42.5D);
        assertEquals(42.5D, progress.getCurrencyAmount());
        assertTrue(progress.hasCurrencyProgress());
    }

    @Test
    void remainsEmptyWhenItemProgressIsCleared() {
        final RDSPlayer player = new RDSPlayer(UUID.randomUUID());
        final RShopStoreRequirementProgress progress = new RShopStoreRequirementProgress(player, "shop-1:item");

        progress.setItemStack(null);

        assertNull(progress.getItemStack());
        assertFalse(progress.hasItemProgress());
        assertTrue(progress.isEmpty());
    }

    @Test
    void becomesEmptyAfterCurrencyProgressIsRemoved() {
        final RDSPlayer player = new RDSPlayer(UUID.randomUUID());
        final RShopStoreRequirementProgress progress = new RShopStoreRequirementProgress(player, "shop-2:item");

        progress.setCurrencyAmount(5.0D);
        assertFalse(progress.isEmpty());

        progress.setCurrencyAmount(0.0D);
        assertTrue(progress.isEmpty());
    }
}
