package de.jexcellence.economy;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class JExEconomyPremium extends JavaPlugin {

    private JExEconomyPremiumImpl jexEconomyImpl;

    @Override
    public void onLoad() {
        try {
            JEDependency.initializeWithRemapping(this, JExEconomyPremium.class);
            this.jexEconomyImpl = new JExEconomyPremiumImpl(this);
            this.jexEconomyImpl.onLoad();
        } catch (Exception exception) {
            this.getLogger().log(Level.SEVERE, "[JExEconomy-Premium] Failed to load", exception);
            this.jexEconomyImpl = null;
        }
    }

    @Override
    public void onEnable() {
        if (this.jexEconomyImpl != null) {
            this.jexEconomyImpl.onEnable();
        } else {
            this.getLogger().severe("Cannot enable - JExEconomy Premium failed to load");
            this.getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (this.jexEconomyImpl != null) {
            this.jexEconomyImpl.onDisable();
        }
    }

    public JExEconomyPremiumImpl getImpl() {
        return this.jexEconomyImpl;
    }
}
