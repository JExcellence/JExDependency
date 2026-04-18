package de.jexcellence.jexplatform.reward.lifecycle;

import de.jexcellence.jexplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Hook interface for observing reward lifecycle events.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public interface RewardLifecycleHook {

    /** Called before a reward is granted. Return false to abort. */
    default boolean beforeGrant(@NotNull Player player, @NotNull AbstractReward reward) {
        return true;
    }

    /** Called after a reward is granted. */
    default void afterGrant(@NotNull Player player, @NotNull AbstractReward reward) {
    }

    /** Called when granting fails. */
    default void onError(@NotNull Player player, @NotNull AbstractReward reward,
                         @NotNull Throwable error) {
    }
}
