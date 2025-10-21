package de.jexcellence.economy;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class JExEconomy extends JavaPlugin {

    private JExEconomyImpl jexEconomyImpl;

    @Override
    public void onLoad() {
        try {
            JEDependency.initializeWithRemapping(this, JExEconomy.class);
            this.jexEconomyImpl = (JExEconomyImpl) Class.forName("de.jexcellence.economy.JExEconomyImpl").getDeclaredConstructor(JExEconomy.class).newInstance(this);
            this.jexEconomyImpl.onLoad();
        } catch (
            final Exception exception
        ) {
	        this.getLogger().log(Level.SEVERE, "[JExEconomy] Failed to load JExEconomy", exception);
	        this.jexEconomyImpl = null;
        }
    }

    @Override
    public void onEnable() {
        if (this.jexEconomyImpl != null) {
            this.jexEconomyImpl.onEnable();
        }
    }

    @Override
    public void onDisable() {
        if (this.jexEconomyImpl != null) {
            this.jexEconomyImpl.onDisable();
        }
    }

    public JExEconomyImpl getImpl() {
        return this.jexEconomyImpl;
    }
}
