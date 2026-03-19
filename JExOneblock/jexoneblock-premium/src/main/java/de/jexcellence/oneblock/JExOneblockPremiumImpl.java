package de.jexcellence.oneblock;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import de.jexcellence.oneblock.service.IOneblockService;
import de.jexcellence.oneblock.service.PremiumOneblockService;
import me.devnatan.inventoryframework.ViewFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JExOneblockPremiumImpl extends AbstractPluginDelegate<JExOneblockPremium> {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("JExOneblock");
    private static final String EDITION = "Premium";

    private @Nullable JExOneblock jexOneblock;

    public JExOneblockPremiumImpl(@NotNull JExOneblockPremium plugin) {
        super(plugin);
    }

    @Nullable
    public JExOneblock getJExOneblock() {
        return jexOneblock;
    }

    @Override
    public void onLoad() {
        try {
            jexOneblock = new JExOneblock(getPlugin(), EDITION) {
                @Override
                protected @NotNull String getStartupMessage() {
                    return STARTUP_MESSAGE;
                }

                @Override
                protected int getMetricsId() {
                    return 12346;
                }

                @Override
                protected @NotNull ViewFrame registerViews(@NotNull ViewFrame viewFrame) {
                    try {
                        return viewFrame.with(
                            new de.jexcellence.oneblock.view.island.BiomeSelectionView(),
                            new de.jexcellence.oneblock.view.generator.GeneratorBrowserView(),
                            new de.jexcellence.oneblock.view.generator.GeneratorDesignDetailView()
                        );
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to register some views: " + e.getMessage(), e);
                        return viewFrame;
                    }
                }

                @Override
                protected @NotNull IOneblockService createOneblockService() {
                    var worldManagementService = new de.jexcellence.oneblock.service.WorldManagementService(
                        Executors.newCachedThreadPool(),
                        null
                    );
                    
                    return new PremiumOneblockService(
                        getOneblockIslandRepository(), 
                        getOneblockPlayerRepository(),
                        worldManagementService,
                        this // Pass the plugin instance
                    );
                }
            };
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to load JExOneblock (" + EDITION + ")", exception);
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void onEnable() {
        if (jexOneblock == null) {
            LOGGER.severe("Cannot enable - JExOneblock (" + EDITION + ") failed during onLoad.");
            getPlugin().getServer().getPluginManager().disablePlugin(getPlugin());
            return;
        }

        jexOneblock.onEnable();
    }

    @Override
    public void onDisable() {
        try {
            if (jexOneblock != null) {
                jexOneblock.onDisable();
            }
            LOGGER.info("JExOneblock (" + EDITION + ") Edition disabled successfully");
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Error during JExOneblock (" + EDITION + ") shutdown", exception);
        }
    }

    private static final String STARTUP_MESSAGE = """
    ===============================================================================================
                     _  _____      ___             _     _            _
                    | || ____|_  _/ _ \\ _ __   ___| |__ | | ___   ___| | __
                 _  | ||  _| \\ \\/ / | | | '_ \\ / _ \\ '_ \\| |/ _ \\ / __| |/ /
                | |_| || |___ >  <| |_| | | | |  __/ |_) | | (_) | (__|   <
                 \\___/ |_____/_/\\_\\\\___/|_| |_|\\___|_.__/|_|\\___/ \\___|_|\\_\\
                         JExOneblock - Premium Edition
                    Product of JExcellence
    ===============================================================================================
    Oneblock System: Enabled (Unlimited)
    Island Limit: Unlimited
    Block Phases: Advanced
    Premium Features: Enabled
    ===============================================================================================
    Language System: JExTranslate v3.0
    Adventure Components: Enabled
    Database: Connected
    ===============================================================================================
    Thank you for supporting JExcellence!
    ===============================================================================================
    """;
}
