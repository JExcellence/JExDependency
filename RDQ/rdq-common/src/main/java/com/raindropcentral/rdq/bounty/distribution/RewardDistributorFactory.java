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

package com.raindropcentral.rdq.bounty.distribution;

import com.raindropcentral.rdq.bounty.type.EDistributionMode;
import org.jetbrains.annotations.NotNull;

/**
 * Factory for creating reward distributors based on distribution mode.
 *
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class RewardDistributorFactory {

    /**
     * Creates a reward distributor for the specified distribution mode.
     *
     * @param mode the distribution mode
     * @return the appropriate reward distributor
     */
    @NotNull
    public static RewardDistributor create(@NotNull EDistributionMode mode) {
        return switch (mode) {
            case INSTANT -> new InstantRewardDistributor();
            case DROP -> new DropRewardDistributor();
            case CHEST -> new ChestRewardDistributor();
            case VIRTUAL -> new VirtualRewardDistributor();
        };
    }
}
