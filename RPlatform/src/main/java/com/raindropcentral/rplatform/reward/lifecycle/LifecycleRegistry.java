package com.raindropcentral.rplatform.reward.lifecycle;

import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LifecycleRegistry {

    private static final Logger LOGGER = Logger.getLogger(LifecycleRegistry.class.getName());
    private static final LifecycleRegistry INSTANCE = new LifecycleRegistry();

    private final List<RewardLifecycleHook> hooks = new CopyOnWriteArrayList<>();

    private LifecycleRegistry() {}

    public static LifecycleRegistry getInstance() {
        return INSTANCE;
    }

    public void registerHook(@NotNull RewardLifecycleHook hook) {
        hooks.add(hook);
    }

    public void unregisterHook(@NotNull RewardLifecycleHook hook) {
        hooks.remove(hook);
    }

    public boolean executeBeforeGrant(@NotNull Player player, @NotNull AbstractReward reward) {
        for (RewardLifecycleHook hook : hooks) {
            try {
                if (!hook.beforeGrant(player, reward)) {
                    return false;
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in beforeGrant hook", e);
            }
        }
        return true;
    }

    public void executeAfterGrant(@NotNull Player player, @NotNull AbstractReward reward, boolean success) {
        for (RewardLifecycleHook hook : hooks) {
            try {
                hook.afterGrant(player, reward, success);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in afterGrant hook", e);
            }
        }
    }

    public void executeOnError(@NotNull Player player, @NotNull AbstractReward reward, @NotNull Throwable error) {
        for (RewardLifecycleHook hook : hooks) {
            try {
                hook.onError(player, reward, error);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in onError hook", e);
            }
        }
    }
}
