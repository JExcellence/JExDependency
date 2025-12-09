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
