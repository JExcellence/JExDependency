package de.jexcellence.glow;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Bukkit entrypoint for JExGlow.
 * <p>
 * This class wires the Bukkit lifecycle into the backend while defending
 * against partial bootstrap failures. During {@link #onLoad()} the plugin runs synchronously on
 * the server thread to initialize {@link JEDependency} with class remapping, reflectively
 * instantiate {@link JExGlowImpl}, and register the delegate so that downstream services can be
 * created. {@link #onEnable()} and {@link #onDisable()} execute on the Bukkit primary thread and
 * forward to the delegate, ensuring asynchronous work is scheduled through the delegate while the
 * entrypoint remains thread-safe and disables itself when initialization fails.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class JExGlow extends JavaPlugin {

    /**
     * Delegate that owns lifecycle orchestration, repository wiring, and service
     * publication. Populated during {@link #onLoad()} and cleared if bootstrapping fails so later
     * lifecycle phases can short-circuit safely.
     */
    private JExGlowImpl jexGlowImpl;

    /**
     * Boots the dependency remapper and instantiates the delegate.
     * <p>
     * Invoked synchronously by Bukkit before the plugin is enabled, this method loads shaded
     * dependencies via {@link JEDependency#initializeWithRemapping(JavaPlugin, Class)} and wires the
     * {@link JExGlowImpl} delegate. Any exception encountered during dependency initialization or
     * reflective instantiation is logged and prevents later phases from running. The delegate is
     * left {@code null} so {@link #onEnable()} can disable the plugin gracefully instead of running
     * with partially configured services.
     * </p>
     */
    @Override
    public void onLoad() {
        try {
            JEDependency.initializeWithRemapping(this, JExGlow.class);
            this.jexGlowImpl = (JExGlowImpl) Class.forName("de.jexcellence.glow.JExGlowImpl")
                .getDeclaredConstructor(JExGlow.class)
                .newInstance(this);
            this.jexGlowImpl.onLoad();
        } catch (final Exception exception) {
            this.getLogger().log(Level.SEVERE, "[JExGlow] Failed to load JExGlow", exception);
            this.jexGlowImpl = null;
        }
    }

    /**
     * Delegates to the implementation to perform asynchronous startup.
     * <p>
     * Executed on the Bukkit primary thread, this method bridges the lifecycle into the delegate
     * so it can schedule asynchronous tasks, register listeners, and publish services. If the
     * delegate failed to initialize during {@link #onLoad()}, the plugin is disabled via Bukkit's
     * plugin manager to avoid running without registered services or repositories.
     * </p>
     */
    @Override
    public void onEnable() {
        if (this.jexGlowImpl != null) {
            this.jexGlowImpl.onEnable();
        } else {
            this.getLogger().severe("[JExGlow] Cannot enable - JExGlow failed to load");
            this.getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Forwards shutdown to the delegate so it can unregister services and stop executors.
     * <p>
     * Bukkit invokes this on the main thread during server shutdown or plugin reload. The
     * delegate performs orderly cleanup of asynchronous executors and unregisters listeners. No
     * action is required if the delegate never initialized; the conditional protects against
     * {@code NullPointerException}s when the plugin aborted earlier in the lifecycle.
     * </p>
     */
    @Override
    public void onDisable() {
        if (this.jexGlowImpl != null) {
            this.jexGlowImpl.onDisable();
        }
    }

    /**
     * Exposes the implementation delegate for tests or other internal components.
     *
     * @return the instantiated delegate or {@code null} when boot failed
     */
    public JExGlowImpl getImpl() {
        return this.jexGlowImpl;
    }
}
