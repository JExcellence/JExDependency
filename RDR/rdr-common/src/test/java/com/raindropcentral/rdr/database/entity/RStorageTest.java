/*
 * RStorageTest.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
 */

package com.raindropcentral.rdr.database.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests r storage behavior.
 */
class RStorageTest {

    @Test
    void playerCanOwnMultipleTransientStorages() {
        final RDRPlayer player = new RDRPlayer(UUID.randomUUID());

        new RStorage(player, "storage-1", 54);
        new RStorage(player, "storage-2", 54);

        assertEquals(2, player.getStorages().size());
    }

    @Test
    void rejectsInventorySizesThatCannotBeRenderedAsChestViews() {
        final RDRPlayer player = new RDRPlayer(UUID.randomUUID());

        assertThrows(IllegalArgumentException.class, () -> new RStorage(player, "invalid", 10));
    }

    @Test
    void leaseLifecycleTracksOwnershipAndExpiry() {
        final RDRPlayer player = new RDRPlayer(UUID.randomUUID());
        final RStorage storage = new RStorage(player, "storage-1", 54);
        final UUID serverUuid = UUID.randomUUID();
        final UUID playerUuid = UUID.randomUUID();
        final UUID leaseToken = UUID.randomUUID();
        final LocalDateTime leaseExpiry = LocalDateTime.now().plusMinutes(2);

        storage.acquireLease(serverUuid, playerUuid, leaseToken, leaseExpiry);

        assertTrue(storage.hasActiveLease(LocalDateTime.now()));
        assertTrue(storage.isLeaseHeldBy(serverUuid, playerUuid, leaseToken));

        storage.clearLease();

        assertFalse(storage.hasActiveLease(LocalDateTime.now()));
    }

    @Test
    void hotkeyLifecycleAcceptsOnlyPositiveValues() {
        final RDRPlayer player = new RDRPlayer(UUID.randomUUID());
        final RStorage storage = new RStorage(player, "storage-1", 54);

        storage.setHotkey(3);

        assertEquals(3, storage.getHotkey());

        storage.clearHotkey();

        assertNull(storage.getHotkey());
        assertThrows(IllegalArgumentException.class, () -> storage.setHotkey(0));
    }

    @Test
    void trustStatusControlsSharedDepositAndWithdrawAccess() {
        final UUID ownerId = UUID.randomUUID();
        final UUID associateId = UUID.randomUUID();
        final UUID trustedId = UUID.randomUUID();
        final RDRPlayer player = new RDRPlayer(ownerId);
        final RStorage storage = new RStorage(player, "storage-1", 54);

        storage.setTrustStatus(associateId, StorageTrustStatus.ASSOCIATE);
        storage.setTrustStatus(trustedId, StorageTrustStatus.TRUSTED);

        assertEquals(StorageTrustStatus.TRUSTED, storage.getTrustStatus(ownerId));
        assertTrue(storage.canDeposit(ownerId));
        assertTrue(storage.canWithdraw(ownerId));

        assertEquals(StorageTrustStatus.ASSOCIATE, storage.getTrustStatus(associateId));
        assertTrue(storage.canDeposit(associateId));
        assertFalse(storage.canWithdraw(associateId));

        assertEquals(StorageTrustStatus.TRUSTED, storage.getTrustStatus(trustedId));
        assertTrue(storage.canDeposit(trustedId));
        assertTrue(storage.canWithdraw(trustedId));

        storage.setTrustStatus(associateId, StorageTrustStatus.PUBLIC);

        assertEquals(StorageTrustStatus.PUBLIC, storage.getTrustStatus(associateId));
        assertFalse(storage.canAccess(associateId));
    }

    @Test
    void trustStatusCycleLoopsBackToPublic() {
        assertEquals(StorageTrustStatus.ASSOCIATE, StorageTrustStatus.PUBLIC.next());
        assertEquals(StorageTrustStatus.TRUSTED, StorageTrustStatus.ASSOCIATE.next());
        assertEquals(StorageTrustStatus.PUBLIC, StorageTrustStatus.TRUSTED.next());
    }
}