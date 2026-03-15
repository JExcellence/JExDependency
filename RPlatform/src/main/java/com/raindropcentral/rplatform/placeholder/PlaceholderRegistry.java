package com.raindropcentral.rplatform.placeholder;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/**
 * Coordinates PlaceholderAPI expansion registration for a plugin-owned.
 * expansion instance. At construction time the registry captures whether
 * PlaceholderAPI is currently enabled so subsequent {@link #register()} and {@link #unregister()}
 * calls can safely no-op when the dependency is unavailable. Successful operations log the
 * lifecycle transitions while the availability flag enables callers to short-circuit placeholder
 * access when PlaceholderAPI is missing.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class PlaceholderRegistry {

    private static final Logger LOGGER = Logger.getLogger(PlaceholderRegistry.class.getName());

    /**
     * Owning plugin reported in lifecycle log entries.
     */
    private final Plugin plugin;

    /**
     * Expansion instance used to register and unregister placeholder handlers.
     */
    private final Object expansion;

    /**
     * Cache of PlaceholderAPI availability captured during construction.
     */
    private final boolean papiAvailable;

    /**
     * Creates a registry bound to the supplied plugin and expansion.
     *
     * @param plugin    plugin that owns the placeholder expansion.
     * @param expansion placeholder expansion that should be registered with PlaceholderAPI.
     */
    public PlaceholderRegistry(
            final @NotNull Plugin plugin,
            final @NotNull Object expansion
    ) {
        this.plugin = plugin;
        this.expansion = expansion;
        this.papiAvailable = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    /**
     * Registers the expansion when PlaceholderAPI is available. Logs a warning if the dependency.
     * is missing and logs an informational message when registration succeeds.
     */
    public void register() {
        if (!papiAvailable) {
            LOGGER.warning("PlaceholderAPI not found - placeholders will not be available");
            return;
        }

        if (!this.invokeNoArgs("register")) {
            LOGGER.warning("Failed to register PlaceholderAPI expansion for " + plugin.getName());
            return;
        }

        LOGGER.info("Registered PlaceholderAPI expansion for " + plugin.getName());
    }

    /**
     * Unregisters the expansion only when PlaceholderAPI was available and registration previously.
     * occurred. Emits an informational log for successful unregister operations.
     */
    public void unregister() {
        if (!papiAvailable || !this.invokeBoolean("isRegistered")) {
            return;
        }

        if (this.invokeNoArgs("unregister")) {
            LOGGER.info("Unregistered PlaceholderAPI expansion for " + plugin.getName());
        } else {
            LOGGER.warning("Failed to unregister PlaceholderAPI expansion for " + plugin.getName());
        }
    }

    /**
     * Indicates whether PlaceholderAPI was available when the registry was created.
     *
     * @return {@code true} when PlaceholderAPI was enabled.
     */
    public boolean isAvailable() {
        return papiAvailable;
    }

    /**
     * Indicates whether the expansion is currently registered.
     *
     * @return {@code true} when PlaceholderAPI is available and the expansion reports registration.
     */
    public boolean isRegistered() {
        return papiAvailable && this.invokeBoolean("isRegistered");
    }

    private boolean invokeNoArgs(final @NotNull String methodName) {
        try {
            this.expansion.getClass().getMethod(methodName).invoke(this.expansion);
            return true;
        } catch (ReflectiveOperationException exception) {
            LOGGER.warning(
                "Failed to invoke '" + methodName + "' on expansion "
                    + this.expansion.getClass().getName() + ": " + exception.getMessage()
            );
            return false;
        }
    }

    private boolean invokeBoolean(final @NotNull String methodName) {
        try {
            final Object value = this.expansion.getClass().getMethod(methodName).invoke(this.expansion);
            return value instanceof Boolean && (Boolean) value;
        } catch (ReflectiveOperationException exception) {
            LOGGER.warning(
                "Failed to invoke '" + methodName + "' on expansion "
                    + this.expansion.getClass().getName() + ": " + exception.getMessage()
            );
            return false;
        }
    }
}
