package de.jexcellence.jexplatform.requirement.lifecycle;

import de.jexcellence.jexplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Hook interface for observing requirement lifecycle events.
 *
 * <p>All methods have default no-op implementations.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public interface RequirementLifecycleHook {

    /**
     * Called before a requirement check is performed.
     *
     * @param player      the player
     * @param requirement the requirement
     * @return {@code true} to proceed, {@code false} to abort
     */
    default boolean beforeCheck(@NotNull Player player,
                                @NotNull AbstractRequirement requirement) {
        return true;
    }

    /**
     * Called after a requirement check completes.
     *
     * @param player      the player
     * @param requirement the requirement
     * @param met         whether the requirement was met
     * @param progress    the calculated progress
     */
    default void afterCheck(@NotNull Player player,
                            @NotNull AbstractRequirement requirement,
                            boolean met, double progress) {
    }

    /**
     * Called before requirement resources are consumed.
     *
     * @param player      the player
     * @param requirement the requirement
     * @return {@code true} to proceed, {@code false} to abort
     */
    default boolean beforeConsume(@NotNull Player player,
                                  @NotNull AbstractRequirement requirement) {
        return true;
    }

    /**
     * Called after requirement resources are consumed.
     *
     * @param player      the player
     * @param requirement the requirement
     */
    default void afterConsume(@NotNull Player player,
                              @NotNull AbstractRequirement requirement) {
    }

    /**
     * Called when an error occurs during requirement processing.
     *
     * @param player      the player
     * @param requirement the requirement
     * @param error       the error that occurred
     */
    default void onError(@NotNull Player player,
                         @NotNull AbstractRequirement requirement,
                         @NotNull Throwable error) {
    }
}
