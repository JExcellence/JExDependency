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

package com.raindropcentral.rplatform.requirement.lifecycle;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Lifecycle hooks for requirement execution.
 *
 * <p>Allows plugins to inject custom logic before/after requirement operations.
 */
public interface RequirementLifecycleHook {

    /**
     * Called before a requirement is checked.
     *
     * @param player the player
     * @param requirement the requirement
     * @return true to continue, false to cancel
     */
    default boolean beforeCheck(@NotNull Player player, @NotNull AbstractRequirement requirement) {
        return true;
    }

    /**
     * Called after a requirement is checked.
     *
     * @param player the player
     * @param requirement the requirement
     * @param met whether the requirement was met
     * @param progress the progress value
     */
    default void afterCheck(@NotNull Player player, @NotNull AbstractRequirement requirement, boolean met, double progress) {
    }

    /**
     * Called before a requirement is consumed.
     *
     * @param player the player
     * @param requirement the requirement
     * @return true to continue, false to cancel
     */
    default boolean beforeConsume(@NotNull Player player, @NotNull AbstractRequirement requirement) {
        return true;
    }

    /**
     * Called after a requirement is consumed.
     *
     * @param player the player
     * @param requirement the requirement
     */
    default void afterConsume(@NotNull Player player, @NotNull AbstractRequirement requirement) {
    }

    /**
     * Called when an error occurs during requirement processing.
     *
     * @param player the player
     * @param requirement the requirement
     * @param error the error that occurred
     */
    default void onError(@NotNull Player player, @NotNull AbstractRequirement requirement, @NotNull Throwable error) {
    }
}
