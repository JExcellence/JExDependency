package com.raindropcentral.rplatform.requirement.lifecycle;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registry for requirement lifecycle hooks.
 */
public final class LifecycleRegistry {

    private static final Logger LOGGER = Logger.getLogger(LifecycleRegistry.class.getName());
    private static final LifecycleRegistry INSTANCE = new LifecycleRegistry();

    private final List<RequirementLifecycleHook> hooks = new CopyOnWriteArrayList<>();

    private LifecycleRegistry() {}

    /**
     * Gets instance.
     */
    @NotNull
    public static LifecycleRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Executes registerHook.
     */
    public void registerHook(@NotNull RequirementLifecycleHook hook) {
        hooks.add(hook);
        LOGGER.info("Registered lifecycle hook: " + hook.getClass().getSimpleName());
    }

    /**
     * Executes unregisterHook.
     */
    public void unregisterHook(@NotNull RequirementLifecycleHook hook) {
        hooks.remove(hook);
        LOGGER.info("Unregistered lifecycle hook: " + hook.getClass().getSimpleName());
    }

    // ==================== Hook Execution ====================

    /**
     * Executes executeBeforeCheck.
     */
    public boolean executeBeforeCheck(@NotNull Player player, @NotNull AbstractRequirement requirement) {
        for (RequirementLifecycleHook hook : hooks) {
            try {
                if (!hook.beforeCheck(player, requirement)) {
                    return false;
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in beforeCheck hook", e);
            }
        }
        return true;
    }

    /**
     * Executes executeAfterCheck.
     */
    public void executeAfterCheck(@NotNull Player player, @NotNull AbstractRequirement requirement, boolean met, double progress) {
        for (RequirementLifecycleHook hook : hooks) {
            try {
                hook.afterCheck(player, requirement, met, progress);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in afterCheck hook", e);
            }
        }
    }

    /**
     * Executes executeBeforeConsume.
     */
    public boolean executeBeforeConsume(@NotNull Player player, @NotNull AbstractRequirement requirement) {
        for (RequirementLifecycleHook hook : hooks) {
            try {
                if (!hook.beforeConsume(player, requirement)) {
                    return false;
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in beforeConsume hook", e);
            }
        }
        return true;
    }

    /**
     * Executes executeAfterConsume.
     */
    public void executeAfterConsume(@NotNull Player player, @NotNull AbstractRequirement requirement) {
        for (RequirementLifecycleHook hook : hooks) {
            try {
                hook.afterConsume(player, requirement);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in afterConsume hook", e);
            }
        }
    }

    /**
     * Executes executeOnError.
     */
    public void executeOnError(@NotNull Player player, @NotNull AbstractRequirement requirement, @NotNull Throwable error) {
        for (RequirementLifecycleHook hook : hooks) {
            try {
                hook.onError(player, requirement, error);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error in onError hook", e);
            }
        }
    }
}
