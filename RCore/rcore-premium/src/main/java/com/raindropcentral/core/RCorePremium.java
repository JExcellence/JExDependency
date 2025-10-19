package com.raindropcentral.core;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Bukkit entrypoint for the premium RCore distribution.
 *
 * <p>The premium jar follows the same lifecycle as the free edition but wraps it with
 * premium-specific boot hooks. {@link #onLoad()} wires JExDependency's remapping support so shaded
 * premium dependencies (for example database connectors or analytics bridges) are relocated before
 * Spigot loads any premium-only classes. It then reflectively instantiates {@link RCorePremiumImpl}
 * to allow the delegate to override service bindings, register premium listeners, and describe the
 * runtime capabilities exposed only to paying customers. Failure to initialize either the remapper
 * or the delegate is surfaced through Bukkit's logger and leaves the plugin in a safe disabled
 * state so enable and disable phases cannot progress partially.</p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class RCorePremium extends JavaPlugin {

    /**
     * Edition-specific delegate that controls lifecycle orchestration, premium service registration,
     * and repository wiring. The delegate augments the base implementation with premium feature
     * overrides such as cloud sync providers or additional analytics emitters. Kept {@code null}
     * when dependency bootstrapping fails so subsequent phases can short-circuit cleanly.
     */
    private RCorePremiumImpl rCoreImpl;

    /**
     * Initializes the runtime dependency remapper and creates the premium delegate.
     *
     * <p>JExDependency is invoked with {@link RCorePremium} as the remapping owner so premium-only
     * jars gain the same relocation rules as the shaded distribution. Exceptions thrown during
     * dependency setup or reflective construction are logged as severe events and prevent the
     * delegate from being assigned. Later phases rely on the {@code null} check to avoid running
     * with an incomplete backend.</p>
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
     * Delegates enablement to the premium backend, disabling the plugin if boot failed earlier so
     * premium listeners and data migrations are only scheduled when dependencies are ready.
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
     * executors tied to premium integrations.
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
     * @return the premium delegate or {@code null} if {@link #onLoad()} failed, allowing callers to
     * query which premium overrides were activated.
     */
    public RCorePremiumImpl getImpl() {
        return this.rCoreImpl;
    }
}
