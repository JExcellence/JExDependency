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

package com.raindropcentral.rdr.view;

import com.raindropcentral.rdr.database.entity.RDRPlayer;
import org.jetbrains.annotations.NotNull;

/**
 * Provides support utilities for storage store.
 */
final class StorageStoreSupport {

    private StorageStoreSupport() {}

    static boolean hasReachedStorageLimit(
        final int ownedStorages,
        final int maxStorages
    ) {
        if (maxStorages <= 0) {
            return false;
        }
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
