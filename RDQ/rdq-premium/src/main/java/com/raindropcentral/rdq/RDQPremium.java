package com.raindropcentral.rdq;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Main plugin class for RDQ Premium Edition.
 * <p>
 * This class serves as the entry point for the Bukkit plugin system and delegates all
 * functionality to {@link RDQPremiumImpl}. The delegate handles the staged enable pipeline:
 * asynchronous platform and executor preparation (stage 1), component and view wiring (stage 2),
 * and repository hydration (stage 3) that provides database-backed services for commands,
 * views, and cross-plugin integrations.
 * </p>
 *
 * @author JExcellence
 * @version 6.0.0
 * @since 6.0.0
 */
public final class RDQPremium extends JavaPlugin {

    private RDQPremiumImpl rdqImpl;

    /**
     * Retrieves the active plugin instance from the Bukkit plugin registry.
     *
     * @return the loaded {@link RDQPremium} instance
     */
    public static @NotNull RDQPremium get() {
        return JavaPlugin.getPlugin(RDQPremium.class);
    }

    /**
     * Boots the premium edition by initializing dependency remapping and creating
     * the delegate implementation.
     * <p>
     * Failures are logged and captured so that the plugin does not enter an
     * inconsistent enabled state.
     * </p>
     */
    @Override
    public void onLoad() {
        try {
            JEDependency.initializeWithRemapping(this, RDQPremium.class);
            this.rdqImpl = new RDQPremiumImpl(this);
            this.rdqImpl.onLoad();
        } catch (final Exception exception) {
            this.getLogger().log(Level.SEVERE, "[RDQ] Failed to load RDQ Premium", exception);
            this.rdqImpl = null;
        }
    }

    /**
     * Enables the plugin by delegating to the loaded implementation or disables
     * itself if initialization previously failed.
     */
    @Override
    public void onEnable() {
        if (this.rdqImpl != null) {
            this.rdqImpl.onEnable();
        } else {
            this.getLogger().severe("[RDQ] Cannot enable - RDQ Premium failed to load");
            this.getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Shuts down the premium delegate when the plugin is disabled.
     */
    @Override
    public void onDisable() {
        if (this.rdqImpl != null) {
            this.rdqImpl.onDisable();
        }
    }

    /**
     * Provides access to the internal premium implementation delegate.
     *
     * @return the delegate responsible for premium-specific behaviour
     */
    public @NotNull RDQPremiumImpl getImpl() {
        if (this.rdqImpl == null) {
            throw new IllegalStateException("RDQ Premium implementation not initialized");
        }
        return this.rdqImpl;
    }

    /**
     * Returns the RDQCore instance from the delegate.
     *
     * @return the core instance
     */
    public @NotNull RDQCore getCore() {
        return getImpl().getCore();
    }

    /**
     * Indicates this is the premium edition.
     *
     * @return always true
     */
    public boolean isPremium() {
        return true;
    }
}
