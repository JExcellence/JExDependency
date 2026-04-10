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

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Shared helpers for rollback admin views.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
final class StorageAdminRollbackViewSupport {

    static final String ADMIN_PERMISSION = "raindroprdr.command.admin";
    static final String ROLLBACK_PERMISSION = "raindroprdr.command.admin.rollback";
    static final String BACKUP_PERMISSION = "raindroprdr.command.admin.backup";

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private StorageAdminRollbackViewSupport() {
        throw new UnsupportedOperationException("Utility class");
    }

    static boolean hasRollbackAccess(final @NotNull Player player) {
        return player.isOp()
            || player.hasPermission(ADMIN_PERMISSION)
            || player.hasPermission(ROLLBACK_PERMISSION);
    }

    static boolean hasBackupAccess(final @NotNull Player player) {
        return player.isOp()
            || player.hasPermission(ADMIN_PERMISSION)
            || player.hasPermission(BACKUP_PERMISSION);
    }

    static @NotNull String resolvePlayerDisplayName(
        final @NotNull UUID playerUuid,
        final @Nullable String lastKnownPlayerName
    ) {
        if (lastKnownPlayerName != null && !lastKnownPlayerName.isBlank()) {
            return lastKnownPlayerName;
        }

        final String offlinePlayerName = Bukkit.getOfflinePlayer(playerUuid).getName();
        return offlinePlayerName == null || offlinePlayerName.isBlank() ? playerUuid.toString() : offlinePlayerName;
    }

    static @NotNull String formatTimestamp(final @NotNull LocalDateTime timestamp) {
        return TIMESTAMP_FORMATTER.format(timestamp);
    }
}
