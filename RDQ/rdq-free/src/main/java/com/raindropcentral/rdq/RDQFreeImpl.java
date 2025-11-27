package com.raindropcentral.rdq;

import com.raindropcentral.rdq.api.FreeBountyService;
import com.raindropcentral.rdq.api.FreePerkService;
import com.raindropcentral.rdq.api.FreeRankService;
import com.raindropcentral.rdq.shared.edition.EditionFeatures;
import com.raindropcentral.rdq.shared.edition.FreeEditionFeatures;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation delegate for RDQ Free Edition.
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
public final class RDQFreeImpl extends AbstractPluginDelegate<RDQFree> {

    private static final String EDITION = "Free";
    private static final Logger LOGGER = CentralLogger.getLogger(RDQFreeImpl.class);

    private @Nullable RDQCore core;
    private @Nullable EditionFeatures editionFeatures;
    private @Nullable FreeRankService rankService;
    private @Nullable FreePerkService perkService;
    private @Nullable FreeBountyService bountyService;

    public RDQFreeImpl(@NotNull RDQFree plugin) {
        super(plugin);
    }

    @Override
    public void onLoad() {
        CentralLogger.initialize(getPlugin());
        LOGGER.info("Loading RDQ " + EDITION + " Edition v" + getVersion());
    }

    @Override
    public void onEnable() {
        LOGGER.info("Starting RDQ " + EDITION + " Edition v" + getVersion());

        try {
            editionFeatures = new FreeEditionFeatures();
            core = new RDQCore(getPlugin(), EDITION, editionFeatures);

            core.initialize()
                .thenRun(this::initializeServices)
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Failed to initialize RDQ " + EDITION, ex);
                    getServer().getPluginManager().disablePlugin(getPlugin());
                    return null;
                });
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to enable RDQ " + EDITION, e);
            getServer().getPluginManager().disablePlugin(getPlugin());
        }
    }

    @Override
    public void onDisable() {
        LOGGER.info("Disabling RDQ " + EDITION + " Edition...");
        
        if (core != null) {
            core.shutdown();
        }
        
        LOGGER.info("RDQ " + EDITION + " Edition disabled");
    }

    private void initializeServices() {
        if (core == null) {
            LOGGER.severe("Cannot initialize services - core is null");
            return;
        }

        LOGGER.info("Initializing " + EDITION + " edition services...");

        rankService = core.createFreeRankService();
        if (rankService != null) {
            core.registerRankService(rankService);
            LOGGER.info("Registered FreeRankService (single active tree)");
        }

        perkService = core.createFreePerkService();
        if (perkService != null) {
            core.registerPerkService(perkService);
            LOGGER.info("Registered FreePerkService (single active perk)");
        }

        bountyService = core.createFreeBountyService();
        if (bountyService != null) {
            core.registerBountyService(bountyService);
            LOGGER.info("Registered FreeBountyService");
        }

        core.registerServiceDependentViews();

        LOGGER.info("RDQ " + EDITION + " Edition services initialized successfully!");
        printStartupBanner();
    }

    private void printStartupBanner() {
        LOGGER.info(STARTUP_MESSAGE);
    }

    @NotNull
    public RDQCore getCore() {
        if (core == null) {
            throw new IllegalStateException("RDQCore not initialized");
        }
        return core;
    }

    @NotNull
    public EditionFeatures getEditionFeatures() {
        if (editionFeatures == null) {
            throw new IllegalStateException("EditionFeatures not initialized");
        }
        return editionFeatures;
    }

    @Nullable
    public FreeRankService getRankService() {
        return rankService;
    }

    @Nullable
    public FreePerkService getPerkService() {
        return perkService;
    }

    @Nullable
    public FreeBountyService getBountyService() {
        return bountyService;
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
