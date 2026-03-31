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

package com.raindropcentral.core.service.statistics;

import com.raindropcentral.core.service.central.RCentralApiClient;
import com.raindropcentral.core.service.central.RCentralService;
import com.raindropcentral.core.service.statistics.collector.NativeStatisticCollector;
import com.raindropcentral.core.service.statistics.collector.PlayerStatisticCollector;
import com.raindropcentral.core.service.statistics.collector.ServerMetricsCollector;
import com.raindropcentral.core.service.statistics.command.StatisticsDeliveryCommand;
import com.raindropcentral.core.service.statistics.config.StatisticsDeliveryConfig;
import com.raindropcentral.core.service.statistics.queue.StatisticsQueueManager;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Factory for creating and initializing the StatisticsDeliveryService.
 * Provides a convenient way to integrate statistics delivery into RCore.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class StatisticsDeliveryServiceFactory {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RCore");

    /**
     * Creates and initializes a StatisticsDeliveryService.
     *
     * @param plugin          the plugin instance
     * @param rCentralService the RCentral service for API access
     * @return the initialized service, or null if disabled
     */
    public static @Nullable StatisticsDeliveryService create(
        final @NotNull Plugin plugin,
        final @NotNull RCentralService rCentralService
    ) {
        // Load configuration
        StatisticsDeliveryConfig config = new StatisticsDeliveryConfig(plugin);

        if (!config.isEnabled()) {
            LOGGER.info("Statistics delivery is disabled in configuration");
            return null;
        }

        // Get API client and credentials from RCentralService
        RCentralApiClient apiClient = rCentralService.getApiClient();
        String apiKey = rCentralService.getApiKey();
        UUID serverUuidObj = rCentralService.getServerUuid();

        if (apiClient == null || apiKey == null || serverUuidObj == null) {
            LOGGER.warning("RCentral service not fully initialized, statistics delivery disabled");
            return null;
        }

        String serverUuid = serverUuidObj.toString();

        // Create queue manager
        StatisticsQueueManager queueManager = new StatisticsQueueManager(plugin, config);

        // Create collectors
        PlayerStatisticCollector playerCollector = new PlayerStatisticCollector(config);
        NativeStatisticCollector nativeCollector = new NativeStatisticCollector(config);
        ServerMetricsCollector serverMetricsCollector = new ServerMetricsCollector(plugin);

        // Create service
        StatisticsDeliveryService service = new StatisticsDeliveryService(
            plugin, config, apiKey, serverUuid, apiClient,
            queueManager, playerCollector, nativeCollector, serverMetricsCollector
        );

        // Register command if plugin is a JavaPlugin
        if (plugin instanceof org.bukkit.plugin.java.JavaPlugin javaPlugin) {
            var command = javaPlugin.getCommand("rcstats");
            if (command != null) {
                StatisticsDeliveryCommand statsCommand = new StatisticsDeliveryCommand(service);
                command.setExecutor(statsCommand);
                command.setTabCompleter(statsCommand);
            }
        }

        LOGGER.info("Statistics delivery service created");
        return service;
    }

    /**
     * Initializes and starts the statistics delivery service.
     *
     * @param service the service to initialize
     */
    public static void initialize(final @Nullable StatisticsDeliveryService service) {
        if (service != null) {
            service.initialize();
            LOGGER.info("Statistics delivery service initialized and started");
        }
    }

    /**
     * Shuts down the statistics delivery service.
     *
     * @param service the service to shutdown
     */
    public static void shutdown(final @Nullable StatisticsDeliveryService service) {
        if (service != null) {
            service.shutdown();
            LOGGER.info("Statistics delivery service shut down");
        }
    }
}
