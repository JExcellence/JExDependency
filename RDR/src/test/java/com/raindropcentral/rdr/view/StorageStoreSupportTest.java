/*
 * StorageStoreSupportTest.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rdr.view;

import java.util.UUID;

import com.raindropcentral.rdr.database.entity.RDRPlayer;
import com.raindropcentral.rdr.database.entity.RStorage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageStoreSupportTest {

    @Test
    void buildsNextStorageKeyUsingFirstAvailableSequenceGap() {
        final RDRPlayer player = new RDRPlayer(UUID.randomUUID());
        new RStorage(player, "storage-1", 54);
        new RStorage(player, "storage-2", 54);
        new RStorage(player, "storage-4", 54);

        assertEquals("storage-3", StorageStoreSupport.buildNextStorageKey(player));
    }

    @Test
    void detectsWhenStorageLimitIsReached() {
        assertTrue(StorageStoreSupport.hasReachedStorageLimit(5, 5));
        assertFalse(StorageStoreSupport.hasReachedStorageLimit(4, 5));
    }
}
