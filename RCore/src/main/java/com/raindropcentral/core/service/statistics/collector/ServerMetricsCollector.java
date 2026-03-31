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

package com.raindropcentral.core.service.statistics.collector;

import com.raindropcentral.core.service.statistics.delivery.PluginMetrics;
import com.raindropcentral.core.service.statistics.delivery.ServerMetrics;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Collects server-level and plugin-specific metrics.
 * Provides TPS, memory, CPU, and custom metric collection.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class ServerMetricsCollector {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RCore");

    private final Plugin plugin;
    private final Map<String, MetricProvider> customMetricProviders;
    private final long serverStartTime;

    // TPS tracking
    private final double[] tpsHistory = new double[60]; // 1 minute of samples
    private int tpsIndex = 0;
    private long lastTpsCheck = System.currentTimeMillis();
    private int lastTickCount = 0;

    // Plugin metrics tracking (reset each collection)
    private int completedQuestsInPeriod = 0;
    private int economyTransactionCount = 0;
    private double economyTransactionVolume = 0.0;
    private int perkActivationCount = 0;
    private int completedBountiesInPeriod = 0;

    /**
     * Executes ServerMetricsCollector.
     */
    public ServerMetricsCollector(final @NotNull Plugin plugin) {
        this.plugin = plugin;
        this.customMetricProviders = new ConcurrentHashMap<>();
        this.serverStartTime = System.currentTimeMillis();

        // Initialize TPS history
        for (int i = 0; i < tpsHistory.length; i++) {
            tpsHistory[i] = 20.0;
        }
    }

    /**
     * Collects current server metrics.
     *
     * @return the server metrics
     */
    public ServerMetrics collectServerMetrics() {
        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        // Memory
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();

        // CPU
        double cpuUsage = osBean.getSystemLoadAverage();
        if (cpuUsage < 0) {
            cpuUsage = 0.0; // Not available on some systems
        }

        // TPS
        double[] tps = calculateTps();

        // World stats
        int worldCount = Bukkit.getWorlds().size();
        int loadedChunks = 0;
        int entityCount = 0;
        int tileEntityCount = 0;

        for (World world : Bukkit.getWorlds()) {
            loadedChunks += world.getLoadedChunks().length;
            entityCount += world.getEntities().size();
            // Tile entities counted via chunks
            for (var chunk : world.getLoadedChunks()) {
                tileEntityCount += chunk.getTileEntities().length;
            }
        }

        return ServerMetrics.builder()
            .tps1m(tps[0])
            .tps5m(tps[1])
            .tps15m(tps[2])
            .heapUsed(heapUsed)
            .heapMax(heapMax)
            .nonHeapUsed(nonHeapUsed)
            .cpuUsage(cpuUsage)
            .onlinePlayers(Bukkit.getOnlinePlayers().size())
            .maxPlayers(Bukkit.getMaxPlayers())
            .uptimeMs(System.currentTimeMillis() - serverStartTime)
            .worldCount(worldCount)
            .loadedChunks(loadedChunks)
            .entityCount(entityCount)
            .tileEntityCount(tileEntityCount)
            .build();
    }

    /**
     * Collects plugin-specific metrics.
     *
     * @return the plugin metrics
     */
    public PluginMetrics collectPluginMetrics() {
        PluginMetrics metrics = PluginMetrics.builder()
            .activeQuestCount(getActiveQuestCount())
            .completedQuestsInPeriod(completedQuestsInPeriod)
            .economyTransactionCount(economyTransactionCount)
            .economyTransactionVolume(economyTransactionVolume)
            .perkActivationCount(perkActivationCount)
            .activePerkCount(getActivePerkCount())
            .activeBountyCount(getActiveBountyCount())
            .completedBountiesInPeriod(completedBountiesInPeriod)
            .build();

        // Reset period counters
        resetPeriodCounters();

        return metrics;
    }

    /**
     * Collects custom metrics from registered providers.
     *
     * @return map of metric names to values
     */
    public Map<String, Object> collectCustomMetrics() {
        Map<String, Object> result = new ConcurrentHashMap<>();

        for (var entry : customMetricProviders.entrySet()) {
            try {
                Object value = entry.getValue().collect();
                if (value != null) {
                    result.put(entry.getKey(), value);
                }
            } catch (Exception e) {
                LOGGER.warning("Failed to collect custom metric '" + entry.getKey() + "': " + e.getMessage());
            }
        }

        return result;
    }

    /**
     * Registers a custom metric provider.
     *
     * @param name     the metric name
     * @param provider the provider function
     */
    public void registerMetricProvider(final @NotNull String name, final @NotNull MetricProvider provider) {
        customMetricProviders.put(name, provider);
        LOGGER.fine("Registered custom metric provider: " + name);
    }

    /**
     * Unregisters a custom metric provider.
     *
     * @param name the metric name
     */
    public void unregisterMetricProvider(final @NotNull String name) {
        customMetricProviders.remove(name);
    }

    // ==================== Event Recording ====================

    /**
     * Records a quest completion for metrics.
     */
    public void recordQuestCompletion() {
        completedQuestsInPeriod++;
    }

    /**
     * Records an economy transaction for metrics.
     *
     * @param amount the transaction amount
     */
    public void recordEconomyTransaction(final double amount) {
        economyTransactionCount++;
        economyTransactionVolume += Math.abs(amount);
    }

    /**
     * Records a perk activation for metrics.
     */
    public void recordPerkActivation() {
        perkActivationCount++;
    }

    /**
     * Records a bounty completion for metrics.
     */
    public void recordBountyCompletion() {
        completedBountiesInPeriod++;
    }

    // ==================== TPS Calculation ====================

    /**
     * Updates TPS tracking. Should be called every tick.
     */
    public void recordTick() {
        lastTickCount++;
    }

    /**
     * Calculates TPS averages.
     *
     * @return array of [1m, 5m, 15m] TPS averages
     */
    private double[] calculateTps() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastTpsCheck;

        if (elapsed >= 1000) {
            double tps = (lastTickCount * 1000.0) / elapsed;
            tps = Math.min(20.0, tps); // Cap at 20

            tpsHistory[tpsIndex] = tps;
            tpsIndex = (tpsIndex + 1) % tpsHistory.length;

            lastTickCount = 0;
            lastTpsCheck = now;
        }

        // Calculate averages
        double sum1m = 0, sum5m = 0, sum15m = 0;
        int count1m = Math.min(60, tpsHistory.length);
        int count5m = Math.min(300, tpsHistory.length);
        int count15m = tpsHistory.length;

        for (int i = 0; i < tpsHistory.length; i++) {
            double val = tpsHistory[(tpsIndex - 1 - i + tpsHistory.length) % tpsHistory.length];
            if (i < count1m) sum1m += val;
            if (i < count5m) sum5m += val;
            sum15m += val;
        }

        return new double[]{
            sum1m / count1m,
            sum5m / Math.min(count5m, tpsHistory.length),
            sum15m / count15m
        };
    }

    // ==================== Plugin Integration Stubs ====================

    private int getActiveQuestCount() {
        // TODO: Integrate with RDQ quest system
        return 0;
    }

    private int getActivePerkCount() {
        // TODO: Integrate with perk system
        return 0;
    }

    private int getActiveBountyCount() {
        // TODO: Integrate with RDQ bounty system
        return 0;
    }

    private void resetPeriodCounters() {
        completedQuestsInPeriod = 0;
        economyTransactionCount = 0;
        economyTransactionVolume = 0.0;
        perkActivationCount = 0;
        completedBountiesInPeriod = 0;
    }

    /**
     * Functional interface for custom metric providers.
     */
    @FunctionalInterface
    public interface MetricProvider {
        /**
         * Collects the metric value.
         *
         * @return the metric value, or null if unavailable
         */
        @Nullable Object collect();
    }
}
