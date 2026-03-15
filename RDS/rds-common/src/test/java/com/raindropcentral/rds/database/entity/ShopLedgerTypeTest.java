package com.raindropcentral.rds.database.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link ShopLedgerType} enum stability.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class ShopLedgerTypeTest {

    @Test
    void exposesExpectedLedgerTypesInStableOrder() {
        assertArrayEquals(
            new ShopLedgerType[]{
                ShopLedgerType.PURCHASE,
                ShopLedgerType.TAXATION
            },
            ShopLedgerType.values()
        );
    }

    @Test
    void resolvesEnumValueByCanonicalName() {
        assertEquals(ShopLedgerType.TAXATION, ShopLedgerType.valueOf("TAXATION"));
    }
}
