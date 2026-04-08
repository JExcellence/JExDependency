package de.jexcellence.home;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import de.jexcellence.home.service.IHomeService;
import de.jexcellence.home.service.PremiumHomeService;
import me.devnatan.inventoryframework.ViewFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation delegate for JExHome Premium Edition.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public final class JExHomePremiumImpl extends AbstractPluginDelegate<JExHomePremium> {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("JExHome");
    private static final String EDITION = "Premium";

    private @Nullable JExHome jexHome;

    public JExHomePremiumImpl(@NotNull JExHomePremium plugin) {
        super(plugin);
    }

    @Override
    public void onLoad() {
        try {
            jexHome = new JExHome(getPlugin(), EDITION) {
                @Override
                protected @NotNull String getStartupMessage() {
                    return STARTUP_MESSAGE;
                }

                @Override
                protected int getMetricsId() {
                    return -1;
                }

                @Override
                protected @NotNull ViewFrame registerViews(@NotNull ViewFrame viewFrame) {
                    return viewFrame;
                }

                @Override
                protected @NotNull IHomeService createHomeService() {
                    return PremiumHomeService.initialize(getHomeRepository(), getHomeConfig());
                }
            };

            jexHome.onLoad();
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to load JExHome (" + EDITION + ")", exception);
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void onEnable() {
        if (jexHome == null) {
            LOGGER.severe("Cannot enable - JExHome (" + EDITION + ") failed during onLoad.");
            getPlugin().getServer().getPluginManager().disablePlugin(getPlugin());
            return;
        }

        jexHome.onEnable();
    }

    @Override
    public void onDisable() {
        try {
            if (jexHome != null) {
                jexHome.onDisable();
            }
            LOGGER.info("JExHome (" + EDITION + ") Edition disabled successfully");
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Error during JExHome (" + EDITION + ") shutdown", exception);
        }
    }

    /**
     * Gets the JExHome instance.
     *
     * @return the JExHome instance, or null if not initialized
     */
    public @Nullable JExHome getJExHome() {
        return jexHome;
    }

    private static final String STARTUP_MESSAGE = """
    ===============================================================================================
                     _  _____      _   _
                    | || ____|_  _| | | | ___  _ __ ___   ___
                 _  | ||  _| \\ \\/ / |_| |/ _ \\| '_ ` _ \\ / _ \\
                | |_| || |___ >  <|  _  | (_) | | | | | |  __/
                 \\___/ |_____/_/\\_\\_| |_|\\___/|_| |_| |_|\\___|
                         JExHome - Premium Edition
                    Product of JExcellence
    ===============================================================================================
    Home System: Enabled (Unlimited)
    Teleport Delay: Configurable
    Home Limits: Unlimited
    Advanced Features: Enabled
    ===============================================================================================
    Language System: JExTranslate v3.0
    Adventure Components: Enabled
    Database: Connected
    ===============================================================================================
    Thank you for supporting JExcellence!
    ===============================================================================================
    """;
}
