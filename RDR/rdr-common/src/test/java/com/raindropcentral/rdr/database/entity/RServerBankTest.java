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

package com.raindropcentral.rdr.database.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests server trade-tax bank multi-currency and ledger behavior.
 */
class RServerBankTest {

    @Test
    void normalizesScopeAndTracksMultiCurrencyBalances() {
        final RServerBank bank = new RServerBank("  GLOBAL  ");

        assertEquals("global", bank.getBankScope());
        assertEquals(10.0D, bank.deposit("  Vault  ", 10.0D));
        assertEquals(15.0D, bank.deposit("vault", 5.0D));
        assertEquals(25.0D, bank.deposit("raindrops", 25.0D));
        assertEquals(15.0D, bank.getBalance("VAULT"));
        assertEquals(25.0D, bank.getBalance("raindrops"));

        assertEquals(9.0D, bank.withdraw("vault", 9.0D));
        assertEquals(6.0D, bank.getBalance("vault"));
        assertEquals(6.0D, bank.withdraw("vault", 100.0D));
        assertEquals(0.0D, bank.getBalance("vault"));
        assertTrue(bank.getBalances().containsKey("raindrops"));
    }

    @Test
    void storesAndReadsCurrencyAwareLedgerEntries() {
        final RServerBank bank = new RServerBank("global");

        bank.appendLedgerEntry(new RServerBank.LedgerEntry(
            10L,
            "raindrops",
            RServerBank.TransactionType.DEPOSIT,
            25.0D,
            25.0D,
            "A1B2C3D4-E5F6-4789-ABCD-112233445566",
            "ABCDEFAB-CDEF-4ABC-8DEF-0123456789AB",
            "trade_tax_collection"
        ));
        bank.appendLedgerEntry(new RServerBank.LedgerEntry(
            11L,
            "vault",
            RServerBank.TransactionType.WITHDRAW,
            10.0D,
            15.0D,
            "A1B2C3D4-E5F6-4789-ABCD-112233445566",
            null,
            "admin_withdrawal"
        ));

        assertEquals(2, bank.getLedgerEntries().size());
        assertEquals(1, bank.getLedgerEntriesForCurrency("raindrops").size());
        assertEquals(1, bank.getLedgerEntriesForCurrency("vault").size());
        assertEquals(RServerBank.TransactionType.WITHDRAW, bank.getRecentLedgerEntries(5).getFirst().transactionType());
        assertEquals(RServerBank.TransactionType.DEPOSIT, bank.getRecentLedgerEntriesForCurrency("raindrops", 5).getFirst().transactionType());
        assertNull(bank.getLedgerEntriesForCurrency("vault").getFirst().tradeUuid());
    }
}
