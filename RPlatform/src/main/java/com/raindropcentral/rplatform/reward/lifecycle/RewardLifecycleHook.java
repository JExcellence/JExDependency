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
