package com.raindropcentral.rplatform.reward.lifecycle;

import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface RewardLifecycleHook {

    default boolean beforeGrant(@NotNull Player player, @NotNull AbstractReward reward) {
        return true;
    }

    default void afterGrant(@NotNull Player player, @NotNull AbstractReward reward, boolean success) {
    }

    default void onError(@NotNull Player player, @NotNull AbstractReward reward, @NotNull Throwable error) {
    }
}
