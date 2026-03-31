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

package com.raindropcentral.core.service.central;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

/**
 * Collects server metrics for sending to RaindropCentral platform.
 */
public class MetricsCollector {

    private static final int TARGET_TPS = 20;
    private long lastTickTime = System.currentTimeMillis();
    private double currentTps = 20.0;

    /**
     * Collects current server metrics.
     */
    public ServerMetrics collect(final boolean includePlayerList) {
        return new ServerMetrics(
            getCurrentPlayers(),
            getMaxPlayers(),
            calculateTps(),
            getServerVersion(),
            getMemoryUsage(),
            includePlayerList ? getPlayerList() : null
        );
    }

    private int getCurrentPlayers() {
        return Bukkit.getOnlinePlayers().size();
    }

    private int getMaxPlayers() {
        return Bukkit.getMaxPlayers();
    }

    private double calculateTps() {
        try {
            var server = Bukkit.getServer();
            var tpsMethod = server.getClass().getMethod("getTPS"); //paper has it own method for that
            var tpsArray = (double[]) tpsMethod.invoke(server);
            return Math.min(tpsArray[0], 20.0);
        } catch (Exception e) {
            var currentTime = System.currentTimeMillis();
            var timeDiff = currentTime - lastTickTime;
            lastTickTime = currentTime;
            
            if (timeDiff > 0) {
                currentTps = Math.min(1000.0 / timeDiff * TARGET_TPS, 20.0);
            }
            return currentTps;
        }
    }

    private String getServerVersion() {
        return Bukkit.getVersion();
    }

    private MemoryInfo getMemoryUsage() {
        var runtime = Runtime.getRuntime();
        var maxMemory = runtime.maxMemory() / 1024 / 1024;
        var totalMemory = runtime.totalMemory() / 1024 / 1024;
        var freeMemory = runtime.freeMemory() / 1024 / 1024;
        var usedMemory = totalMemory - freeMemory;
        
        return new MemoryInfo(usedMemory, maxMemory);
    }

    private String getPlayerList() {
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .collect(Collectors.joining(","));
    }

    /**
     * Snapshot of server metrics collected for API transmission.
     *
     * @param currentPlayers current online player count
     * @param maxPlayers configured maximum player count
     * @param tps measured ticks per second
     * @param serverVersion server software version string
     * @param memory memory usage snapshot
     * @param playerList comma-separated player names when included
     */
    public record ServerMetrics(
        int currentPlayers,
        int maxPlayers,
        double tps,
        @NotNull String serverVersion,
        @NotNull MemoryInfo memory,
        String playerList
    ) {}

    /**
     * Memory usage snapshot in megabytes.
     *
     * @param usedMb used heap memory in megabytes
     * @param maxMb maximum heap memory in megabytes
     */
    public record MemoryInfo(
        long usedMb,
        long maxMb
    ) {
        /**
         * Returns used heap percentage.
         *
         * @return heap usage percent, or {@code 0} when max memory is unavailable
         */
        public int usagePercent() {
            return maxMb > 0 ? (int) ((usedMb * 100) / maxMb) : 0;
        }
    }
}
