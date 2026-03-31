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

package com.raindropcentral.core.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests token-bucket behavior in {@link RateLimiter}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class RateLimiterTest {

    @Test
    void consumesUpToCapacityThenRateLimits() {
        final RateLimiter limiter = new RateLimiter(2, 10_000L);
        final UUID serverUniqueId = UUID.randomUUID();

        assertTrue(limiter.tryConsume(serverUniqueId));
        assertTrue(limiter.tryConsume(serverUniqueId));
        assertFalse(limiter.tryConsume(serverUniqueId));
    }

    @Test
    void resetClearsConsumedState() {
        final RateLimiter limiter = new RateLimiter(1, 10_000L);
        final UUID serverUniqueId = UUID.randomUUID();

        assertTrue(limiter.tryConsume(serverUniqueId));
        assertFalse(limiter.tryConsume(serverUniqueId));

        limiter.reset(serverUniqueId);

        assertTrue(limiter.tryConsume(serverUniqueId));
    }

    @Test
    void refillsAfterInterval() throws InterruptedException {
        final RateLimiter limiter = new RateLimiter(1, 25L);
        final UUID serverUniqueId = UUID.randomUUID();

        assertTrue(limiter.tryConsume(serverUniqueId));
        assertFalse(limiter.tryConsume(serverUniqueId));

        Thread.sleep(60L);

        assertTrue(limiter.tryConsume(serverUniqueId));
    }

    @Test
    void rejectsNullServerIdentifier() {
        final RateLimiter limiter = new RateLimiter(1, 1000L);

        assertThrows(NullPointerException.class, () -> limiter.tryConsume(null));
        assertThrows(NullPointerException.class, () -> limiter.reset(null));
    }
}
