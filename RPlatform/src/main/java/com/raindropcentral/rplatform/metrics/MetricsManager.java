/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rplatform.metrics;

import com.raindropcentral.rplatform.api.PlatformType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Discovers and initializes the relocated bStats metrics entry point via reflection so that the.
 * runtime can opt-in to metrics collection without directly depending on the shaded implementation.
 * The manager stores contextual information about the host plugin and platform variant to ensure the
 * reflective invocation receives the expected parameters.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class MetricsManager {

    /**
     * Plugin requesting metrics initialization; used for reflective construction and logging.
     */
    private final JavaPlugin plugin;

    /**
     * bStats service identifier assigned to the plugin.
     */
    private final int serviceId;

    /**
     * Platform type guiding whether Folia specific metrics should be toggled.
     */
    private final PlatformType platformType;

    /**
     * Active bStats metrics facade used to register custom charts.
     */
    private BStatsMetrics metrics;

    /**
     * Creates a new metrics manager that attempts to bootstrap the relocated metrics implementation.
     *
     * @param plugin       the plugin requesting metrics initialization
     * @param serviceId    the bStats service identifier to register submissions under
     * @param platformType the current server platform the plugin is running on
     */
    public MetricsManager(
            final @NotNull JavaPlugin plugin,
            final int serviceId,
            final @NotNull PlatformType platformType
    ) {
        this.plugin = plugin;
        this.serviceId = serviceId;
        this.platformType = platformType;
        
        initialize();
    }

    /**
     * Attempts to load and instantiate the relocated {@code Metrics} class via reflection. Any.
     * reflective failure is caught, with the stack trace suppressed and the message logged at
     * warning level so startup continues gracefully.
     */
    private void initialize() {
        try {
            this.metrics = new BStatsMetrics(plugin, serviceId, platformType == PlatformType.FOLIA);
        } catch (final Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to initialize metrics", e);
        }
    }

    /**
     * Registers a custom chart with the active bStats instance.
     *
     * @param chart chart to register for reporting
     */
    public void addCustomChart(final @NotNull BStatsMetrics.CustomChart chart) {
        if (this.metrics != null) {
            this.metrics.addCustomChart(chart);
        }
    }
}
