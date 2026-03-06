package com.raindropcentral.rdr.database.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests town storage-tax bank ledger entry behavior.
 */
class RTownStorageBankTest {

    @Test
    void normalizesScopeAndTracksAmounts() {
        final RTownStorageBank bankEntry = new RTownStorageBank(
            "  Towny  ",
            "  Town-UUID  ",
            "  My Town  ",
            "  Vault  ",
            10.0D
        );

        assertEquals("towny", bankEntry.getProtectionPlugin());
        assertEquals("town-uuid", bankEntry.getTownIdentifier());
        assertEquals("My Town", bankEntry.getTownDisplayName());
        assertEquals("vault", bankEntry.getCurrencyType());
        assertEquals(10.0D, bankEntry.getAmount());

        assertEquals(15.0D, bankEntry.deposit(5.0D));
        assertEquals(11.0D, bankEntry.withdraw(4.0D));
        assertEquals(0.0D, bankEntry.withdraw(100.0D));
    }

    @Test
    void matchesScopeIgnoresCaseAndWhitespace() {
        final RTownStorageBank bankEntry = new RTownStorageBank(
            "towny",
            "alpha-town",
            "Alpha Town",
            "vault",
            1.0D
        );

        assertTrue(bankEntry.matchesScope(" Towny ", " Alpha-Town ", " Vault "));
        assertFalse(bankEntry.matchesScope("RDT", "Alpha-Town", "vault"));
    }
}
