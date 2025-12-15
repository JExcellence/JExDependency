package de.jexcellence.economy;

import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import me.devnatan.inventoryframework.ViewFrame;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class JExEconomyPremiumImpl extends AbstractPluginDelegate<JExEconomyPremium> {

    private static final Logger LOGGER = Logger.getLogger(JExEconomyPremiumImpl.class.getName());
    private static final String EDITION = "Premium";

    private JExEconomy economy;

    public JExEconomyPremiumImpl(final @NotNull JExEconomyPremium plugin) {
        super(plugin);
    }

    @Override
    public void onLoad() {
        try {
            this.economy = new JExEconomy(this.getPlugin(), EDITION) {
                @Override
                protected @NotNull String getStartupMessage() {
                    return STARTUP_MESSAGE;
                }

                @Override
                protected int getMetricsId() {
                    return 0;
                }

                @Override
                protected @NotNull ViewFrame registerViews(final @NotNull ViewFrame viewFrame) {
                    return viewFrame;
                }
            };

            this.economy.onLoad();

        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to load JExEconomy " + EDITION, exception);
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void onEnable() {
        if (this.economy == null) {
            LOGGER.severe("Cannot enable - JExEconomy Premium failed during onLoad.");
            this.getPlugin().getServer().getPluginManager().disablePlugin(this.getPlugin());
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
            LOGGER.info("JExEconomy " + EDITION + " Edition disabled successfully");
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Error during JExEconomy " + EDITION + " shutdown", exception);
        }
    }

    private static final String STARTUP_MESSAGE = """
    ===============================================================================================
         __        ___      ______ _____ _   _  ______ _____ ___   _   _  _____ __  __  _   _
         \\ \\      / / |    |  ____/ ____| \\ | |/ __ \\_   _|__ \\ | \\ | |/ ____|  \\/  || \\ | |
          \\ \\ /\\ / /| |    | |__ | |    |  \\| | |  | || |    ) ||  \\| | |    | \\  / ||  \\| |
           \\ V  V / | |    |  __|| |    | . ` | |  | || |   / / | . ` | |    | |\\/| || . ` |
            \\_/\\_/  | |____| |___| |____| |\\  | |__| || |_ / /_ | |\\  | |____| |  | || |\\  |
                    |______|______\\_____|_| \\_|\\____/_____|____||_| \\_|\\_____|_|  |_||_| \\_|
    
                          JExEconomy - Premium Edition
                          Product of JExcellence
    ===============================================================================================
    Multi-Currency System: Enabled (Full)
    Vault Integration: Enabled
    PlaceholderAPI: Enabled
    Advanced Analytics: Enabled
    ===============================================================================================
    Language System: JExTranslate v3.0
    Adventure Components: Enabled
    Database: Connected
    ===============================================================================================
    """;
}
