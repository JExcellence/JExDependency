package de.jexcellence.quests;

import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Free edition delegate. Instantiates a {@link JExQuests} orchestrator
 * and forwards lifecycle events.
 */
public final class JExQuestsFreeImpl extends AbstractPluginDelegate<JExQuestsFree> {

    private static final Logger LOGGER = Logger.getLogger(JExQuestsFreeImpl.class.getName());

    private JExQuests quests;

    public JExQuestsFreeImpl(@NotNull JExQuestsFree plugin) {
        super(plugin);
    }

    @Override
    public void onLoad() {
        try {
            this.quests = new JExQuests(getPlugin(), "Free") {
                @Override protected int metricsId() { return 0; }
            };
            this.quests.onLoad();
        } catch (final Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to load JExQuests Free", ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void onEnable() {
        if (this.quests == null) {
            LOGGER.severe("Cannot enable - JExQuests Free failed during onLoad.");
            getPlugin().getServer().getPluginManager().disablePlugin(getPlugin());
            return;
        }
        this.quests.onEnable();
    }

    @Override
    public void onDisable() {
        try {
            if (this.quests != null) this.quests.onDisable();
        } catch (final Exception ex) {
            LOGGER.log(Level.SEVERE, "Error during JExQuests Free shutdown", ex);
        }
    }
}
