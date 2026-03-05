package com.raindropcentral.rds.database.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests shop trust status behaviour.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
class ShopTrustStatusTest {

    @Test
    void accessFlagsMatchTrustLevelCapabilities() {
        assertFalse(ShopTrustStatus.PUBLIC.hasSupplyAccess());
        assertFalse(ShopTrustStatus.PUBLIC.hasFullAccess());

        assertTrue(ShopTrustStatus.ASSOCIATE.hasSupplyAccess());
        assertFalse(ShopTrustStatus.ASSOCIATE.hasFullAccess());

        assertTrue(ShopTrustStatus.TRUSTED.hasSupplyAccess());
        assertTrue(ShopTrustStatus.TRUSTED.hasFullAccess());
    }

    @Test
    void trustStatusCycleLoopsBackToPublic() {
        assertEquals(ShopTrustStatus.ASSOCIATE, ShopTrustStatus.PUBLIC.next());
        assertEquals(ShopTrustStatus.TRUSTED, ShopTrustStatus.ASSOCIATE.next());
        assertEquals(ShopTrustStatus.PUBLIC, ShopTrustStatus.TRUSTED.next());
    }
}
