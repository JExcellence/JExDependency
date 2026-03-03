/*
 * StorageStoreSupport.java
 *
 * @author RaindropCentral
 * @version 5.0.0
 */

package com.raindropcentral.rdr.view;

import com.raindropcentral.rdr.database.entity.RDRPlayer;
import org.jetbrains.annotations.NotNull;

final class StorageStoreSupport {

    private StorageStoreSupport() {}

    static boolean hasReachedStorageLimit(
        final int ownedStorages,
        final int maxStorages
    ) {
        return ownedStorages >= maxStorages;
    }

    static int getNextPurchaseNumber(
        final int ownedStorages,
        final int startingStorages
    ) {
        final int normalizedOwnedStorages = Math.max(ownedStorages, 0);
        final int normalizedStartingStorages = Math.max(startingStorages, 0);
        return Math.max(normalizedOwnedStorages - normalizedStartingStorages + 1, 1);
    }

    static @NotNull String buildNextStorageKey(final @NotNull RDRPlayer player) {
        int index = 1;
        while (player.findStorage("storage-" + index).isPresent()) {
            index++;
        }
        return "storage-" + index;
    }
}
