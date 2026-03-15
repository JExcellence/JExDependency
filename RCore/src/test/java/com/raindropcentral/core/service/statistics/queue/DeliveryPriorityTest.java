package com.raindropcentral.core.service.statistics.queue;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests ordering and throttle metadata on {@link DeliveryPriority}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class DeliveryPriorityTest {

    @Test
    void lowerOrderPrioritiesRankHigher() {
        assertTrue(DeliveryPriority.CRITICAL.isHigherThan(DeliveryPriority.HIGH));
        assertTrue(DeliveryPriority.HIGH.isHigherThan(DeliveryPriority.NORMAL));
        assertTrue(DeliveryPriority.NORMAL.isHigherThan(DeliveryPriority.LOW));
        assertTrue(DeliveryPriority.LOW.isHigherThan(DeliveryPriority.BULK));
        assertFalse(DeliveryPriority.BULK.isHigherThan(DeliveryPriority.CRITICAL));
    }

    @Test
    void identifiesThrottleablePriorities() {
        assertFalse(DeliveryPriority.CRITICAL.isThrottleable());
        assertFalse(DeliveryPriority.HIGH.isThrottleable());
        assertFalse(DeliveryPriority.NORMAL.isThrottleable());
        assertTrue(DeliveryPriority.LOW.isThrottleable());
        assertTrue(DeliveryPriority.BULK.isThrottleable());
    }
}
