package com.raindropcentral.rdq;

import com.raindropcentral.rdq.manager.RDQFreeManager;
import com.raindropcentral.rdq.manager.RDQManager;
import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import me.devnatan.inventoryframework.ViewFrame;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Free variant implementation of RaindropQuests.
 * <p>
 * This implementation provides limited functionality:
 * <ul>
 * <li>View-only bounty system with mock data</li>
 * <li>No bounty creation or modification</li>
 * <li>Limited to 1 bounty per player</li>
 * <li>Maximum 3 reward items per bounty</li>
 * </ul>
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public final class RDQFreeImpl extends AbstractPluginDelegate<RDQFree> {

    private static final Logger LOGGER = Logger.getLogger(RDQFreeImpl.class.getName());
    private static final String EDITION = "Free";

    private RDQ rdq;

    public RDQFreeImpl(final @NotNull RDQFree plugin) {
        super(plugin);
    }

    @Override
    public void onLoad() {
        try {
            LOGGER.info("Loading RDQ " + EDITION + " Edition");

            final RDQManager manager = new RDQFreeManager();
            manager.initialize();

            this.rdq = new RDQ(this.getPlugin(), EDITION, manager) {
                @Override
                protected @NotNull String getStartupMessage() {
                    return STARTUP_MESSAGE;
                }

                @Override
                protected int getMetricsId() {
                    return 25810;
                }

                @Override
                protected @NotNull ViewFrame registerViews(@NotNull ViewFrame viewFrame) {
                    return viewFrame;
                }
            };

            this.rdq.onLoad();

            LOGGER.info("RDQ " + EDITION + " Edition loaded successfully");
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to load RDQ " + EDITION, exception);
            throw exception;
        }
    }

    @Override
    public void onEnable() {
        try {
            this.rdq.onEnable();
            LOGGER.info(STARTUP_MESSAGE);
            LOGGER.info("RDQ " + EDITION + " Edition enabled successfully");
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to enable RDQ " + EDITION, exception);
        }
    }

    @Override
    public void onDisable() {
        try {
            if (this.rdq != null) {
                this.rdq.onDisable();
            }

            LOGGER.info("RDQ " + EDITION + " Edition disabled successfully");
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Error during RDQ " + EDITION + " shutdown", exception);
        }
    }

    public @NotNull RDQ getRDQ() {
        return this.rdq;
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
        Quest System: Enabled
        Rank System: Enabled
        Bounty System: Limited (View Only)
        Perk System: Enabled
        ===============================================================================================
        Language System: JExTranslate v3.0
        Adventure Components: Enabled
        ===============================================================================================
        """;
}