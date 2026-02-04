package com.raindropcentral.rplatform.requirement.lifecycle;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Lifecycle hooks for requirement execution.
 * <p>
 * Allows plugins to inject custom logic before/after requirement operations.
 * </p>
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
