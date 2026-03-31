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

package com.raindropcentral.rdq.bounty.type;

import org.jetbrains.annotations.NotNull;

/**
 * Enum representing different reward distribution modes for bounties.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public enum EDistributionMode {
    /**
     * Rewards are added directly to the hunter's inventory.
     * Excess items are dropped if inventory is full.
     */
    INSTANT,

    /**
     * Rewards are dropped at the death location.
     */
    DROP,

    /**
     * Rewards are placed in a chest at the death location.
     */
    CHEST,

    /**
     * Rewards are stored in a virtual storage system.
     */
    VIRTUAL;

    /**
     * Parses a string into an EDistributionMode.
     *
     * @param value the string value
     * @return the corresponding distribution mode, or INSTANT if invalid
     */
    @NotNull
    public static EDistributionMode of(@NotNull String value) {
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return INSTANT;
        }
    }
}
