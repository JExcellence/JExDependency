package de.jexcellence.multiverse;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import de.jexcellence.multiverse.service.IMultiverseService;
import de.jexcellence.multiverse.service.PremiumMultiverseService;
import me.devnatan.inventoryframework.ViewFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation delegate for JExMultiverse Premium Edition.
 * <p>
 * This class extends {@link AbstractPluginDelegate} and creates an anonymous
 * implementation of the abstract {@link JExMultiverse} class with premium edition
 * specific values and full functionality.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public final class JExMultiversePremiumImpl extends AbstractPluginDelegate<JExMultiversePremium> {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("JExMultiverse");
    private static final String EDITION = "Premium";
    private static final int METRICS_ID = 12345; // Replace with actual bStats metrics ID

    private @Nullable JExMultiverse jexMultiverse;

    public JExMultiversePremiumImpl(@NotNull JExMultiversePremium plugin) {
        super(plugin);
    }

    @Override
    public void onLoad() {
        try {
            jexMultiverse = new JExMultiverse(getPlugin(), EDITION) {
                @Override
                protected @NotNull String getStartupMessage() {
                    return STARTUP_MESSAGE;
                }

                @Override
                protected int getMetricsId() {
                    return METRICS_ID;
                }

                @Override
                protected @NotNull ViewFrame registerViews(@NotNull ViewFrame viewFrame) {
                    // Premium edition can register additional views here
                    return viewFrame;
                }

                @Override
                protected @NotNull IMultiverseService createMultiverseService() {
                    return PremiumMultiverseService.initialize(getWorldRepository(), getWorldFactory());
                }
            };

            jexMultiverse.onLoad();
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to load JExMultiverse (" + EDITION + ")", exception);
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void onEnable() {
        if (jexMultiverse == null) {
            LOGGER.severe("Cannot enable - JExMultiverse (" + EDITION + ") failed during onLoad.");
            getPlugin().getServer().getPluginManager().disablePlugin(getPlugin());
            return;
        }

        jexMultiverse.onEnable();
    }

    @Override
    public void onDisable() {
        try {
            if (jexMultiverse != null) {
                jexMultiverse.onDisable();
            }
            PremiumMultiverseService.reset();
            LOGGER.info("JExMultiverse (" + EDITION + ") Edition disabled successfully");
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Error during JExMultiverse (" + EDITION + ") shutdown", exception);
        }
    }

    /**
     * Gets the JExMultiverse instance.
     *
     * @return the JExMultiverse instance, or null if not initialized
     */
    public @Nullable JExMultiverse getJExMultiverse() {
        return jexMultiverse;
    }

    private static final String STARTUP_MESSAGE = """
    ===============================================================================================
                     _  _____      __  __       _ _   _
                    | || ____|_  _|  \\/  |_   _| | |_(_)_   _____ _ __ ___  ___
                 _  | ||  _| \\ \\/ / |\\/| | | | | | __| \\ \\ / / _ \\ '__/ __|/ _ \\
                | |_| || |___ >  <| |  | | |_| | | |_| |\\ V /  __/ |  \\__ \\  __/
                 \\___/ |_____/_/\\_\\_|  |_|\\__,_|_|\\__|_| \\_/ \\___|_|  |___/\\___|
                         JExMultiverse - Premium Edition
                    Product of JExcellence
    ===============================================================================================
    World Management: Enabled
    Max Worlds: Unlimited
    World Types: DEFAULT, VOID, PLOT
    ===============================================================================================
    Language System: JExTranslate v3.0
    Adventure Components: Enabled
    Database: Connected
    ===============================================================================================
    Thank you for supporting JExcellence with Premium!
    ===============================================================================================
    """;
}
