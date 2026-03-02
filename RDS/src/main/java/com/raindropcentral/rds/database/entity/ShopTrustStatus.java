package com.raindropcentral.rds.database.entity;

import org.jetbrains.annotations.NotNull;

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

    public boolean hasSupplyAccess() {
        return this.supplyAccess;
    }

    public boolean hasFullAccess() {
        return this.fullAccess;
    }

    public @NotNull ShopTrustStatus next() {
        return switch (this) {
            case PUBLIC -> ASSOCIATE;
            case ASSOCIATE -> TRUSTED;
            case TRUSTED -> PUBLIC;
        };
    }
}
