package de.jexcellence.oneblock;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class JExOneblockFree extends JavaPlugin {

    private JExOneblockFreeImpl impl;

    @Override
    public void onLoad() {
        try {
            JEDependency.initializeWithRemapping(this, JExOneblockFree.class);
            impl = new JExOneblockFreeImpl(this);
            impl.onLoad();
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "Failed to load JExOneblock", exception);
            impl = null;
        }
    }

    @Override
    public void onEnable() {
        if (impl != null) {
            impl.onEnable();
        } else {
            getLogger().log(Level.SEVERE, "Cannot enable - JExOneblock failed to load");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (impl != null) {
            impl.onDisable();
        }
    }

    public JExOneblockFreeImpl getImpl() {
        return impl;
    }
}
