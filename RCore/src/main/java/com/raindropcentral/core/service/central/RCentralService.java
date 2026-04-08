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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.raindropcentral.core.RCoreImpl;
import com.raindropcentral.core.config.RCentralConfig;
import com.raindropcentral.core.database.entity.central.RCentralServer;
import com.raindropcentral.core.database.repository.RCentralServerRepository;
import com.raindropcentral.rplatform.RPlatform;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main service for managing RaindropCentral platform integration.
 */
public class RCentralService {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("RCore");
    private static final Duration DROPLET_STORE_ALLOWLIST_STALE_AFTER = Duration.ofHours(6);
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {}.getType();

    private final RCoreImpl rCore;
    private final Plugin plugin;
    private final RPlatform platform;
    private final FileConfiguration config;
    private final RCentralConfig rcentralConfig;
    private final RCentralApiClient apiClient;
    private final @Nullable RCentralServerRepository serverRepository;
    private final Gson gson;
    private final UUID serverUuid;
    private RCentralServer serverEntity;
    private HeartbeatScheduler heartbeatScheduler;

    /**
     * Executes RCentralService.
     */
    public RCentralService(final @NotNull RCoreImpl rCore) {
        this.rCore = rCore;
        this.plugin = rCore.getPlugin();
        this.platform = rCore.getPlatform();
        this.config = this.plugin.getConfig();
        this.rcentralConfig = new RCentralConfig(this.plugin);
        this.serverRepository = rCore.getRCentralServerRepository();
        this.gson = new Gson();

        var backendUrl = detectBackendUrl();
        this.apiClient = new RCentralApiClient(this.plugin, backendUrl);

        var savedUuid = this.config.getString("connection.server-uuid");
        if (savedUuid != null) {
            this.serverUuid = UUID.fromString(savedUuid);
        } else {
            this.serverUuid = UUID.randomUUID();
            this.config.set("connection.server-uuid", this.serverUuid.toString());
            this.plugin.saveConfig();
        }
        this.serverEntity = this.loadPersistedServerEntity();
        if (this.serverEntity != null) {
            this.serverEntity.setConnectionStatus(RCentralServer.ConnectionStatus.DISCONNECTED);
        }

        LOGGER.info("RCentralService initialized with server UUID: " + this.serverUuid);
        LOGGER.info("Backend URL: " + backendUrl);

        sendWakeupPingIfNeeded();
    }

