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

package com.raindropcentral.core.service.statistics.queue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests helper behavior on {@link BackpressureLevel}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class BackpressureLevelTest {

    @Test
    void exposesCollectionMultipliers() {
        assertEquals(1.0, BackpressureLevel.NONE.getCollectionMultiplier(), 0.000_1D);
        assertEquals(0.5, BackpressureLevel.WARNING.getCollectionMultiplier(), 0.000_1D);
        assertEquals(0.25, BackpressureLevel.CRITICAL.getCollectionMultiplier(), 0.000_1D);
        assertEquals(0.0, BackpressureLevel.OVERFLOW.getCollectionMultiplier(), 0.000_1D);
    }

    @Test
    void reportsActiveAndPauseStates() {
        assertFalse(BackpressureLevel.NONE.isActive());
        assertTrue(BackpressureLevel.WARNING.isActive());
        assertTrue(BackpressureLevel.CRITICAL.isActive());
        assertTrue(BackpressureLevel.OVERFLOW.isActive());

        assertFalse(BackpressureLevel.NONE.shouldPauseThrottleable());
        assertFalse(BackpressureLevel.WARNING.shouldPauseThrottleable());
        assertFalse(BackpressureLevel.CRITICAL.shouldPauseThrottleable());
        assertTrue(BackpressureLevel.OVERFLOW.shouldPauseThrottleable());
    }
}
