package com.raindropcentral.core.service.central;

import com.raindropcentral.core.database.entity.central.RCentralServer;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Main service for managing RaindropCentral platform integration.
 */
public class RCentralService {

    private static final Logger LOGGER = CentralLogger.getLogger(RCentralService.class);

    private final Plugin plugin;
    private final FileConfiguration config;
    private final RCentralApiClient apiClient;
    private RCentralServer serverEntity;
    private UUID serverUuid;
    private HeartbeatScheduler heartbeatScheduler;

    public RCentralService(final @NotNull Plugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();

        var backendUrl = detectBackendUrl();
        this.apiClient = new RCentralApiClient(plugin, backendUrl);

        var savedUuid = config.getString("connection.server-uuid");
        if (savedUuid != null) {
            this.serverUuid = UUID.fromString(savedUuid);
        } else {
            this.serverUuid = UUID.randomUUID();
            config.set("connection.server-uuid", serverUuid.toString());
            plugin.saveConfig();
        }

        LOGGER.info("RCentralService initialized with server UUID: " + serverUuid);
        LOGGER.info("Backend URL: " + backendUrl);

        // Send wakeup ping if server was previously connected
        sendWakeupPingIfNeeded();
    }

    private String detectBackendUrl() {
        var configUrl = config.getString("backend.url");
        var isDevelopmentMode = config.getBoolean("backend.development-mode", false);
        var serverPort = Bukkit.getServer().getPort();

        if (isDevelopmentMode || serverPort != 25565) {
            LOGGER.info("Development mode detected - using localhost:3000");
            return "http://localhost:3000";
        }

        return configUrl != null ? configUrl : "https://raindropcentral.com";
    }

