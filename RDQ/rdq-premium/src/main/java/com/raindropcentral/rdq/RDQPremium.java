package com.raindropcentral.rdq;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Main plugin class for RaindropQuests Premium Edition.
 * <p>
 * This class serves as the entry point for the Bukkit plugin system and delegates all
 * functionality to {@link RDQPremiumImpl}. The delegate mirrors the staged enable pipeline defined
 * by {@link com.raindropcentral.rdq.RDQ}: asynchronous platform and executor preparation
 * (stage&nbsp;1), {@link com.raindropcentral.rdq.RDQ#runSync(Runnable) runSync}-scoped component and
 * view wiring (stage&nbsp;2), and repository hydration (stage&nbsp;3) that provides database-backed
 * services for commands, views, and cross-plugin integrations. Premium contributors should keep the
 * resource READMEs under {@code rdq-common/src/main/resources/} in sync with these lifecycle
 * expectations when evolving edition-specific features.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
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
        return this.rdqImpl;
    }
}