/*
 * StorageStoreSupportTest.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rdr.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StorageStoreSupportTest {

    @Test
    void calculatesPurchaseNumberRelativeToStartingStorages() {
        assertEquals(1, StorageStoreSupport.getNextPurchaseNumber(1, 1));
        assertEquals(2, StorageStoreSupport.getNextPurchaseNumber(2, 1));
        assertEquals(3, StorageStoreSupport.getNextPurchaseNumber(5, 3));
    }

    @Test
    void clampsPurchaseNumberToOneWhenValuesAreInvalid() {
        assertEquals(1, StorageStoreSupport.getNextPurchaseNumber(0, 1));
        assertEquals(1, StorageStoreSupport.getNextPurchaseNumber(-2, 1));
        assertEquals(1, StorageStoreSupport.getNextPurchaseNumber(2, 5));
    }
}
