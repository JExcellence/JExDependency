package de.jexcellence.economy;

import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Premium edition delegate. Bootstraps and delegates lifecycle to {@link JExEconomy}.
 *
 * @author JExcellence
 * @since 3.0.0
 */
public final class JExEconomyPremiumImpl extends AbstractPluginDelegate<JExEconomyPremium> {

    private static final Logger LOGGER = Logger.getLogger(JExEconomyPremiumImpl.class.getName());
    private static final String EDITION = "Premium";

    private JExEconomy economy;

    public JExEconomyPremiumImpl(@NotNull JExEconomyPremium plugin) {
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
            LOGGER.severe("Cannot enable — JExEconomy Premium failed during onLoad.");
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
