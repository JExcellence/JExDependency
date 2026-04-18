package de.jexcellence.jexplatform.requirement.lifecycle;

import de.jexcellence.jexplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry for requirement lifecycle hooks.
 *
 * <p>Not a singleton — create one per service instance and pass via
 * constructor injection.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class LifecycleRegistry {

    private final List<RequirementLifecycleHook> hooks = new CopyOnWriteArrayList<>();

    /**
     * Registers a lifecycle hook.
     *
     * @param hook the hook to register
     */
    public void register(@NotNull RequirementLifecycleHook hook) {
        hooks.add(hook);
    }

    /**
     * Unregisters a lifecycle hook.
     *
     * @param hook the hook to remove
     */
    public void unregister(@NotNull RequirementLifecycleHook hook) {
        hooks.remove(hook);
    }

    /**
     * Executes all {@code beforeCheck} hooks.
     *
     * @param player      the player
     * @param requirement the requirement
     * @return {@code true} if all hooks allow proceeding
     */
    public boolean executeBeforeCheck(@NotNull Player player,
                                      @NotNull AbstractRequirement requirement) {
        for (var hook : hooks) {
            if (!hook.beforeCheck(player, requirement)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Executes all {@code afterCheck} hooks.
     *
     * @param player      the player
     * @param requirement the requirement
     * @param met         whether the requirement was met
     * @param progress    the calculated progress
     */
    public void executeAfterCheck(@NotNull Player player,
                                  @NotNull AbstractRequirement requirement,
                                  boolean met, double progress) {
        for (var hook : hooks) {
            hook.afterCheck(player, requirement, met, progress);
        }
    }

    /**
     * Executes all {@code beforeConsume} hooks.
     *
     * @param player      the player
     * @param requirement the requirement
     * @return {@code true} if all hooks allow proceeding
     */
    public boolean executeBeforeConsume(@NotNull Player player,
                                        @NotNull AbstractRequirement requirement) {
        for (var hook : hooks) {
            if (!hook.beforeConsume(player, requirement)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Executes all {@code afterConsume} hooks.
     *
     * @param player      the player
     * @param requirement the requirement
     */
    public void executeAfterConsume(@NotNull Player player,
                                    @NotNull AbstractRequirement requirement) {
        for (var hook : hooks) {
            hook.afterConsume(player, requirement);
        }
    }

    /**
     * Executes all {@code onError} hooks.
     *
     * @param player      the player
     * @param requirement the requirement
     * @param error       the error
     */
    public void executeOnError(@NotNull Player player,
                               @NotNull AbstractRequirement requirement,
                               @NotNull Throwable error) {
        for (var hook : hooks) {
            hook.onError(player, requirement, error);
        }
    }

    /**
     * Removes all registered hooks.
     */
    public void clear() {
        hooks.clear();
    }
}
