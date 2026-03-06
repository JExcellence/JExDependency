package com.raindropcentral.rds.database.entity;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests tax-debt and bankrupt-state behavior on {@link Shop}.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class ShopTaxDebtStateTest {

    @Test
    void tracksDebtPerCurrencyUsingNormalizedCurrencyIds() {
        final Shop shop = new Shop(UUID.randomUUID(), null);

        shop.addTaxDebt("Vault", 100.0D);
        shop.addTaxDebt("vault", 20.0D);
        shop.addTaxDebt("Coins", 5.5D);

        assertTrue(shop.hasTaxDebt());
        assertTrue(shop.isBankrupt());
        assertEquals(2, shop.getTaxDebtEntries().size());
        assertEquals(120.0D, shop.getTaxDebtAmount("vault"), 1.0E-6D);
        assertEquals(5.5D, shop.getTaxDebtAmount("coins"), 1.0E-6D);
    }

    @Test
    void clearsBankruptStateWhenDebtIsFullyPaid() {
        final Shop shop = new Shop(UUID.randomUUID(), null);
        shop.addTaxDebt("vault", 50.0D);

        assertTrue(shop.hasTaxDebt());
        shop.reduceTaxDebt("vault", 15.0D);
        assertEquals(35.0D, shop.getTaxDebtAmount("vault"), 1.0E-6D);
        assertTrue(shop.hasTaxDebt());

        shop.reduceTaxDebt("vault", 35.0D);
        assertFalse(shop.hasTaxDebt());
        assertFalse(shop.isBankrupt());
    }

    @Test
    void capsDebtGrowthWhenMaximumAmountIsPositive() {
        final Shop shop = new Shop(UUID.randomUUID(), null);

        shop.addTaxDebt("vault", 50.0D, 75.0D);
        shop.addTaxDebt("vault", 200.0D, 75.0D);

        assertEquals(75.0D, shop.getTaxDebtAmount("vault"), 1.0E-6D);
        assertTrue(shop.hasTaxDebt());
    }

    @Test
    void treatsNonPositiveDebtCapAsUnlimited() {
        final Shop shop = new Shop(UUID.randomUUID(), null);

        shop.addTaxDebt("vault", 50.0D, 0.0D);
        shop.addTaxDebt("vault", 25.0D, -1.0D);

        assertEquals(75.0D, shop.getTaxDebtAmount("vault"), 1.0E-6D);
    }

}
