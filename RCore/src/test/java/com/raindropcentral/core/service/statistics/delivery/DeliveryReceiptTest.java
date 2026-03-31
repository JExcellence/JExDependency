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
 * Tests receipt factory helpers in {@link DeliveryReceipt}.
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
class DeliveryReceiptTest {

    @Test
    void successFactoryPopulatesSuccessFields() {
        final long before = System.currentTimeMillis();

        final DeliveryReceipt receipt = DeliveryReceipt.success("batch-1", 10, 8);

        final long after = System.currentTimeMillis();
        assertTrue(receipt.success());
        assertEquals("batch-1", receipt.batchId());
        assertEquals(10, receipt.receivedCount());
        assertEquals(8, receipt.processedCount());
        assertNull(receipt.errorMessage());
        assertNull(receipt.signature());
        assertTrue(receipt.timestamp() >= before);
        assertTrue(receipt.timestamp() <= after);
    }

    @Test
    void failureFactoryPopulatesErrorFields() {
        final DeliveryReceipt receipt = DeliveryReceipt.failure("batch-2", "timeout");

        assertFalse(receipt.success());
        assertEquals("batch-2", receipt.batchId());
        assertEquals(0, receipt.receivedCount());
        assertEquals(0, receipt.processedCount());
        assertEquals("timeout", receipt.errorMessage());
        assertNull(receipt.signature());
    }
}
