package com.raindropcentral.rdq;

import com.raindropcentral.rdq.manager.RDQFreeManager;
import com.raindropcentral.rdq.manager.RDQManager;
import com.raindropcentral.rdq.service.BountyService;
import com.raindropcentral.rdq.service.BountyServiceProvider;
import com.raindropcentral.rdq.service.FreeBountyService;
import de.jexcellence.dependency.delegate.AbstractPluginDelegate;
import me.devnatan.inventoryframework.ViewFrame;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Free variant implementation of RaindropQuests.
 * <p>
 * This implementation provides limited functionality by wiring up services
 * and managers suitable for the free tier. It correctly hooks into the
 * asynchronous startup sequence of the abstract RDQ class.
 * </p>
 *
 * @author JExcellence
 * @version 3.0.0
 * @since 2.0.0
 */
public final class RDQFreeImpl extends AbstractPluginDelegate<RDQFree> {

    private static final Logger LOGGER = Logger.getLogger(RDQFreeImpl.class.getName());
    private static final String EDITION = "Free";

    private RDQ rdq;
    private BountyService bountyService;

    public RDQFreeImpl(final @NotNull RDQFree plugin) {
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
                    return 25810; // Free Edition Metrics ID
                }

                @Override
                protected @NotNull ViewFrame registerViews(@NotNull ViewFrame viewFrame) {
                    return viewFrame;
                }

                /**
                 * This method is called by the RDQ base class at the correct point in the
                 * startup sequence, after core components are ready.
                 */
                @Override
                protected @NotNull RDQManager initializeManager() {
                    // 1. Initialize services.
                    bountyService = new FreeBountyService();
                    BountyServiceProvider.setInstance(bountyService);
                    registerServices();

                    // 2. Initialize the main manager, passing it all necessary dependencies.
                    RDQManager manager = new RDQFreeManager(getPlugin(), getPlatform());
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
            LOGGER.severe("Cannot enable - RDQ Free failed during onLoad.");
            this.getPlugin().getServer().getPluginManager().disablePlugin(this.getPlugin());
            return;
        }
        // Delegate to the base class, which handles the full async startup chain.
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



    public @NotNull RDQ getRDQ() {
        return this.rdq;
    }

    private void registerServices() {
        if (this.bountyService != null) {
            Bukkit.getServer().getServicesManager().register(
                    BountyService.class,
                    this.bountyService,
                    this.getPlugin(),
                    ServicePriority.Normal // Free version uses Normal priority
            );
            LOGGER.info("Registered BountyService provider (Free) with priority NORMAL.");
        }
    }

    private void unregisterServices() {
        Optional.ofNullable(Bukkit.getServer().getServicesManager().getRegistration(BountyService.class))
                .ifPresent(registration -> {
                    if (registration.getProvider() == this.bountyService) {
                        Bukkit.getServer().getServicesManager().unregister(BountyService.class, this.bountyService);
                        LOGGER.info("Unregistered BountyService provider (Free).");
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