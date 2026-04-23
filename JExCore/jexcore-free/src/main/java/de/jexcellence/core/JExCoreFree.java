package de.jexcellence.core;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Bukkit entrypoint for the Free edition of JExCore. Delegates lifecycle to
 * {@link JExCoreFreeImpl}.
 */
public final class JExCoreFree extends JavaPlugin {

    private JExCoreFreeImpl implementation;

    @Override
    public void onLoad() {
        try {
            JEDependency.initializeWithRemapping(this, JExCoreFree.class);
            this.implementation = new JExCoreFreeImpl(this);
            this.implementation.onLoad();
        } catch (final Exception exception) {
            this.getLogger().log(Level.SEVERE, "[JExCore-Free] Failed to load", exception);
            this.implementation = null;
        }
    }

    @Override
    public void onEnable() {
        if (this.implementation != null) {
            this.implementation.onEnable();
        } else {
            this.getLogger().severe("Cannot enable - JExCore Free failed to load");
            this.getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (this.implementation != null) {
            this.implementation.onDisable();
        }
    }

    public JExCoreFreeImpl getImpl() {
        return this.implementation;
    }
}
