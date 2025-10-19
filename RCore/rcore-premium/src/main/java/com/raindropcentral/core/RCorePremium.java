package com.raindropcentral.core;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Bukkit entrypoint for the premium RCore distribution.
 *
 * <p>The premium jar follows the same lifecycle as the free edition but delegates to
 * {@link RCorePremiumImpl} so enhanced integrations or overrides can be introduced without changing
 * the {@code JavaPlugin} subclass. {@link #onLoad()} is responsible for bootstrapping the runtime
 * dependency system, reflectively creating the delegate, and propagating any initialization errors
 * through Bukkit's logging facilities. Enable and disable phases forward control to the delegate
 * while ensuring the plugin shuts down safely if bootstrapping fails.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class RCorePremium extends JavaPlugin {

    /**
     * Edition-specific delegate that controls lifecycle orchestration, premium service registration,
     * and repository wiring. Kept {@code null} when dependency bootstrapping fails so subsequent
     * phases can short-circuit cleanly.
     */
    private RCorePremiumImpl rCoreImpl;

    /**
     * Initializes the runtime dependency remapper and creates the premium delegate.
     *
     * <p>Exceptions thrown during dependency setup or reflective construction are logged as severe
     * events and prevent the delegate from being assigned. Later phases rely on the {@code null}
     * check to avoid running with an incomplete backend.</p>
     */
    @Override
    public void onLoad() {
        try {
            JEDependency.initializeWithRemapping(this, RCorePremium.class);
            this.rCoreImpl = (RCorePremiumImpl) Class.forName("com.raindropcentral.core.RCorePremiumImpl")
                .getDeclaredConstructor(RCorePremium.class)
                .newInstance(this);
            this.rCoreImpl.onLoad();
        } catch (final Exception exception) {
            this.getLogger().log(Level.SEVERE, "[RCore] Failed to load RCore", exception);
            this.rCoreImpl = null;
        }
    }

    /**
     * Delegates enablement to the premium backend, disabling the plugin if boot failed earlier.
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
     * Forwards shutdown to the premium backend so it can unregister services and tear down
     * executors.
     */
    @Override
    public void onDisable() {
        if (this.rCoreImpl != null) {
            this.rCoreImpl.onDisable();
        }
    }

    /**
     * Exposes the premium implementation for diagnostics and internal tests.
     *
     * @return the premium delegate or {@code null} if {@link #onLoad()} failed
     */
    public RCorePremiumImpl getImpl() {
        return this.rCoreImpl;
    }
}
