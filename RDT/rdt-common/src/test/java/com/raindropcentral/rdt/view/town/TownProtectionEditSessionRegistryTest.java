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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TownProtectionEditSessionRegistryTest {

    @AfterEach
    void tearDown() {
        TownProtectionEditSessionRegistry.clear();
    }

    @Test
    void samePlayerMayReenterTheProtectionEditor() {
        final UUID townUuid = UUID.randomUUID();
        final UUID playerUuid = UUID.randomUUID();

        assertTrue(TownProtectionEditSessionRegistry.acquire(townUuid, playerUuid));
        assertTrue(TownProtectionEditSessionRegistry.acquire(townUuid, playerUuid));

        TownProtectionEditSessionRegistry.release(townUuid, playerUuid);
        assertTrue(TownProtectionEditSessionRegistry.canOpen(townUuid, playerUuid));
    }

    @Test
    void secondPlayerIsBlockedUntilTheOwnerReleasesTheSession() {
        final UUID townUuid = UUID.randomUUID();
        final UUID firstPlayerUuid = UUID.randomUUID();
        final UUID secondPlayerUuid = UUID.randomUUID();

        assertTrue(TownProtectionEditSessionRegistry.acquire(townUuid, firstPlayerUuid));
        assertFalse(TownProtectionEditSessionRegistry.canOpen(townUuid, secondPlayerUuid));
        assertFalse(TownProtectionEditSessionRegistry.acquire(townUuid, secondPlayerUuid));

        TownProtectionEditSessionRegistry.release(townUuid, firstPlayerUuid);
        assertTrue(TownProtectionEditSessionRegistry.canOpen(townUuid, secondPlayerUuid));
        assertTrue(TownProtectionEditSessionRegistry.acquire(townUuid, secondPlayerUuid));
    }
}
