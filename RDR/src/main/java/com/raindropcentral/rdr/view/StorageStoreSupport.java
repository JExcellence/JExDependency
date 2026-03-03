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

    static @NotNull String buildNextStorageKey(final @NotNull RDRPlayer player) {
        int index = 1;
        while (player.findStorage("storage-" + index).isPresent()) {
            index++;
        }
        return "storage-" + index;
    }
}
