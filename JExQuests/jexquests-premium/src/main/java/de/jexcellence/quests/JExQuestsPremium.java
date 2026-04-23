package de.jexcellence.quests;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Bukkit entrypoint for the Premium edition of JExQuests.
 */
public final class JExQuestsPremium extends JavaPlugin {

    private JExQuestsPremiumImpl implementation;

    @Override
    public void onLoad() {
        try {
            JEDependency.initializeWithRemapping(this, JExQuestsPremium.class);
            this.implementation = new JExQuestsPremiumImpl(this);
            this.implementation.onLoad();
        } catch (final Exception ex) {
            this.getLogger().log(Level.SEVERE, "[JExQuests-Premium] Failed to load", ex);
            this.implementation = null;
        }
    }

    @Override
    public void onEnable() {
        if (this.implementation != null) {
            this.implementation.onEnable();
        } else {
            this.getLogger().severe("Cannot enable - JExQuests Premium failed to load");
            this.getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (this.implementation != null) this.implementation.onDisable();
    }

    public JExQuestsPremiumImpl getImpl() { return this.implementation; }
}