    private String detectBackendUrl() {
        var configUrl = rcentralConfig.getBackendUrl();
        if (configUrl != null && !configUrl.isEmpty()) {
            LOGGER.info("Using configured backend URL: " + configUrl);
            return configUrl;
        }

        if (rcentralConfig.isDevelopmentMode()) {
            LOGGER.info("Development mode enabled - using localhost:3000");
            return "http://localhost:3000";
        }

        LOGGER.info("Using production backend: https://raindropcentral.com");
        return "https://raindropcentral.com";
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
        var pluginVersion = plugin.getPluginMeta().getVersion();
        var maxPlayers = Bukkit.getMaxPlayers();

        return apiClient.connectServer(
                        apiKey,
                        serverUuid.toString(),
                        serverVersion,
                        pluginVersion,
                        playerUuid,
                        playerName,
                        maxPlayers
                )
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        config.set("connection.api-key", apiKey);
                        // Store player info for wakeup pings after server restart
                        config.set("connection.minecraft-uuid", playerUuid);
                        config.set("connection.minecraft-username", playerName);
                        plugin.saveConfig();

                        final RCentralServer entity = this.getOrCreateServerEntity();
                        entity.setApiKeyHash(hashApiKey(apiKey));
                        entity.setConnectionStatus(RCentralServer.ConnectionStatus.CONNECTED);
                        entity.setServerVersion(serverVersion);
                        entity.setPluginVersion(pluginVersion);
                        entity.setMaxPlayers(maxPlayers);
                        this.persistServerEntityBlocking();

                        LOGGER.info("Successfully connected to RaindropCentral");
                        startHeartbeat(apiKey);
                        this.refreshDropletStoreAllowlist(false)
                                .thenAccept(this::logAllowlistRefreshFailure);

                        return new ConnectionResult(true, null, null);
                    } else {
                        var errorCode = parseErrorCode(response);
                        var errorMsg = parseErrorMessage(response);
                        LOGGER.warning("Connection failed: " + errorMsg);
                        return new ConnectionResult(false, errorMsg, errorCode);
                    }
                /**
                 * Executes method.
                 */
                /**
                 * Executes this member.
                 */
                });
    }

    /**
     * Disconnects from RaindropCentral by using the configured API key.
     *
     * @return asynchronous result indicating whether disconnect succeeded
     */
    public CompletableFuture<Boolean> disconnect() {
        var apiKey = config.getString("connection.api-key");
        if (apiKey == null) {
            return CompletableFuture.completedFuture(false);
        }
        return disconnect(apiKey);
    }

    /**
     * Disconnects from RaindropCentral using a provided API key.
     *
     * @param apiKey API key that must match the stored connection key
     * @return asynchronous result indicating whether disconnect succeeded
     */
    public CompletableFuture<Boolean> disconnect(final @NotNull String apiKey) {
        if (!matchesStoredApiKey(apiKey)) {
            return CompletableFuture.completedFuture(false);
        }

        return apiClient.disconnectServer(apiKey)
                .thenApply(response -> {
                    if (response.isSuccess()) {
                        config.set("connection.api-key", null);
                        plugin.saveConfig();

                        if (serverEntity != null) {
                            serverEntity.setConnectionStatus(RCentralServer.ConnectionStatus.DISCONNECTED);
                            this.persistServerEntityBlocking();
                        }

                        LOGGER.info("Disconnected from RaindropCentral");
                        stopHeartbeat();
                        return true;
                    }
                    return false;
                });
    }

    /**
     * Verifies whether the provided API key matches the currently connected server key.
     *
     * @param apiKey API key provided by the command sender
     * @return {@code true} when the key matches the stored key, otherwise {@code false}
     */
    public boolean matchesStoredApiKey(final @NotNull String apiKey) {
        var configuredApiKey = config.getString("connection.api-key");
        return configuredApiKey != null && configuredApiKey.equals(apiKey);
    }

    /**
     * Returns whether connected.
     */
    public boolean isConnected() {
        return serverEntity != null && serverEntity.isConnected();
    }

    /**
     * Gets serverUuid.
     */
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
                            this.persistServerEntityBlocking();
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
        var pluginVersion = plugin.getPluginMeta().getVersion();
        var maxPlayers = Bukkit.getMaxPlayers();
        
        // Retrieve stored player info from last successful connection
        var minecraftUuid = config.getString("connection.minecraft-uuid");
        var minecraftUsername = config.getString("connection.minecraft-username");

        apiClient.wakeupServer(
                        apiKey,
                        serverUuid.toString(),
                        serverVersion,
                        pluginVersion,
                        maxPlayers,
                        minecraftUuid,
                        minecraftUsername
                )
                .thenAccept(response -> {
                    if (response.isSuccess()) {
                        LOGGER.info("Wakeup ping sent successfully - server marked as online");

                        final RCentralServer entity = this.getOrCreateServerEntity();
                        entity.setConnectionStatus(RCentralServer.ConnectionStatus.CONNECTED);
                        entity.setServerVersion(serverVersion);
                        entity.setPluginVersion(pluginVersion);
                        entity.setMaxPlayers(maxPlayers);
                        this.persistServerEntityBlocking();
                        
                        startHeartbeat(apiKey);
                        this.refreshDropletStoreAllowlist(false)
                                .thenAccept(this::logAllowlistRefreshFailure);
                    } else {
                        LOGGER.warning("Wakeup ping failed: " + response.statusCode() +
                                " - Server will need manual reconnection via /rc connect <api-key>");
                        if (response.statusCode() == 401 || response.statusCode() == 403) {
                            config.set("connection.api-key", null);
                            plugin.saveConfig();
                        }
                        if (this.serverEntity != null) {
                            this.serverEntity.setConnectionStatus(RCentralServer.ConnectionStatus.DISCONNECTED);
                            this.persistServerEntityBlocking();
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
        heartbeatScheduler = new HeartbeatScheduler(
                plugin,
                platform,
                apiClient,
                apiKey,
                sharePlayerList
        );
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
        /**
         * Represents the type API type.
         */
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

    /**
     * Represents the ConnectionResult API type.
     */
    public record ConnectionResult(boolean success, String errorMessage, String errorCode) {}

    // ==================== Getters for Statistics Delivery ====================

    /**
     * Gets the API client for statistics delivery.
     *
     * @return the API client
     */
    public RCentralApiClient getApiClient() {
        return apiClient;
    }

    /**
     * Gets the API key for statistics delivery.
     *
     * @return the API key or null if not connected
     */
    public String getApiKey() {
        return config.getString("connection.api-key");
    }

    /**
     * Checks whether one supported droplet-store reward is currently allowed for this server.
     *
     * <p>When the backend allowlist has not been fetched yet, the method defaults to
     * {@code true} so claimability is ultimately enforced by the backend until the first
     * successful cache refresh completes.</p>
     *
     * @param itemCode backend item code
     * @return {@code true} when the cached allowlist currently permits the item
     */
    public boolean isDropletStoreRewardEnabled(final @NotNull String itemCode) {
        final List<String> allowedItemCodes = this.getCachedAllowedDropletStoreItemCodesOrNull();
        if (allowedItemCodes == null) {
            return true;
        }
        return allowedItemCodes.contains(normalizeItemCode(itemCode));
    }

    /**
     * Fetches unclaimed droplet-store purchases for a player linked to RaindropCentral.
     *
     * @param playerUuid player UUID to look up
     * @return asynchronous parsed API response
     */
    public CompletableFuture<RCentralApiClient.ParsedApiResponse<List<RCentralApiClient.DropletStorePurchaseData>>> getUnclaimedDropletPurchases(
            final @NotNull UUID playerUuid
    ) {
        final String apiKey = this.getApiKey();
        if (apiKey == null || !this.isConnected()) {
            return CompletableFuture.completedFuture(
                    new RCentralApiClient.ParsedApiResponse<>(0, null, "Server is not connected to RaindropCentral.", "Not connected")
            );
        }

        return this.ensureDropletStoreAllowlistFresh()
                .thenCompose(ignored -> this.apiClient.getUnclaimedDropletPurchases(apiKey, playerUuid));
    }

    /**
     * Claims a droplet-store purchase for a linked player.
     *
     * @param playerUuid player UUID to identify the linked account
     * @param purchaseId purchase identifier to claim
     * @return asynchronous parsed API response
     */
    public CompletableFuture<RCentralApiClient.ParsedApiResponse<RCentralApiClient.DropletStorePurchaseData>> claimDropletPurchase(
            final @NotNull UUID playerUuid,
            final long purchaseId
    ) {
        final String apiKey = this.getApiKey();
        if (apiKey == null || !this.isConnected()) {
            return CompletableFuture.completedFuture(
                    new RCentralApiClient.ParsedApiResponse<>(0, null, "Server is not connected to RaindropCentral.", "Not connected")
            );
        }

        return this.ensureDropletStoreAllowlistFresh()
                .thenCompose(ignored -> this.apiClient.claimDropletPurchase(apiKey, playerUuid, purchaseId));
    }

    /**
     * Forces an immediate backend refresh of the local droplet-store allowlist cache.
     *
     * @return refresh outcome describing whether the cache was updated or a stale cache was kept
     */
    public CompletableFuture<AllowlistRefreshResult> forceRefreshDropletStoreAllowlist() {
        return this.refreshDropletStoreAllowlist(true);
    }

    /**
     * Returns the cached droplet-store allowlist, or an empty list when nothing has been cached yet.
     *
     * @return cached allowed item codes
     */
    public @NotNull List<String> getCachedDropletStoreAllowedItemCodes() {
        final List<String> cachedItemCodes = this.getCachedAllowedDropletStoreItemCodesOrNull();
        return cachedItemCodes == null ? List.of() : cachedItemCodes;
    }

    private @NotNull CompletableFuture<Void> ensureDropletStoreAllowlistFresh() {
        if (!this.isDropletStoreAllowlistStale()) {
            return CompletableFuture.completedFuture(null);
        }

        return this.refreshDropletStoreAllowlist(false)
                .thenAccept(this::logAllowlistRefreshFailure);
    }

    private @NotNull CompletableFuture<AllowlistRefreshResult> refreshDropletStoreAllowlist(final boolean force) {
        final String apiKey = this.getApiKey();
        if (apiKey == null || !this.isConnected()) {
            return CompletableFuture.completedFuture(new AllowlistRefreshResult(
                    false,
                    this.hasCachedDropletStoreAllowlist(),
                    this.getCachedDropletStoreAllowedItemCodes(),
                    "Server is not connected to RaindropCentral."
            ));
        }
        if (!force && !this.isDropletStoreAllowlistStale()) {
            return CompletableFuture.completedFuture(new AllowlistRefreshResult(
                    true,
                    true,
                    this.getCachedDropletStoreAllowedItemCodes(),
                    null
            ));
        }

        return this.apiClient.getDropletStoreAllowlist(apiKey)
                .thenApply(response -> {
                    if (!response.isSuccess() || response.data() == null) {
                        return this.toAllowlistFailureResult(this.resolveParsedApiError(response));
                    }

                    final List<String> allowedItemCodes = normalizeAllowlistItemCodes(
                            response.data().allowedItemCodesOrEmpty()
                    );
                    this.cacheDropletStoreAllowlist(allowedItemCodes);
                    return new AllowlistRefreshResult(true, false, allowedItemCodes, null);
                })
                .exceptionally(throwable -> this.toAllowlistFailureResult(this.resolveThrowableMessage(throwable)));
    }

    private void logAllowlistRefreshFailure(final @NotNull AllowlistRefreshResult result) {
        if (!result.success() && result.errorMessage() != null && !result.errorMessage().isBlank()) {
            LOGGER.warning("Failed to refresh droplet-store allowlist: " + result.errorMessage());
        }
    }

    private @NotNull AllowlistRefreshResult toAllowlistFailureResult(final @NotNull String errorMessage) {
        return new AllowlistRefreshResult(
                false,
                this.hasCachedDropletStoreAllowlist(),
                this.getCachedDropletStoreAllowedItemCodes(),
                errorMessage
        );
    }

    private void cacheDropletStoreAllowlist(final @NotNull List<String> allowedItemCodes) {
        final RCentralServer entity = this.getOrCreateServerEntity();
        entity.setDropletStoreAllowedItemCodesJson(this.gson.toJson(normalizeAllowlistItemCodes(allowedItemCodes)));
        entity.setDropletStoreAllowedItemCodesFetchedAt(LocalDateTime.now());
        this.persistServerEntityBlocking();
    }

    private boolean hasCachedDropletStoreAllowlist() {
        return this.serverEntity != null
                && this.serverEntity.getDropletStoreAllowedItemCodesJson() != null
                && !this.serverEntity.getDropletStoreAllowedItemCodesJson().isBlank();
    }

    private boolean isDropletStoreAllowlistStale() {
        return isAllowlistStale(
                this.serverEntity == null ? null : this.serverEntity.getDropletStoreAllowedItemCodesFetchedAt(),
                LocalDateTime.now()
        );
    }

    static boolean isAllowlistStale(
            final @Nullable LocalDateTime fetchedAt,
            final @NotNull LocalDateTime now
    ) {
        if (fetchedAt == null) {
            return true;
        }
        return !fetchedAt.plus(DROPLET_STORE_ALLOWLIST_STALE_AFTER).isAfter(now);
    }

    static @NotNull List<String> normalizeAllowlistItemCodes(final @Nullable List<String> rawItemCodes) {
        if (rawItemCodes == null || rawItemCodes.isEmpty()) {
            return List.of();
        }

        final LinkedHashSet<String> normalizedItemCodes = new LinkedHashSet<>();
        for (final String rawItemCode : rawItemCodes) {
            final String normalizedItemCode = normalizeItemCode(rawItemCode);
            if (!normalizedItemCode.isBlank()) {
                normalizedItemCodes.add(normalizedItemCode);
            }
        }
        return List.copyOf(normalizedItemCodes);
    }

    private @Nullable List<String> getCachedAllowedDropletStoreItemCodesOrNull() {
        if (!this.hasCachedDropletStoreAllowlist()) {
            return null;
        }

        try {
            final List<String> cachedCodes = this.gson.fromJson(
                    this.serverEntity.getDropletStoreAllowedItemCodesJson(),
                    STRING_LIST_TYPE
            );
            return normalizeAllowlistItemCodes(cachedCodes);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Failed to parse cached droplet-store allowlist for " + this.serverUuid, exception);
            return null;
        }
    }

    private @Nullable RCentralServer loadPersistedServerEntity() {
        if (this.serverRepository == null) {
            LOGGER.warning("RCentralServerRepository is unavailable; droplet-store allowlist caching is disabled.");
            return null;
        }

        try {
            return this.serverRepository.findByServerUuid(this.serverUuid).join().orElse(null);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Failed to load persisted RCentral server entity for " + this.serverUuid, exception);
            return null;
        }
    }

    private @NotNull RCentralServer getOrCreateServerEntity() {
        if (this.serverEntity == null) {
            this.serverEntity = new RCentralServer(this.serverUuid);
        }
        return this.serverEntity;
    }

    private void persistServerEntityBlocking() {
        if (this.serverRepository == null || this.serverEntity == null) {
            return;
        }

        try {
            if (this.serverEntity.getId() == null) {
                this.serverEntity = this.serverRepository.createAsync(this.serverEntity).join();
            } else {
                this.serverEntity = this.serverRepository.updateAsync(this.serverEntity).join();
            }
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Failed to persist RCentral server entity for " + this.serverUuid, exception);
        }
    }

    private static @NotNull String normalizeItemCode(final @Nullable String itemCode) {
        return itemCode == null ? "" : itemCode.trim().toLowerCase(Locale.ROOT);
    }

    private @NotNull String resolveParsedApiError(final @NotNull RCentralApiClient.ParsedApiResponse<?> response) {
        if (response.message() != null && !response.message().isBlank()) {
            return response.message();
        }
        if (response.error() != null && !response.error().isBlank()) {
            return response.error();
        }
        return "Status " + response.statusCode();
    }

    private @NotNull String resolveThrowableMessage(final @NotNull Throwable throwable) {
        final String message = throwable.getMessage();
        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
    }

    /**
     * Droplet-store allowlist refresh outcome.
     *
     * @param success whether the backend refresh completed successfully
     * @param usedCachedValue whether the result kept or returned cached values instead of a fresh fetch
     * @param allowedItemCodes best-known effective allowlist after the refresh attempt
     * @param errorMessage failure detail when the refresh did not succeed
     */
    public record AllowlistRefreshResult(
            boolean success,
            boolean usedCachedValue,
            @NotNull List<String> allowedItemCodes,
            @Nullable String errorMessage
    ) {
        public AllowlistRefreshResult {
            allowedItemCodes = List.copyOf(allowedItemCodes);
        }
    }
}
