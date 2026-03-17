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

package com.raindropcentral.core.service.statistics.delivery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests factory, metric, and builder behavior in {@link DeliveryResult}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class DeliveryResultTest {

    @Test
    void factoryMethodsProduceExpectedResultShapes() {
        final DeliveryReceipt receipt = DeliveryReceipt.success("batch-1", 10, 10);

        final DeliveryResult empty = DeliveryResult.empty();
        final DeliveryResult limited = DeliveryResult.rateLimited(6, 2500L);
        final DeliveryResult success = DeliveryResult.success(10, receipt);
        final DeliveryResult failure = DeliveryResult.failure(5, "backend_error");

        assertTrue(empty.success());
        assertEquals(0, empty.totalCount());

        assertFalse(limited.success());
        assertTrue(limited.rateLimited());
        assertEquals(6, limited.statisticsFailed());
        assertEquals(2500L, limited.retryAfterMs());

        assertTrue(success.success());
        assertEquals("batch-1", success.batchId());
        assertEquals(10, success.statisticsDelivered());
        assertEquals(0, success.statisticsFailed());

        assertFalse(failure.success());
        assertEquals("backend_error", failure.errorMessage());
        assertEquals(5, failure.statisticsFailed());
    }

    @Test
    void extendedFactoriesAndHelpersComputeRates() {
        final DeliveryReceipt receipt = DeliveryReceipt.success("batch-2", 8, 7);

        final DeliveryResult success = DeliveryResult.success("batch-2", 8, receipt, 120L);
        final DeliveryResult failure = DeliveryResult.failure("batch-3", 4, "timeout", 220L);
        final DeliveryResult partial = DeliveryResult.partial("batch-4", 6, 2, receipt, 80L);

        assertEquals(120L, success.latencyMs());
        assertTrue(success.success());

        assertFalse(failure.success());
        assertEquals("batch-3", failure.batchId());
        assertEquals(220L, failure.latencyMs());

        assertTrue(partial.success());
        assertEquals(8, partial.totalCount());
        assertEquals(75.0D, partial.successRate(), 0.000_1D);
        assertEquals(6, partial.deliveredCount());
        assertEquals(2, partial.failedCount());
        assertTrue(partial.errorMessage().contains("Partial delivery"));
    }

    @Test
    void withLatencyReturnsCopyWithUpdatedLatency() {
        final DeliveryResult base = DeliveryResult.failure(3, "failed");

        final DeliveryResult updated = base.withLatency(300L);

        assertEquals(300L, updated.latencyMs());
        assertEquals(base.statisticsFailed(), updated.statisticsFailed());
        assertEquals(base.errorMessage(), updated.errorMessage());
    }

    @Test
    void builderMapsAllProvidedFields() {
        final DeliveryReceipt receipt = DeliveryReceipt.success("batch-5", 2, 2);
        final DeliveryResult result = DeliveryResult.builder()
            .success(false)
            .batchId("batch-5")
            .statisticsDelivered(1)
            .statisticsFailed(1)
            .errorMessage("partial")
            .receipt(receipt)
            .latencyMs(90L)
            .rateLimited(true)
            .retryAfterMs(500L)
            .build();

        assertFalse(result.success());
        assertEquals("batch-5", result.batchId());
        assertEquals(1, result.statisticsDelivered());
        assertEquals(1, result.statisticsFailed());
        assertEquals("partial", result.errorMessage());
        assertEquals(receipt, result.receipt());
        assertEquals(90L, result.latencyMs());
        assertTrue(result.rateLimited());
        assertEquals(500L, result.retryAfterMs());
        assertEquals(50.0D, result.successRate(), 0.000_1D);
    }

    @Test
    void successRateHandlesZeroTotals() {
        final DeliveryResult result = DeliveryResult.empty();

        assertEquals(0.0D, result.successRate(), 0.000_1D);
        assertNull(result.batchId());
    }
}
