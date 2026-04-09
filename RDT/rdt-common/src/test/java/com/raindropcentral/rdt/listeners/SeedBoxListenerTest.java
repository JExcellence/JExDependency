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

package com.raindropcentral.rdt.listeners;

import org.bukkit.event.inventory.InventoryAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedBoxListenerTest {

    @Test
    void pickupActionsDoNotCountAsSeedInsertion() {
        assertFalse(SeedBoxListener.insertsIntoSeedBox(true, InventoryAction.PICKUP_ALL));
        assertFalse(SeedBoxListener.insertsIntoSeedBox(true, InventoryAction.PICKUP_HALF));
        assertFalse(SeedBoxListener.insertsIntoSeedBox(true, InventoryAction.MOVE_TO_OTHER_INVENTORY));
    }

    @Test
    void placementActionsIntoTopInventoryAreValidated() {
        assertTrue(SeedBoxListener.insertsIntoSeedBox(true, InventoryAction.PLACE_ALL));
        assertTrue(SeedBoxListener.insertsIntoSeedBox(true, InventoryAction.SWAP_WITH_CURSOR));
        assertTrue(SeedBoxListener.insertsIntoSeedBox(true, InventoryAction.HOTBAR_SWAP));
    }

    @Test
    void shiftClicksOnlyValidateWhenMovingIntoSeedBox() {
        assertTrue(SeedBoxListener.insertsIntoSeedBox(false, InventoryAction.MOVE_TO_OTHER_INVENTORY));
        assertFalse(SeedBoxListener.insertsIntoSeedBox(true, InventoryAction.MOVE_TO_OTHER_INVENTORY));
    }
}
