package com.raindropcentral.core;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class RCoreFree extends JavaPlugin {
    
    private RCoreFreeImpl rCoreImpl;

    @Override
    public void onLoad() {
        try {
            JEDependency.initializeWithRemapping(this, RCoreFree.class);
            this.rCoreImpl = (RCoreFreeImpl) Class.forName("com.raindropcentral.core.RCoreFreeImpl").getDeclaredConstructor(RCoreFree.class).newInstance(this);
            this.rCoreImpl.onLoad();
        } catch (final Exception exception) {
            this.getLogger().log(Level.SEVERE, "[RCore] Failed to load RCore", exception);
            this.rCoreImpl = null;
        }
    }

    @Override
    public void onEnable() {

        if (this.rCoreImpl != null) {
            this.rCoreImpl.onEnable();
        } else {
            this.getLogger().severe("[RCore] Cannot enable - RCore failed to load");
            this.getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (this.rCoreImpl != null) {
            this.rCoreImpl.onDisable();
        }
    }

    public RCoreFreeImpl getImpl() {
        return this.rCoreImpl;
    }
}
