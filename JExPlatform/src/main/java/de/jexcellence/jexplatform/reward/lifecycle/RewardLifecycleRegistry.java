package de.jexcellence.jexplatform.reward.lifecycle;

import de.jexcellence.jexplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry for reward lifecycle hooks.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class RewardLifecycleRegistry {

    private final List<RewardLifecycleHook> hooks = new CopyOnWriteArrayList<>();

    /** Registers a lifecycle hook. */
    public void register(@NotNull RewardLifecycleHook hook) { hooks.add(hook); }

    /** Unregisters a lifecycle hook. */
    public void unregister(@NotNull RewardLifecycleHook hook) { hooks.remove(hook); }

    /**
     * Executes all before-grant hooks.
     *
     * @param player the player
     * @param reward the reward
     * @return {@code false} if any hook vetoes the grant
     */
    public boolean executeBeforeGrant(@NotNull Player player, @NotNull AbstractReward reward) {
        for (var hook : hooks) {
            if (!hook.beforeGrant(player, reward)) return false;
        }
        return true;
    }

    /**
     * Executes all after-grant hooks.
     *
     * @param player the player
     * @param reward the reward
     */
    public void executeAfterGrant(@NotNull Player player, @NotNull AbstractReward reward) {
        hooks.forEach(h -> h.afterGrant(player, reward));
    }

    /**
     * Executes all error hooks.
     *
     * @param player the player
     * @param reward the reward
     * @param error  the exception
     */
    public void executeOnError(@NotNull Player player, @NotNull AbstractReward reward,
                               @NotNull Throwable error) {
        hooks.forEach(h -> h.onError(player, reward, error));
    }

    /** Removes all registered hooks. */
    public void clear() { hooks.clear(); }
}
