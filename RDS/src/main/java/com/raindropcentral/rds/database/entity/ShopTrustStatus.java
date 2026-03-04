/*
 * ShopTrustStatus.java
 *
 * @author ItsRainingHP
 * @version 5.0.0
 */

package com.raindropcentral.rds.database.entity;

import org.jetbrains.annotations.NotNull;

/**
 * Represents the available shop trust states.
 */
public enum ShopTrustStatus {

    PUBLIC(false, false),
    ASSOCIATE(true, false),
    TRUSTED(true, true);

    private final boolean supplyAccess;
    private final boolean fullAccess;

    ShopTrustStatus(
            final boolean supplyAccess,
            final boolean fullAccess
    ) {
        this.supplyAccess = supplyAccess;
        this.fullAccess = fullAccess;
    }

    /**
     * Indicates whether supply access is available.
     *
     * @return {@code true} if supply access; otherwise {@code false}
     */
    public boolean hasSupplyAccess() {
        return this.supplyAccess;
    }

    /**
     * Indicates whether full access is available.
     *
     * @return {@code true} if full access; otherwise {@code false}
     */
    public boolean hasFullAccess() {
        return this.fullAccess;
    }

    /**
     * Executes next.
     *
     * @return the next result
     */
    public @NotNull ShopTrustStatus next() {
        return switch (this) {
            case PUBLIC -> ASSOCIATE;
            case ASSOCIATE -> TRUSTED;
            case TRUSTED -> PUBLIC;
        };
    }
}