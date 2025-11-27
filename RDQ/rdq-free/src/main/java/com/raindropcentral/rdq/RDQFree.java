package com.raindropcentral.rdq;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Main plugin class for RDQ Free Edition.
 * <p>
 * This class serves as the entry point for the Bukkit plugin system and delegates all
 * functionality to {@link RDQFreeImpl}. The delegate handles the staged enable pipeline:
 * asynchronous platform and executor preparation (stage 1), component and view wiring (stage 2),
 * and repository hydration (stage 3) that provides database-backed services for commands,
 * views, and cross-plugin integrations.
 * </p>
 *
 * @author JExcellence
 * @version 6.0.0
 * @since 6.0.0
 */
public final class RDQFree extends JavaPlugin {

    private RDQFreeImpl rdqImpl;

    /**
     * Retrieves the active plugin instance from the Bukkit plugin registry.
     *
     * @return the loaded {@link RDQFree} instance
     */
    public static @NotNull RDQFree get() {
        return JavaPlugin.getPlugin(RDQFree.class);
    }

    /**
     * Boots the free edition by initializing dependency remapping and creating
     * the delegate implementation.
     * <p>
     * Failures are logged and captured so that the plugin does not enter an
     * inconsistent enabled state.
     * </p>
     */
    @Override
    public void onLoad() {
        try {
            JEDependency.initializeWithRemapping(this, RDQFree.class);
            this.rdqImpl = new RDQFreeImpl(this);
            this.rdqImpl.onLoad();
        } catch (final Exception exception) {
            this.getLogger().log(Level.SEVERE, "[RDQ] Failed to load RDQ Free", exception);
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
            this.getLogger().severe("[RDQ] Cannot enable - RDQ Free failed to load");
            this.getServer().getPluginManager().disablePlugin(this);
        }
    }

    /**
     * Shuts down the free delegate when the plugin is disabled.
     */
    @Override
    public void onDisable() {
        if (this.rdqImpl != null) {
            this.rdqImpl.onDisable();
        }
    }

    /**
     * Provides access to the internal free implementation delegate.
     *
     * @return the delegate responsible for free-specific behaviour
     */
    public @NotNull RDQFreeImpl getImpl() {
        if (this.rdqImpl == null) {
            throw new IllegalStateException("RDQ Free implementation not initialized");
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
     * Indicates this is the free edition.
     *
     * @return always false
     */
    public boolean isPremium() {
        return false;
    }
}