    /**
     * Connects to RaindropCentral with player UUID verification.
     */
    public CompletableFuture<ConnectionResult> connect(
            final @NotNull String apiKey,
            final @NotNull String playerUuid,
            final @NotNull String playerName
    ) {
        var serverVersion = Bukkit.getVersion();
        var pluginVersion = plugin.getDescription().getVersion();

        return apiClient.connectServer(apiKey, serverUuid.toString(), serverVersion, pluginVersion, playerUuid, playerName)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        config.set("connection.api-key", apiKey);
                        plugin.saveConfig();

                        if (serverEntity == null) {
                            serverEntity = new RCentralServer(serverUuid);
                        }
                        serverEntity.setApiKeyHash(hashApiKey(apiKey));
                        serverEntity.setConnectionStatus(RCentralServer.ConnectionStatus.CONNECTED);
                        serverEntity.setServerVersion(serverVersion);
                        serverEntity.setPluginVersion(pluginVersion);

                        LOGGER.info("Successfully connected to RaindropCentral");
                        startHeartbeat(apiKey);

                        return new ConnectionResult(true, null, null);
                    } else {
                        var errorCode = parseErrorCode(response);
                        var errorMsg = parseErrorMessage(response);
                        LOGGER.warning("Connection failed: " + errorMsg);
                        return new ConnectionResult(false, errorMsg, errorCode);
                    }
                });
    }

    public CompletableFuture<Boolean> disconnect() {
        var apiKey = config.getString("connection.api-key");
        if (apiKey == null) {
            return CompletableFuture.completedFuture(false);
        }

        return apiClient.disconnectServer(apiKey)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        config.set("connection.api-key", null);
                        plugin.saveConfig();

                        if (serverEntity != null) {
                            serverEntity.setConnectionStatus(RCentralServer.ConnectionStatus.DISCONNECTED);
                        }

                        LOGGER.info("Disconnected from RaindropCentral");
                        stopHeartbeat();
                        return true;
                    }
                    return false;
                });
    }

    public boolean isConnected() {
        return serverEntity != null && serverEntity.isConnected();
    }

    public UUID getServerUuid() {
        return serverUuid;
    }

    /**
     * Sends a shutdown notification to the backend when the server is stopping.
     * This allows the backend to immediately mark the server as offline instead of
     * waiting for heartbeat timeout.
     */
    public CompletableFuture<Boolean> notifyShutdown() {
        var apiKey = config.getString("connection.api-key");
        if (apiKey == null || !isConnected()) {
            LOGGER.fine("Not sending shutdown notification - server not connected");
            return CompletableFuture.completedFuture(false);
        }

        LOGGER.info("Sending shutdown notification to RaindropCentral");

        return apiClient.shutdownServer(apiKey, serverUuid.toString())
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        LOGGER.info("Shutdown notification sent successfully");
                        if (serverEntity != null) {
                            serverEntity.setConnectionStatus(RCentralServer.ConnectionStatus.DISCONNECTED);
                        }
                        stopHeartbeat();
                        return true;
                    } else {
                        LOGGER.warning("Failed to send shutdown notification: " + response.statusCode());
                        return false;
                    }
                })
                .exceptionally(throwable -> {
                    LOGGER.warning("Error sending shutdown notification: " + throwable.getMessage());
                    return false;
                });
    }

    /**
     * Sends a wakeup ping to the backend when the server starts up.
     * This is only sent if the server was previously connected (has an API key saved).
     * Allows the backend to immediately mark the server as online instead of waiting
     * for the first heartbeat.
     */
    private void sendWakeupPingIfNeeded() {
        var apiKey = config.getString("connection.api-key");
        if (apiKey == null) {
            LOGGER.fine("No saved API key - skipping wakeup ping");
            return;
        }

        LOGGER.info("Sending wakeup ping to RaindropCentral");

        var serverVersion = Bukkit.getVersion();
        var pluginVersion = plugin.getDescription().getVersion();

        apiClient.wakeupServer(apiKey, serverUuid.toString(), serverVersion, pluginVersion)
                .thenAccept(response -> {
                    if (response.isSuccess()) {
                        LOGGER.info("Wakeup ping sent successfully - server marked as online");
                        
                        // Initialize server entity and start heartbeat
                        if (serverEntity == null) {
                            serverEntity = new RCentralServer(serverUuid);
                        }
                        serverEntity.setConnectionStatus(RCentralServer.ConnectionStatus.CONNECTED);
                        serverEntity.setServerVersion(serverVersion);
                        serverEntity.setPluginVersion(pluginVersion);
                        
                        startHeartbeat(apiKey);
                    } else {
                        LOGGER.warning("Wakeup ping failed: " + response.statusCode() + 
                                " - Server will need manual reconnection via /rcconnect");
                        // Clear saved API key if it's invalid
                        if (response.statusCode() == 401 || response.statusCode() == 403) {
                            config.set("connection.api-key", null);
                            plugin.saveConfig();
                        }
                    }
                })
                .exceptionally(throwable -> {
                    LOGGER.warning("Error sending wakeup ping: " + throwable.getMessage());
                    return null;
                });
    }

    private void startHeartbeat(final String apiKey) {
        if (heartbeatScheduler != null && heartbeatScheduler.isRunning()) {
            LOGGER.warning("Heartbeat scheduler is already running");
            return;
        }

        var sharePlayerList = config.getBoolean("privacy.share-player-list", true);
        heartbeatScheduler = new HeartbeatScheduler(plugin, apiClient, apiKey, sharePlayerList);
        heartbeatScheduler.start();
    }

    private void stopHeartbeat() {
        if (heartbeatScheduler != null) {
            heartbeatScheduler.stop();
            heartbeatScheduler = null;
        }
    }

    private String hashApiKey(final String apiKey) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hashBytes = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.severe("SHA-256 algorithm not available: " + e.getMessage());
            return apiKey;
        }
    }

    private String parseErrorCode(final RCentralApiClient.ApiResponse response) {
        return switch (response.statusCode()) {
            case 401 -> "INVALID_KEY";
            case 403 -> "UUID_MISMATCH";
            default -> "UNKNOWN";
        };
    }

    private String parseErrorMessage(final RCentralApiClient.ApiResponse response) {
        if (response.error() != null) {
            return response.error();
        }

        return switch (response.statusCode()) {
            case 401 -> "Invalid API key";
            case 403 -> "This API key belongs to a different Minecraft account";
            case 404 -> "Server not found";
            case 429 -> "Too many requests, please try again later";
            case 500 -> "Backend server error";
            default -> "Unknown error (status: " + response.statusCode() + ")";
        };
    }

    public record ConnectionResult(boolean success, String errorMessage, String errorCode) {}
}
