package com.raindropcentral.core;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Bukkit entrypoint for the free RCore distribution.
 *
 * <p>This class is responsible for wiring Paper's lifecycle callbacks to the edition-specific
 * backend implementation. During {@link #onLoad()} the plugin bootstraps the shaded dependency
 * manager, reflectively creates {@link RCoreFreeImpl}, and delegates lifecycle hooks so the
 * implementation can orchestrate database and service initialization. {@link #onEnable()} and
 * {@link #onDisable()} simply forward control to the delegate while ensuring the plugin is safely
 * disabled if bootstrapping fails.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class RCoreFree extends JavaPlugin {

    /**
     * Edition-specific delegate that owns lifecycle orchestration, repository wiring, and service
     * publication. Populated during {@link #onLoad()} and cleared if bootstrapping fails so later
     * lifecycle phases can short-circuit safely.
     */
    private RCoreFreeImpl rCoreImpl;

    /**
     * Boots the dependency remapper and instantiates the edition delegate.
     *
     * <p>Any exception encountered during dependency initialization or reflective instantiation is
     * logged and prevents later phases from running. The delegate is left {@code null} so
     * {@link #onEnable()} can disable the plugin gracefully instead of running with partially
     * configured services.</p>
     */
    @Override
    public void onLoad() {
        try {
            JEDependency.initializeWithRemapping(this, RCoreFree.class);
            this.rCoreImpl = (RCoreFreeImpl) Class.forName("com.raindropcentral.core.RCoreFreeImpl").getDeclaredConstructor(RCoreFree.class).newInstance(this);
            this.rCoreImpl.onLoad();
        } catch (final Exception exception) {
            this.getLogger().log(Level.SEVERE, "[RCore] Failed to load RCore", exception);
            this.rCoreImpl = null;
        }
    }

    /**
     * Delegates to the edition implementation to perform asynchronous startup.
     *
     * <p>If the delegate failed to initialize during {@link #onLoad()}, the plugin is disabled via
     * Bukkit's plugin manager to avoid running without registered services or repositories.</p>
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
     * <p>No action is required if the delegate never initialized. The conditional protects against
     * {@code NullPointerException}s when the plugin aborted earlier in the lifecycle.</p>
     */
    @Override
    public void onDisable() {
        if (this.rCoreImpl != null) {
            this.rCoreImpl.onDisable();
        }
    }

    /**
     * Exposes the edition-specific delegate for tests or other internal components.
     *
     * @return the instantiated delegate or {@code null} when boot failed
     */
    public RCoreFreeImpl getImpl() {
        return this.rCoreImpl;
    }
}
