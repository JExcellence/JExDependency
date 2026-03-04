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
