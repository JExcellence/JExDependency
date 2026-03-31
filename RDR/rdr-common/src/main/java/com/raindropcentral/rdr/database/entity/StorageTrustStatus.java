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

/**
 * Trust levels for shared storage access.
 *
 * <p>{@link #PUBLIC} grants no access, {@link #ASSOCIATE} allows deposits only, and
 * {@link #TRUSTED} allows both deposits and withdrawals.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public enum StorageTrustStatus {

    PUBLIC(false, false),
    ASSOCIATE(true, false),
    TRUSTED(true, true);

    private final boolean depositAccess;
    private final boolean withdrawAccess;

    StorageTrustStatus(
        final boolean depositAccess,
        final boolean withdrawAccess
    ) {
        this.depositAccess = depositAccess;
        this.withdrawAccess = withdrawAccess;
    }

    /**
     * Returns whether this trust level allows storing items in the storage.
     *
     * @return {@code true} when deposit access is granted
     */
    public boolean hasDepositAccess() {
        return this.depositAccess;
    }

    /**
     * Returns whether this trust level allows withdrawing items from the storage.
     *
     * @return {@code true} when withdraw access is granted
     */
    public boolean hasWithdrawAccess() {
        return this.withdrawAccess;
    }

    /**
     * Returns the next trust level in the owner management cycle.
     *
     * @return next trust level in PUBLIC -> ASSOCIATE -> TRUSTED -> PUBLIC order
     */
    public @NotNull StorageTrustStatus next() {
        return switch (this) {
            case PUBLIC -> ASSOCIATE;
            case ASSOCIATE -> TRUSTED;
            case TRUSTED -> PUBLIC;
        };
    }
}