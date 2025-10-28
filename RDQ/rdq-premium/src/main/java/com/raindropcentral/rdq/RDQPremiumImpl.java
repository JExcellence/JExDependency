package com.raindropcentral.rdq;

import com.raindropcentral.rdq.manager.PremiumRDQManager;
import com.raindropcentral.rdq.manager.RDQManager;
import com.raindropcentral.rdq.service.BountyService;
import com.raindropcentral.rdq.service.BountyServiceProvider;
import com.raindropcentral.rdq.service.bounty.PremiumBountyService;
import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RDQPremiumImpl extends AbstractPluginDelegate<RDQPremium> {

    private static final Logger LOGGER = Logger.getLogger(RDQPremiumImpl.class.getName());
    private static final String EDITION = "Premium";

    private RDQ rdq;
    private BountyService bountyService;

    public RDQPremiumImpl(final @NotNull RDQPremium plugin) {
        super(plugin);
    }

    @Override
    public void onLoad() {
        try {
            this.rdq = new RDQ(this.getPlugin(), EDITION) {
                @Override
                protected @NotNull String getStartupMessage() {
                    return STARTUP_MESSAGE;
                }

                @Override
                protected int getMetricsId() {
                    return 25811;
                }

                @Override
                protected @NotNull ViewFrame registerViews(final @NotNull ViewFrame viewFrame) {
                    return viewFrame;
                }

                /**
                 * This is where the magic happens. This method is called by the RDQ base class
                 * at the correct point in the startup sequence.
                 */
                @Override
                protected @NotNull RDQManager initializeManager(@NotNull RDQ rdq) {
                    bountyService = new PremiumBountyService(getBountyRepository());
                    BountyServiceProvider.setInstance(bountyService);
                    registerServices();

                    RDQManager manager = new PremiumRDQManager(
                            rdq,
                            getPlugin(),
                            getPlatform(),
                            getExecutor(),
                            getBountyRepository(),
                            getPlayerRepository()
                    );
                    manager.initialize();

                    return manager;
                }
            };

            this.rdq.onLoad();

        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to load RDQ " + EDITION, exception);
            throw new RuntimeException(exception);
        }
    }

    @Override
    public void onEnable() {
        if (this.rdq == null) {
            LOGGER.severe("Cannot enable - RDQ Premium failed during onLoad.");
            this.getPlugin().getServer().getPluginManager().disablePlugin(this.getPlugin());
            return;
        }
        this.rdq.onEnable();
    }

    @Override
    public void onDisable() {
        try {
            unregisterServices();
            if (this.rdq != null) {
                this.rdq.onDisable();
            }
            LOGGER.info("RDQ " + EDITION + " Edition disabled successfully");
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Error during RDQ " + EDITION + " shutdown", exception);
        }
    }
	

    private void registerServices() {
        if (this.bountyService != null) {
            Bukkit.getServer().getServicesManager().register(
                    BountyService.class,
                    this.bountyService,
                    this.getPlugin(),
                    ServicePriority.High
            );
            LOGGER.info("Registered BountyService provider (Premium) with priority HIGH.");
        }
    }

    private void unregisterServices() {
        Optional.ofNullable(Bukkit.getServer().getServicesManager().getRegistration(BountyService.class))
                .ifPresent(registration -> {
                    if (registration.getProvider() == this.bountyService) {
                        Bukkit.getServer().getServicesManager().unregister(BountyService.class, this.bountyService);
                        LOGGER.info("Unregistered BountyService provider (Premium).");
                    }
                });
        BountyServiceProvider.reset();
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
    Quest System: Enabled (Full)
    Rank System: Enabled (Full)
    Bounty System: Enabled (Full)
    Perk System: Enabled (Full)
    ===============================================================================================
    Language System: JExTranslate v3.0
    Adventure Components: Enabled
    Database: Connected
    ===============================================================================================
    """;
}