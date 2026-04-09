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

package com.raindropcentral.rdt.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownBankSessionRegistryTest {

    @AfterEach
    void tearDown() {
        TownBankSessionRegistry.clear();
    }

    @Test
    void sameOwnerMayReenterUntilAllHoldsAreReleased() {
        final UUID townUuid = UUID.randomUUID();
        final UUID ownerUuid = UUID.randomUUID();

        assertTrue(TownBankSessionRegistry.acquire(townUuid, ownerUuid));
        assertTrue(TownBankSessionRegistry.acquire(townUuid, ownerUuid));
        assertTrue(TownBankSessionRegistry.isLocked(townUuid));
        assertTrue(TownBankSessionRegistry.canOpen(townUuid, ownerUuid));

        TownBankSessionRegistry.release(townUuid, ownerUuid);
        assertTrue(TownBankSessionRegistry.isLocked(townUuid));

        TownBankSessionRegistry.release(townUuid, ownerUuid);
        assertFalse(TownBankSessionRegistry.isLocked(townUuid));
        assertTrue(TownBankSessionRegistry.canOpen(townUuid, ownerUuid));
    }

    @Test
    void differentOwnerCannotAcquireWhileTownBankIsHeld() {
        final UUID townUuid = UUID.randomUUID();
        final UUID firstOwnerUuid = UUID.randomUUID();
        final UUID secondOwnerUuid = UUID.randomUUID();

        assertTrue(TownBankSessionRegistry.acquire(townUuid, firstOwnerUuid));
        assertFalse(TownBankSessionRegistry.acquire(townUuid, secondOwnerUuid));
        assertFalse(TownBankSessionRegistry.canOpen(townUuid, secondOwnerUuid));

        TownBankSessionRegistry.release(townUuid, firstOwnerUuid);
        assertFalse(TownBankSessionRegistry.isLocked(townUuid));
        assertTrue(TownBankSessionRegistry.acquire(townUuid, secondOwnerUuid));
    }
}
