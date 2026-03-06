package com.raindropcentral.rdr.view;

import org.jetbrains.annotations.NotNull;

/**
 * Action modes used by the storage-admin control browser.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
enum StorageAdminStorageControlMode {
    FORCE_DRAIN_STORAGE,
    FORCE_RESET_STORAGE;

    static @NotNull StorageAdminStorageControlMode fromRaw(
        final @NotNull String rawValue
    ) {
        for (final StorageAdminStorageControlMode value : values()) {
            if (value.name().equalsIgnoreCase(rawValue)) {
                return value;
            }
        }
        return FORCE_DRAIN_STORAGE;
    }
}
