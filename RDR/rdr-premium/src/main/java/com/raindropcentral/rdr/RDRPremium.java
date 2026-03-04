package com.raindropcentral.rdr;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Main plugin class for RDR Premium Edition.
 *
 * <p>This Bukkit entry point performs dependency remapping and delegates runtime bootstrap work to
 * {@link RDRPremiumImpl} so the shared RDR core can stay edition-agnostic.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public final class RDRPremium extends JavaPlugin {

    private RDRPremiumImpl impl;

    /**
     * Loads the dependency remapper and initializes the premium-edition delegate.
     */
    @Override
    public void onLoad() {
        try {
            JEDependency.initializeWithRemapping(this, RDRPremium.class);
            this.impl = new RDRPremiumImpl(this);
            this.impl.onLoad();
        } catch (final Exception exception) {
            this.getLogger().log(Level.SEVERE, "Failed to load RDR Premium", exception);
            this.impl = null;
        }
    }

    /**
     * Enables the plugin delegate or disables the plugin when bootstrap failed during load.
     */
    @Override
    public void onEnable() {
        if (this.impl != null) {
            this.impl.onEnable();
            return;
        }

        this.getLogger().severe("Cannot enable RDR Premium because the delegate failed to load.");
        this.getServer().getPluginManager().disablePlugin(this);
    }

    /**
     * Shuts down the premium delegate when the plugin is disabled.
     */
    @Override
    public void onDisable() {
        if (this.impl != null) {
            this.impl.onDisable();
        }
    }
}
