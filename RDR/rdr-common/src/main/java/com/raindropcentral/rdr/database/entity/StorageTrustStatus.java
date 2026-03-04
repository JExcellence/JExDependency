/*
 * StorageTrustStatus.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
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