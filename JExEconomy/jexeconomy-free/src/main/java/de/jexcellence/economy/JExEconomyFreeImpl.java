package de.jexcellence.economy;

import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Free edition delegate. Bootstraps and delegates lifecycle to {@link JExEconomy}.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public final class JExEconomyFreeImpl extends AbstractPluginDelegate<JExEconomyFree> {

    private static final Logger LOGGER = Logger.getLogger(JExEconomyFreeImpl.class.getName());
    private static final String EDITION = "Free";

    private JExEconomy economy;

    public JExEconomyFreeImpl(@NotNull JExEconomyFree plugin) {
        super(plugin);
    }

    @Override
    public void onLoad() {
        try {
            this.economy = new JExEconomy(getPlugin(), EDITION) {
                @Override
                protected int metricsId() {
                    return 0;
                }
            };
            this.economy.onLoad();
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to load JExEconomy " + EDITION, ex);
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void onEnable() {
        if (this.economy == null) {
            LOGGER.severe("Cannot enable — JExEconomy Free failed during onLoad.");
            getPlugin().getServer().getPluginManager().disablePlugin(getPlugin());
            return;
        }
        this.economy.onEnable();
    }

    @Override
    public void onDisable() {
        try {
            if (this.economy != null) {
                this.economy.onDisable();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Error during JExEconomy " + EDITION + " shutdown", ex);
        }
    }
}
