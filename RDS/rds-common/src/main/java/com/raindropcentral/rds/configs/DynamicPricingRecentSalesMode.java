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

package com.raindropcentral.rds.configs;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * Represents how recent sales are counted for dynamic-pricing demand signals.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
public enum DynamicPricingRecentSalesMode {

    /**
     * Counts each completed purchase transaction as one sale.
     */
    TRANSACTIONS,

    /**
     * Counts the number of purchased item units across completed transactions.
     */
    ITEM_UNITS;

    /**
     * Parses a recent-sales mode value.
     *
     * @param rawValue raw configuration value
     * @return parsed mode, defaulting to {@link #ITEM_UNITS} when the value is blank or unknown
     */
    public static @NotNull DynamicPricingRecentSalesMode fromString(
            final @Nullable String rawValue
    ) {
        if (rawValue == null || rawValue.isBlank()) {
            return ITEM_UNITS;
        }

        final String normalized = rawValue.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "TRANSACTION", "TRANSACTIONS", "PURCHASES" -> TRANSACTIONS;
            default -> ITEM_UNITS;
        };
    }
}
