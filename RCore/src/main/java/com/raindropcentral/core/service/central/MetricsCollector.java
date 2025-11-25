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

    public record ServerMetrics(
        int currentPlayers,
        int maxPlayers,
        double tps,
        @NotNull String serverVersion,
        @NotNull MemoryInfo memory,
        String playerList
    ) {}

    public record MemoryInfo(
        long usedMb,
        long maxMb
    ) {
        public int usagePercent() {
            return maxMb > 0 ? (int) ((usedMb * 100) / maxMb) : 0;
        }
    }
}
