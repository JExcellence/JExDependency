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

package com.raindropcentral.rplatform.reward.lifecycle;

import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the RewardLifecycleHook API type.
 */
public interface RewardLifecycleHook {

    /**
     * Executes beforeGrant.
     */
    default boolean beforeGrant(@NotNull Player player, @NotNull AbstractReward reward) {
        return true;
    }

    /**
     * Executes afterGrant.
     */
    default void afterGrant(@NotNull Player player, @NotNull AbstractReward reward, boolean success) {
    }

    /**
     * Executes onError.
     */
    default void onError(@NotNull Player player, @NotNull AbstractReward reward, @NotNull Throwable error) {
    }
}
