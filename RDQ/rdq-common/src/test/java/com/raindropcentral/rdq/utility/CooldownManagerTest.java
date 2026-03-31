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

package com.raindropcentral.rdq.utility;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates cooldown lifecycle behavior in {@link CooldownManager}.
 *
 * @author ItsRainingHP
 * @since 6.0.0
 * @version 6.0.0
 */
class CooldownManagerTest {

    @Test
    void keyWithoutRecordedCooldownIsNotBlocked() {
        final CooldownManager manager = new CooldownManager();

        assertFalse(manager.isOnCooldown("sample", 1_000));
        assertEquals(0L, manager.getRemainingCooldown("sample", 1_000));
    }

    @Test
    void resetCooldownMarksKeyOnCooldown() {
        final CooldownManager manager = new CooldownManager();

        manager.resetCooldown("sample");

        assertTrue(manager.isOnCooldown("sample", 1_000));
        assertTrue(manager.getRemainingCooldown("sample", 1_000) > 0L);
    }

    @Test
    void clearCooldownRemovesEntryImmediately() {
        final CooldownManager manager = new CooldownManager();

        manager.resetCooldown("sample");
        manager.clearCooldown("sample");

        assertFalse(manager.isOnCooldown("sample", 1_000));
        assertEquals(0L, manager.getRemainingCooldown("sample", 1_000));
    }

    @Test
    void cooldownExpiresAfterConfiguredDuration() throws InterruptedException {
        final CooldownManager manager = new CooldownManager();
        final long cooldownMillis = 120L;

        manager.resetCooldown("sample");
        Thread.sleep(180L);

        assertFalse(manager.isOnCooldown("sample", cooldownMillis));
        assertEquals(0L, manager.getRemainingCooldown("sample", cooldownMillis));
    }
}
