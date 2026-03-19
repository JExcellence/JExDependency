package de.jexcellence.multiverse;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import de.jexcellence.multiverse.service.FreeMultiverseService;
import de.jexcellence.multiverse.service.IMultiverseService;
import me.devnatan.inventoryframework.ViewFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation delegate for JExMultiverse Free Edition.
 * <p>
 * This class extends {@link AbstractPluginDelegate} and creates an anonymous
 * implementation of the abstract {@link JExMultiverse} class with free edition
 * specific values and limitations.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public final class JExMultiverseFreeImpl extends AbstractPluginDelegate<JExMultiverseFree> {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("JExMultiverse");
    private static final String EDITION = "Free";

    private @Nullable JExMultiverse jexMultiverse;

    public JExMultiverseFreeImpl(@NotNull JExMultiverseFree plugin) {
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
                    return -1; // No metrics for free edition
                }

                @Override
                protected @NotNull ViewFrame registerViews(@NotNull ViewFrame viewFrame) {
                    // Free edition uses default views from common module
                    return viewFrame;
                }

                @Override
                protected @NotNull IMultiverseService createMultiverseService() {
                    return FreeMultiverseService.initialize(getWorldRepository(), getWorldFactory());
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
            FreeMultiverseService.reset();
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
                         JExMultiverse - Free Edition
                    Product of JExcellence
    ===============================================================================================
    World Management: Enabled
    Max Worlds: 3
    World Types: DEFAULT, VOID
    ===============================================================================================
    Language System: JExTranslate v3.0
    Adventure Components: Enabled
    Database: Connected
    ===============================================================================================
    Upgrade to Premium for unlimited worlds and PLOT world type!
    ===============================================================================================
    """;
}
