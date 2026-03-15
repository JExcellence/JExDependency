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
