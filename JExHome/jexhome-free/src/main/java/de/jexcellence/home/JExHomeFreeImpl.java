package de.jexcellence.home;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import de.jexcellence.home.service.FreeHomeService;
import de.jexcellence.home.service.IHomeService;
import me.devnatan.inventoryframework.ViewFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation delegate for JExHome Free Edition.
 *
 * @author JExcellence
 * @version 1.0.0
 */
public final class JExHomeFreeImpl extends AbstractPluginDelegate<JExHomeFree> {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("JExHome");
    private static final String EDITION = "Free";

    private @Nullable JExHome jexHome;

    public JExHomeFreeImpl(@NotNull JExHomeFree plugin) {
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
                    return FreeHomeService.initialize(getHomeRepository());
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
                         JExHome - Free Edition
                    Product of JExcellence
    ===============================================================================================
    Home System: Enabled
    Teleport Delay: Configurable
    Home Limits: Permission-based
    ===============================================================================================
    Language System: JExTranslate v3.0
    Adventure Components: Enabled
    Database: Connected
    ===============================================================================================
    Upgrade to Premium for unlimited homes and advanced features!
    ===============================================================================================
    """;
}
