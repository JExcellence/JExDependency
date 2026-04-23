package de.jexcellence.core;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Bukkit entrypoint for the Premium edition of JExCore. Delegates lifecycle to
 * {@link JExCorePremiumImpl}.
 */
public final class JExCorePremium extends JavaPlugin {

    private JExCorePremiumImpl implementation;

    @Override
    public void onLoad() {
        try {
            JEDependency.initializeWithRemapping(this, JExCorePremium.class);
            this.implementation = new JExCorePremiumImpl(this);
            this.implementation.onLoad();
        } catch (final Exception exception) {
            this.getLogger().log(Level.SEVERE, "[JExCore-Premium] Failed to load", exception);
            this.implementation = null;
        }
    }

    @Override
    public void onEnable() {
        if (this.implementation != null) {
            this.implementation.onEnable();
        } else {
            this.getLogger().severe("Cannot enable - JExCore Premium failed to load");
            this.getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (this.implementation != null) {
            this.implementation.onDisable();
        }
    }

    public JExCorePremiumImpl getImpl() {
        return this.implementation;
    }
}
