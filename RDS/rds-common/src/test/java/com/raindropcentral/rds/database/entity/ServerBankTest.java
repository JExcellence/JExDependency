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

/*
 * ServerBankTest.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rds.database.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests server bank entity behavior.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class ServerBankTest {

    @Test
    void normalizesConstructorValues() {
        final ServerBank bank = new ServerBank("  COINS  ", -100D);

        assertEquals("coins", bank.getCurrencyType());
        assertEquals(0D, bank.getAmount(), 1.0E-9D);
    }

    @Test
    void depositAndWithdrawClampAtZero() {
        final ServerBank bank = new ServerBank("vault", 10D);

        bank.deposit(5D);
        assertEquals(15D, bank.getAmount(), 1.0E-9D);

        bank.withdraw(100D);
        assertEquals(0D, bank.getAmount(), 1.0E-9D);
    }

    @Test
    void currencyComparisonIsCaseInsensitive() {
        final ServerBank bank = new ServerBank("vault", 1D);

        assertTrue(bank.matchesCurrencyType("VAULT"));
        assertFalse(bank.matchesCurrencyType("coins"));
    }
}
