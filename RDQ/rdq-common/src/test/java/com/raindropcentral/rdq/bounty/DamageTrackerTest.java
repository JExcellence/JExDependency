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

package com.raindropcentral.rdq.bounty;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies damage aggregation and tracking-window behavior for {@link DamageTracker}.
 *
 * @author ItsRainingHP
 * @since 6.0.0
 * @version 6.0.0
 */
class DamageTrackerTest {

    @Test
    void constructorRejectsNonPositiveTrackingWindow() {
        assertThrows(IllegalArgumentException.class, () -> new DamageTracker(0));
        assertThrows(IllegalArgumentException.class, () -> new DamageTracker(-1));
    }

    @Test
    void recordDamageAggregatesByAttackerWithinWindow() {
        final DamageTracker tracker = new DamageTracker(5_000);
        final UUID victimUniqueId = UUID.randomUUID();
        final UUID attackerOneUniqueId = UUID.randomUUID();
        final UUID attackerTwoUniqueId = UUID.randomUUID();

        tracker.recordDamage(victimUniqueId, attackerOneUniqueId, 3.5);
        tracker.recordDamage(victimUniqueId, attackerOneUniqueId, 1.5);
        tracker.recordDamage(victimUniqueId, attackerTwoUniqueId, 2.0);

        final Map<UUID, Double> damageMap = tracker.getDamageMap(victimUniqueId);

        assertEquals(2, damageMap.size());
        assertEquals(5.0D, damageMap.get(attackerOneUniqueId), 0.000_1D);
        assertEquals(2.0D, damageMap.get(attackerTwoUniqueId), 0.000_1D);
        assertEquals(7.0D, tracker.getTotalDamage(victimUniqueId), 0.000_1D);
        assertEquals(1, tracker.getTrackedVictimCount());
    }

    @Test
    void recordDamageIgnoresNonPositiveValues() {
        final DamageTracker tracker = new DamageTracker(5_000);
        final UUID victimUniqueId = UUID.randomUUID();
        final UUID attackerUniqueId = UUID.randomUUID();

        tracker.recordDamage(victimUniqueId, attackerUniqueId, 0.0D);
        tracker.recordDamage(victimUniqueId, attackerUniqueId, -1.0D);

        assertTrue(tracker.getDamageMap(victimUniqueId).isEmpty());
        assertEquals(0.0D, tracker.getTotalDamage(victimUniqueId), 0.000_1D);
        assertEquals(0, tracker.getTrackedVictimCount());
    }

    @Test
    void clearDamageRemovesTrackedVictim() {
        final DamageTracker tracker = new DamageTracker(5_000);
        final UUID victimUniqueId = UUID.randomUUID();
        final UUID attackerUniqueId = UUID.randomUUID();

        tracker.recordDamage(victimUniqueId, attackerUniqueId, 1.0D);
        assertEquals(1, tracker.getTrackedVictimCount());

        tracker.clearDamage(victimUniqueId);

        assertEquals(0, tracker.getTrackedVictimCount());
        assertTrue(tracker.getDamageMap(victimUniqueId).isEmpty());
    }

    @Test
    void cleanupExpiredRecordsRemovesOnlyExpiredEntries() throws InterruptedException {
        final DamageTracker tracker = new DamageTracker(120);
        final UUID firstVictimUniqueId = UUID.randomUUID();
        final UUID secondVictimUniqueId = UUID.randomUUID();
        final UUID attackerUniqueId = UUID.randomUUID();

        tracker.recordDamage(firstVictimUniqueId, attackerUniqueId, 2.0D);
        Thread.sleep(180);
        tracker.recordDamage(secondVictimUniqueId, attackerUniqueId, 1.0D);

        tracker.cleanupExpiredRecords();

        assertTrue(tracker.getDamageMap(firstVictimUniqueId).isEmpty());
        assertFalse(tracker.getDamageMap(secondVictimUniqueId).isEmpty());
        assertEquals(1, tracker.getTrackedVictimCount());
    }
}
