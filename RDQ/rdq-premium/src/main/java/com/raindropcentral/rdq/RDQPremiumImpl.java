package com.raindropcentral.rdq;

import com.raindropcentral.rdq.api.spi.BountyPersistence;
import com.raindropcentral.rdq.api.spi.PersistenceRegistry;
import com.raindropcentral.rdq.api.spi.PlayerPersistence;
import com.raindropcentral.rdq.service.BountyService;
import com.raindropcentral.rdq.service.BountyServiceProvider;
import com.raindropcentral.rdq.service.PremiumBountyService;
import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Premium variant implementation of RaindropQuests.
 * <p>
 * This implementation provides full functionality:
 * <ul>
 * <li>Complete bounty system with database persistence</li>
 * <li>Unlimited bounties per player</li>
 * <li>Unlimited reward items</li>
 * <li>Full CRUD operations</li>
 * <li>Advanced features and customization</li>
 * </ul>
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
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
            LOGGER.info("Loading RDQ " + EDITION + " Edition");
            
            final PersistenceRegistry persistenceRegistry = createPremiumPersistenceRegistry();
            
            this.rdq = new RDQ(this.getPlugin(), EDITION, persistenceRegistry) {
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
            };
            
            this.rdq.onLoad();
            
            this.bountyService = new PremiumBountyService(
                    this.rdq.getBountyRepository(),
                    this.rdq.getPlayerRepository()
            );
            BountyServiceProvider.setInstance(this.bountyService);
            
            registerServices();
            
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
            this.getPlugin().getServer().getPluginManager().disablePlugin(this.getPlugin());
        }
    }

    @Override
    public void onDisable() {
        try {
            if (this.rdq != null) {
                this.rdq.onDisable();
            }
            
            unregisterServices();
            
            LOGGER.info("RDQ " + EDITION + " Edition disabled successfully");
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Error during RDQ " + EDITION + " shutdown", exception);
        }
    }

    public @NotNull RDQ getRDQ() {
        return this.rdq;
    }

    private PersistenceRegistry createPremiumPersistenceRegistry() {
        return new PersistenceRegistry() {
            @Override
            public java.util.Optional<BountyPersistence> getBountyPersistence() {
                return java.util.Optional.of(new PremiumBountyPersistence());
            }

            @Override
            public java.util.Optional<PlayerPersistence> getPlayerPersistence() {
                return java.util.Optional.of(new PremiumPlayerPersistence());
            }
        };
    }

    private void registerServices() {
        if (this.bountyService != null) {
            Bukkit.getServer().getServicesManager().register(
                    BountyService.class,
                    this.bountyService,
                    this.getPlugin(),
                    ServicePriority.High
            );
            LOGGER.info("Registered BountyService provider (Premium) with priority HIGH");
        }
    }

    private void unregisterServices() {
        if (this.bountyService != null) {
            Bukkit.getServer().getServicesManager().unregister(BountyService.class, this.bountyService);
            LOGGER.info("Unregistered BountyService provider (Premium)");
        }
        
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

    private static class PremiumBountyPersistence implements BountyPersistence {
    }

    private static class PremiumPlayerPersistence implements PlayerPersistence {
    }
}