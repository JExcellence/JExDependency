package com.raindropcentral.rplatform.metrics;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Test helper that captures interactions with the reflective metrics entry point so unit tests can
 * assert how {@link MetricsManager} collaborates with the relocated bStats implementation without
 * touching the real networking stack.
 */
final class MetricsTestRegistry {

    private static JavaPlugin lastPlugin;
    private static Integer lastServiceId;
    private static boolean lastFoliaFlag;
    private static boolean pluginEnabledAtConstruction;
    private static int initializationCount;
    private static int attemptCount;
    private static BStatsMetricsStub lastBStatsMetrics;
    private static boolean failWhenPluginDisabled;
    private static boolean failOnInvalidServiceId;

    private MetricsTestRegistry() {
    }

    static void reset() {
        lastPlugin = null;
        lastServiceId = null;
        lastFoliaFlag = false;
        pluginEnabledAtConstruction = false;
        initializationCount = 0;
        attemptCount = 0;
        lastBStatsMetrics = null;
        failWhenPluginDisabled = false;
        failOnInvalidServiceId = false;
    }

    static void setFailWhenPluginDisabled(final boolean value) {
        failWhenPluginDisabled = value;
    }

    static void setFailOnInvalidServiceId(final boolean value) {
        failOnInvalidServiceId = value;
    }

    static void recordConstruction(
            final @NotNull JavaPlugin plugin,
            final int serviceId,
            final boolean folia
    ) {
        attemptCount++;
        pluginEnabledAtConstruction = plugin.isEnabled();
        lastPlugin = plugin;
        lastServiceId = serviceId;
        lastFoliaFlag = folia;

        if (failWhenPluginDisabled && !pluginEnabledAtConstruction) {
            lastBStatsMetrics = null;
            throw new IllegalStateException("Plugin disabled");
        }

        if (failOnInvalidServiceId && serviceId <= 0) {
            lastBStatsMetrics = null;
            throw new IllegalArgumentException("Service ID must be positive");
        }

        lastBStatsMetrics = new BStatsMetricsStub(plugin, serviceId, folia);
        initializationCount++;
    }

    static @Nullable JavaPlugin getLastPlugin() {
        return lastPlugin;
    }

    static @Nullable Integer getLastServiceId() {
        return lastServiceId;
    }

    static boolean isLastFoliaFlag() {
        return lastFoliaFlag;
    }

    static boolean wasPluginEnabledAtConstruction() {
        return pluginEnabledAtConstruction;
    }

    static int getInitializationCount() {
        return initializationCount;
    }

    static int getAttemptCount() {
        return attemptCount;
    }

    static @Nullable BStatsMetricsStub getLastBStatsMetrics() {
        return lastBStatsMetrics;
    }

    /**
     * Lightweight stand-in for the relocated {@code BStatsMetrics} implementation used so tests can
     * assert how metrics are provisioned without invoking the production code.
     */
    static final class BStatsMetricsStub {

        private final JavaPlugin plugin;
        private final int serviceId;
        private final boolean folia;
        private boolean shutdown;

        BStatsMetricsStub(final @NotNull JavaPlugin plugin, final int serviceId, final boolean folia) {
            this.plugin = plugin;
            this.serviceId = serviceId;
            this.folia = folia;
            this.shutdown = false;
        }

        void shutdown() {
            shutdown = true;
        }

        @NotNull JavaPlugin getPlugin() {
            return plugin;
        }

        int getServiceId() {
            return serviceId;
        }

        boolean isFolia() {
            return folia;
        }

        boolean isShutdown() {
            return shutdown;
        }
    }
}
