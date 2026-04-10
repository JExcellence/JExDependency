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

package com.raindropcentral.rdr.database.entity;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Trigger types that can create one persisted rollback snapshot.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public enum RollbackTriggerType {
    /**
     * Snapshot captured when the player dies.
     */
    DEATH,
    /**
     * Snapshot captured shortly after the player joins the server.
     */
    JOIN,
    /**
     * Snapshot captured while the player leaves the server.
     */
    LEAVE,
    /**
     * Snapshot captured after the player changes worlds.
     */
    WORLD_CHANGE,
    /**
     * Snapshot captured by an admin-initiated all-online backup.
     */
    MANUAL_BACKUP,
    /**
     * Snapshot captured automatically before a rollback restore is applied.
     */
    PRE_RESTORE_SAFETY;

    /**
     * Returns the translation-key suffix used by rollback views.
     *
     * @return lowercase translation suffix for this trigger
     */
    public @NotNull String getTranslationKey() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
