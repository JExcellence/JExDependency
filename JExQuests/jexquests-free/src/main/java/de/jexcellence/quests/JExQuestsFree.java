package de.jexcellence.quests;

import de.jexcellence.dependency.JEDependency;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Bukkit entrypoint for the Free edition of JExQuests.
 */
public final class JExQuestsFree extends JavaPlugin {

    private JExQuestsFreeImpl implementation;

    @Override
    public void onLoad() {
        try {
            JEDependency.initializeWithRemapping(this, JExQuestsFree.class);
            this.implementation = new JExQuestsFreeImpl(this);
            this.implementation.onLoad();
        } catch (final Exception ex) {
            this.getLogger().log(Level.SEVERE, "[JExQuests-Free] Failed to load", ex);
            this.implementation = null;
        }
    }

    @Override
    public void onEnable() {
        if (this.implementation != null) {
            this.implementation.onEnable();
        } else {
            this.getLogger().severe("Cannot enable - JExQuests Free failed to load");
            this.getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (this.implementation != null) this.implementation.onDisable();
    }

    public JExQuestsFreeImpl getImpl() { return this.implementation; }
}
