package de.jexcellence.home;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Main plugin class for JExHome Free Edition.
 * <p>
 * This class serves as the entry point for the Bukkit plugin system and delegates all
 * functionality to {@link JExHomeFreeImpl}. The delegate handles the staged enable pipeline:
 * asynchronous platform and executor preparation, component and view wiring, and repository
 * hydration that provides database-backed services for commands, views, and cross-plugin integrations.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public final class JExHomeFree extends JavaPlugin {

    private JExHomeFreeImpl impl;

    @Override
    public void onLoad() {
        try {
            JEDependency.initializeWithRemapping(this, JExHomeFree.class);
            impl = new JExHomeFreeImpl(this);
            impl.onLoad();
        } catch (final Exception exception) {
            getLogger().log(Level.SEVERE, "Failed to load JExHome", exception);
            impl = null;
        }
    }

    @Override
    public void onEnable() {
        if (impl != null) {
            impl.onEnable();
        } else {
            getLogger().log(Level.SEVERE, "Cannot enable - JExHome failed to load");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (impl != null) {
            impl.onDisable();
        }
    }

    /**
     * Gets the implementation delegate.
     *
     * @return the implementation delegate
     */
    public JExHomeFreeImpl getImpl() {
        return impl;
    }
}
