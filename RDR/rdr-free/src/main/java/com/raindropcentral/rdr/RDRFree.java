package com.raindropcentral.rdr;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Main plugin class for RDR Free Edition.
 *
 * <p>This Bukkit entry point performs dependency remapping and delegates all runtime bootstrap work to
 * {@link RDRFreeImpl} so the shared RDR core can stay free of direct plugin-loader concerns.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public final class RDRFree extends JavaPlugin {

    private RDRFreeImpl impl;

    /**
     * Loads the dependency remapper and initializes the free-edition delegate.
     */
    @Override
    public void onLoad() {
        try {
            JEDependency.initializeWithRemapping(this, RDRFree.class);
            this.impl = new RDRFreeImpl(this);
            this.impl.onLoad();
        } catch (final Exception exception) {
            this.getLogger().log(Level.SEVERE, "Failed to load RDR Free", exception);
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

        this.getLogger().severe("Cannot enable RDR Free because the delegate failed to load.");
        this.getServer().getPluginManager().disablePlugin(this);
    }

    /**
     * Shuts down the free delegate when the plugin is disabled.
     */
    @Override
    public void onDisable() {
        if (this.impl != null) {
            this.impl.onDisable();
        }
    }
}
