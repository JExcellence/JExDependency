package com.raindropcentral.core.service.central;

import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Schedules and manages periodic heartbeat updates to RaindropCentral platform.
 */
public class HeartbeatScheduler {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RCore");
    private static final int HEARTBEAT_INTERVAL_TICKS = 20 * 60 * 5; // 5 minutes
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    private final Plugin plugin;
    private final RPlatform platform;
    private final RCentralApiClient apiClient;
    private final MetricsCollector metricsCollector;
    private final String apiKey;
    private final boolean sharePlayerList;
    
    private int consecutiveFailures = 0;
    private boolean isRunning = false;

    /**
     * Executes HeartbeatScheduler.
     */
    public HeartbeatScheduler(
        final @NotNull Plugin plugin,
        final @NotNull RPlatform platform,
        final @NotNull RCentralApiClient apiClient,
        final @NotNull String apiKey,
        final boolean sharePlayerList
    ) {
        this.plugin = plugin;
        this.platform = platform;
        this.apiClient = apiClient;
        this.apiKey = apiKey;
        this.sharePlayerList = sharePlayerList;
        this.metricsCollector = new MetricsCollector();
    }

    /**
     * Starts the heartbeat scheduler.
     */
    public void start() {
        if (isRunning) {
            LOGGER.warning("Heartbeat scheduler is already running");
            return;
        }

        LOGGER.info("Starting heartbeat scheduler (interval: 5 minutes)");
        
        platform.getScheduler().runRepeating(
            this::sendHeartbeat,
            HEARTBEAT_INTERVAL_TICKS, // Initial delay
            HEARTBEAT_INTERVAL_TICKS  // Period
        );
        
        isRunning = true;
    }

    /**
     * Stops the heartbeat scheduler.
     */
    public void stop() {
        if (!isRunning) {
            return;
        }

        LOGGER.info("Stopping heartbeat scheduler");
        
        isRunning = false;
        consecutiveFailures = 0;
    }

    /**
     * Sends a heartbeat update to the platform.
     */
    private void sendHeartbeat() {
        try {
            var metrics = metricsCollector.collect(sharePlayerList);
            
            apiClient.sendHeartbeat(
                apiKey,
                metrics.currentPlayers(),
                metrics.maxPlayers(),
                metrics.tps(),
                metrics.playerList()
            ).thenAccept(response -> {
                if (response.isSuccess()) {
                    consecutiveFailures = 0;
                    LOGGER.fine("Heartbeat sent successfully");
                } else {
                    handleHeartbeatFailure("API returned error: " + response.statusCode());
                }
            }).exceptionally(throwable -> {
                handleHeartbeatFailure(throwable.getMessage());
                return null;
            });
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error collecting metrics for heartbeat", e);
        }
    }

    /**
     * Handles heartbeat failures with exponential backoff.
     */
    private void handleHeartbeatFailure(final String reason) {
        consecutiveFailures++;
        
        if (consecutiveFailures <= MAX_RETRY_ATTEMPTS) {
            LOGGER.warning("Heartbeat failed (attempt " + consecutiveFailures + "/" + MAX_RETRY_ATTEMPTS + "): " + reason);
        }
        
        if (consecutiveFailures >= MAX_RETRY_ATTEMPTS) {
            if (consecutiveFailures == MAX_RETRY_ATTEMPTS) {
                LOGGER.severe("Max heartbeat failures reached. Stopping scheduler.");
                LOGGER.severe("Last error: " + reason);
                
                // Notify online operators
                platform.getScheduler().runSync(() -> {
                    Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission("rcore.central.admin"))
                        .forEach(p -> p.sendMessage(
                            "§c[RCore] Connection to RaindropCentral lost after " + MAX_RETRY_ATTEMPTS + " attempts."
                        ));
                });
            }
            
            // Don't increment further to avoid spam
            consecutiveFailures = MAX_RETRY_ATTEMPTS;
            
            // Note: We don't call stop() here because that would prevent future heartbeats
            // The scheduler continues running but logs are suppressed after max attempts
        }
    }

    /**
     * Returns whether running.
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Gets consecutiveFailures.
     */
    public int getConsecutiveFailures() {
        return consecutiveFailures;
    }
}
