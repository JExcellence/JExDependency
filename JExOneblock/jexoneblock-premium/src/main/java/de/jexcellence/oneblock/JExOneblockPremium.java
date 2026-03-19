package de.jexcellence.oneblock;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public class JExOneblockPremium extends JavaPlugin {

    private JExOneblockPremiumImpl impl;

    @Override
    public void onLoad() {
        try {
            JEDependency.initializeWithRemapping(this, JExOneblockPremium.class);
            impl = new JExOneblockPremiumImpl(this);
            impl.onLoad();
        } catch (Exception exception) {
            getLogger().log(Level.SEVERE, "Failed to load JExOneblock Premium", exception);
            impl = null;
        }
    }

    @Override
    public void onEnable() {
        if (impl != null) {
            impl.onEnable();
        } else {
            getLogger().severe("Cannot enable - JExOneblock failed to load");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (impl != null) {
            impl.onDisable();
        }
    }

    public @NotNull JExOneblockPremiumImpl getImpl() {
        return impl;
    }
}
