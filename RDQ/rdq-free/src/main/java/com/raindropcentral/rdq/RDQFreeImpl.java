package com.raindropcentral.rdq;

import com.raindropcentral.rdq.bounty.FreeBountyService;
import com.raindropcentral.rdq.bounty.IBountyService;
import com.raindropcentral.rdq.rank.FreeRankSystemService;
import com.raindropcentral.rdq.rank.IRankSystemService;
import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import me.devnatan.inventoryframework.ViewFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation delegate for RDQ Free Edition.
 *
 * <p>This class handles all the actual plugin logic, separated from the main plugin class
 * to allow for proper dependency loading via JEDependency before any external classes
 * are referenced.
 * </p>
 *
 * @author JExcellence
 * @version 6.0.0
 * @since 6.0.0
 */
public final class RDQFreeImpl extends AbstractPluginDelegate<RDQFree> {

    private static final Logger LOGGER = Logger.getLogger(RDQFreeImpl.class.getName());
    private static final String EDITION = "Free";

    private @Nullable RDQ rdq;
    private @Nullable FreeBountyService bountyService;

    /**
     * Executes RDQFreeImpl.
     */
    public RDQFreeImpl(@NotNull RDQFree plugin) {
        super(plugin);
    }

    /**
     * Executes onLoad.
     */
    @Override
    public void onLoad() {
        try {
            // Initialize RDQ
            
            rdq = new RDQ(getPlugin(), EDITION) {
                @Override
                protected @NotNull String getStartupMessage() {
                    return STARTUP_MESSAGE;
                }

                @Override
                protected int getMetricsId() {
                    return 1690;
                }

                @Override
                protected @NotNull ViewFrame registerViews(@NotNull ViewFrame viewFrame) {
                    return viewFrame;
                }

                @Override
                protected @NotNull IBountyService createBountyService() {
                    return FreeBountyService.initialize(this);
                }

                @Override
                protected @NotNull IRankSystemService createRankSystemService() {
                    return FreeRankSystemService.initialize(this);
                }
            };

            //rdg.onLoad();
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to load RDQ (" + EDITION + ")", exception);
            throw new RuntimeException(exception);
        }
    }

    /**
     * Executes onEnable.
     */
    @Override
    public void onEnable() {
        if (rdq == null) {
            LOGGER.severe("Cannot enable - RDQ (" + EDITION + ") failed during onLoad.");
            this.getPlugin().getServer().getPluginManager().disablePlugin(this.getPlugin());
            return;
        }

        rdq.onEnable();
    }

    /**
     * Executes onDisable.
     */
    @Override
    public void onDisable() {
        try {
            if (this.rdq != null) {
                this.rdq.onDisable();
            }
            LOGGER.info("RDQ (" + EDITION + ") Edition disabled successfully");
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Error during RDQ (" + EDITION + ") shutdown", exception);
        }
    }

    private static final String STARTUP_MESSAGE = """
    ===============================================================================================
                ____     ____      ___
               |  _ \\   |  _ \\    / _ \\
               | |_) |  | | | |  | | | |
               |  _ <   | |_| |  | |_| |
               |_| \\_\\  |____/    \\__\\_\\
                    RaindropQuests - Free Edition
               Product of Antimatter Zone LLC
               Powered by JExcellence
    ===============================================================================================
    Rank System: Enabled (Limited - Single Tree)
    Bounty System: Enabled (Limited)
    Perk System: Enabled (Limited - Single Active Perk)
    ===============================================================================================
    Language System: JExTranslate v3.0
    Adventure Components: Enabled
    Database: Connected
    ===============================================================================================
    Upgrade to Premium for full features!
    ===============================================================================================
    """;
}
