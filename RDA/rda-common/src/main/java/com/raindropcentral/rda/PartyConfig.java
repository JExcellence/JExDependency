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

package com.raindropcentral.rda;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Immutable party configuration for RDA.
 *
 * <p>The configuration controls party size limits, invitation expiration, and the XP share model
 * used when nearby party members assist with skill progression.</p>
 *
 * @param maxMembers maximum party size including the leader, or {@code 0} / a negative value for unlimited size
 * @param inviteTimeoutSeconds lifetime of a pending invite in seconds
 * @param xpShareSettings XP share settings applied to party skill progression
 * @author Codex
 * @since 1.3.0
 * @version 1.3.0
 */
public record PartyConfig(
    int maxMembers,
    long inviteTimeoutSeconds,
    @NotNull XpShareSettings xpShareSettings
) {

    /**
     * Validates the immutable party configuration.
     */
    public PartyConfig {
        if (inviteTimeoutSeconds < 0L) {
            throw new IllegalArgumentException("inviteTimeoutSeconds must be non-negative");
        }
        Objects.requireNonNull(xpShareSettings, "xpShareSettings");
    }

    /**
     * Reports whether the party size cap is disabled.
     *
     * @return {@code true} when any number of members is allowed because the configured cap is {@code 0} or lower
     */
    public boolean hasUnlimitedMemberCap() {
        return this.maxMembers <= 0;
    }

    /**
     * Reports whether a party with the supplied member count can accept one more member.
     *
     * @param currentMembers current party size including the leader
     * @return {@code true} when another member can join
     */
    public boolean canAcceptAnotherMember(final int currentMembers) {
        return this.hasUnlimitedMemberCap() || currentMembers < this.maxMembers;
    }

    /**
     * Returns the invite timeout in milliseconds.
     *
     * @return invite timeout in milliseconds
     */
    public long inviteTimeoutMillis() {
        return this.inviteTimeoutSeconds * 1000L;
    }

    /**
     * Immutable XP share settings used by party progression.
     *
     * @param selfShare fraction of the pre-prestige XP pool granted to the earner
     * @param othersTotalShare fraction of the pre-prestige XP pool shared with nearby party members
     * @param rangeBlocks maximum distance in blocks between the earner and a sharing recipient
     */
    public record XpShareSettings(
        double selfShare,
        double othersTotalShare,
        double rangeBlocks
    ) {

        /**
         * Validates the immutable XP share settings.
         */
        public XpShareSettings {
            if (selfShare < 0.0D) {
                throw new IllegalArgumentException("selfShare must be non-negative");
            }
            if (othersTotalShare < 0.0D) {
                throw new IllegalArgumentException("othersTotalShare must be non-negative");
            }
            if (rangeBlocks < 0.0D) {
                throw new IllegalArgumentException("rangeBlocks must be non-negative");
            }
        }

        /**
         * Returns the squared share range used for distance comparisons.
         *
         * @return squared distance threshold
         */
        public double rangeSquared() {
            return this.rangeBlocks * this.rangeBlocks;
        }
    }
}
