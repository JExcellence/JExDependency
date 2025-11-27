package com.raindropcentral.core;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Bukkit entrypoint for RCore.
 *
 * <p>This class wires the Bukkit lifecycle into the backend while defending
 * against partial bootstrap failures. During {@link #onLoad()} the plugin runs synchronously on
 * the server thread to initialize {@link JEDependency} with class remapping, reflectively
 * instantiate {@link RCoreImpl}, and register the delegate so that downstream services can be
 * created. {@link #onEnable()} and {@link #onDisable()} execute on the Bukkit primary thread and
 * forward to the delegate, ensuring asynchronous work is scheduled through the delegate while the
 * entrypoint remains thread-safe and disables itself when initialization fails.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 2.0.0
 */
public class RCore extends JavaPlugin {

    /**
     * Delegate that owns lifecycle orchestration, repository wiring, and service
     * publication. Populated during {@link #onLoad()} and cleared if bootstrapping fails so later
     * lifecycle phases can short-circuit safely.
     */
    private RCoreImpl rCoreImpl;

    /**
     * Boots the dependency remapper and instantiates the delegate.
     *
     * <p>Invoked synchronously by Bukkit before the plugin is enabled, this method loads shaded
     * dependencies via {@link JEDependency#initializeWithRemapping(JavaPlugin, Class)} and wires the
     * {@link RCoreImpl} delegate. Any exception encountered during dependency initialization or
     * reflective instantiation is logged and prevents later phases from running. The delegate is
     * left {@code null} so {@link #onEnable()} can disable the plugin gracefully instead of running
     * with partially configured services.</p>
     */
    @Override
    public void onLoad() {
        try {
            JEDependency.initializeWithRemapping(this, RCore.class);
            this.rCoreImpl = (RCoreImpl) Class.forName("com.raindropcentral.core.RCoreImpl").getDeclaredConstructor(RCore.class).newInstance(this);
            this.rCoreImpl.onLoad();
        } catch (final Exception exception) {
            this.getLogger().log(Level.SEVERE, "[RCore] Failed to load RCore", exception);
            this.rCoreImpl = null;
        }
    }

    /**
     * Delegates to the implementation to perform asynchronous startup.
     *
     * <p>Executed on the Bukkit primary thread, this method bridges the lifecycle into the delegate
     * so it can schedule asynchronous tasks, register listeners, and publish services. If the
     * delegate failed to initialize during {@link #onLoad()}, the plugin is disabled via Bukkit's
     * plugin manager to avoid running without registered services or repositories.</p>
     */
    @Override
    public void onEnable() {

        if (this.rCoreImpl != null) {
            this.rCoreImpl.onEnable();
        } else {
            this.getLogger().severe("[RCore] Cannot enable - RCore failed to load");
            this.getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Forwards shutdown to the delegate so it can unregister services and stop executors.
     *
     * <p>Bukkit invokes this on the main thread during server shutdown or plugin reload. The
     * delegate performs orderly cleanup of asynchronous executors and unregisters listeners. No
     * action is required if the delegate never initialized; the conditional protects against
     * {@code NullPointerException}s when the plugin aborted earlier in the lifecycle.</p>
     */
    @Override
    public void onDisable() {
        if (this.rCoreImpl != null) {
            this.rCoreImpl.onDisable();
        }
    }

    /**
     * Exposes the implementation delegate for tests or other internal components.
     *
     * @return the instantiated delegate or {@code null} when boot failed
     */
    public RCoreImpl getImpl() {
        return this.rCoreImpl;
    }
}
