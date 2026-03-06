package com.raindropcentral.rdq;

import com.raindropcentral.rdq.bounty.IBountyService;
import com.raindropcentral.rdq.bounty.PremiumBountyService;
import com.raindropcentral.rdq.rank.IRankSystemService;
import com.raindropcentral.rdq.rank.PremiumRankSystemService;
import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import me.devnatan.inventoryframework.ViewFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation delegate for RDQ Premium Edition.
 * <p>
 * This class handles all the actual plugin logic, separated from the main plugin class
 * to allow for proper dependency loading via JEDependency before any external classes
 * are referenced.
 * </p>
 *
 * @author JExcellence
 * @version 6.0.0
 * @since 6.0.0
 */
public final class RDQPremiumImpl extends AbstractPluginDelegate<RDQPremium> {

    private static final Logger LOGGER = Logger.getLogger(RDQPremiumImpl.class.getName());
    private static final String EDITION = "Premium";

    private @Nullable RDQ rdq;

    public RDQPremiumImpl(@NotNull RDQPremium plugin) {
        super(plugin);
    }

    /**
     * Gets the RDQ instance.
     *
     * @return the RDQ instance, or null if not initialized
     */
    @Nullable
    public RDQ getRdq() {
        return rdq;
    }

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
                    return PremiumBountyService.initialize(this);
                }

                @Override
                protected @NotNull IRankSystemService createRankSystemService() {
                    // Create Premium rank system service with no limits
                    return PremiumRankSystemService.initialize(this);
                }
            };
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to load RDQ (" + EDITION + ")", exception);
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void onEnable() {
        if (rdq == null) {
            LOGGER.severe("Cannot enable - RDQ (" + EDITION + ") failed during onLoad.");
            this.getPlugin().getServer().getPluginManager().disablePlugin(this.getPlugin());
            return;
        }

        rdq.onEnable();
    }

    @Override
    public void onDisable() {
        try {
            if (this.rdq != null) {
                //this.rdq.onDisable();
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
                    RaindropQuests - Premium Edition
               Product of Antimatter Zone LLC
               Powered by JExcellence
    ===============================================================================================
    Rank System: Enabled (Full - Multiple Trees)
    Bounty System: Enabled (Full - Advanced Distribution)
    Perk System: Enabled (Full - Multiple Active Perks)
    ===============================================================================================
    Language System: JExTranslate v3.0
    Adventure Components: Enabled
    Database: Connected
    ===============================================================================================
    """;
}
