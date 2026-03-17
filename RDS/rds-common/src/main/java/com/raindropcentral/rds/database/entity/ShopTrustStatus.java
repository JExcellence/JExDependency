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

package com.raindropcentral.rds.database.entity;

import org.jetbrains.annotations.NotNull;

/**
 * Trust levels for shared shop management.
 *
 * <p>{@link #PUBLIC} grants no delegated access, {@link #ASSOCIATE} allows stocking access, and
 * {@link #TRUSTED} allows both stocking and full-management actions.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
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
     * Returns whether this trust level allows stocking items into the shop.
     *
     * @return {@code true} when stocking access is granted
     */
    public boolean hasSupplyAccess() {
        return this.supplyAccess;
    }

    /**
     * Returns whether this trust level allows full shop-management access.
     *
     * @return {@code true} when full access is granted
     */
    public boolean hasFullAccess() {
        return this.fullAccess;
    }

    /**
     * Returns the next trust level in the owner management cycle.
     *
     * @return next trust level in PUBLIC -> ASSOCIATE -> TRUSTED -> PUBLIC order
     */
    public @NotNull ShopTrustStatus next() {
        return switch (this) {
            case PUBLIC -> ASSOCIATE;
            case ASSOCIATE -> TRUSTED;
            case TRUSTED -> PUBLIC;
        };
    }
}
