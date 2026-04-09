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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.raindropcentral.core.service.statistics.delivery.BatchPayload;
import com.raindropcentral.core.service.statistics.delivery.DeliveryReceipt;
import com.raindropcentral.core.service.statistics.delivery.StatisticEntry;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HTTP client for communicating with the RaindropCentral backend API.
 */
public class RCentralApiClient {

    private final Plugin plugin;
    private final Logger logger;
    private final HttpClient httpClient;
    private final Gson gson;
    private final String baseUrl;

    /**
     * Creates an HTTP client bound to the supplied backend base URL.
     *
     * @param plugin plugin providing logging context
     * @param baseUrl RaindropCentral backend base URL without a trailing slash requirement
     */
    public RCentralApiClient(final @NotNull Plugin plugin, final @NotNull String baseUrl) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL) // Follow 301, 302, 303, 307, 308 redirects
                .build();
        // Configure Gson to handle ISO timestamp strings as longs and ensure proper serialization
        this.gson = new GsonBuilder()
                .registerTypeAdapter(long.class, (JsonDeserializer<Long>) (json, type, context) -> {
                    if (json.isJsonPrimitive()) {
                        var primitive = json.getAsJsonPrimitive();
                        if (primitive.isNumber()) {
                            return primitive.getAsLong();
                        } else if (primitive.isString()) {
                            // Try to parse ISO timestamp string
                            String str = primitive.getAsString();
                            try {
                                return Instant.parse(str).toEpochMilli();
                            } catch (Exception e) {
                                // Try parsing as plain number string
                                return Long.parseLong(str);
                            }
                        }
                    }
                    return json.getAsLong();
                })
                .registerTypeAdapter(Long.class, (JsonDeserializer<Long>) (json, type, context) -> {
                    if (json.isJsonPrimitive()) {
                        var primitive = json.getAsJsonPrimitive();
                        if (primitive.isNumber()) {
                            return primitive.getAsLong();
                        } else if (primitive.isString()) {
                            // Try to parse ISO timestamp string
                            String str = primitive.getAsString();
                            try {
                                return Instant.parse(str).toEpochMilli();
                            } catch (Exception e) {
                                // Try parsing as plain number string
                                return Long.parseLong(str);
                            }
                        }
                    }
                    return json.getAsLong();
                })
                // Ensure StatisticEntry values are always serialized as strings
                .registerTypeAdapter(StatisticEntry.class, (com.google.gson.JsonSerializer<StatisticEntry>) (src, typeOfSrc, context) -> {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("playerUuid", src.playerUuid().toString());
                    obj.addProperty("statisticKey", src.statisticKey());
                    obj.addProperty("value", src.value()); // Already a String from fromQueued()
                    obj.addProperty("dataType", src.dataType().name());
                    obj.addProperty("collectionTimestamp", src.collectionTimestamp());
                    obj.addProperty("isDelta", src.isDelta());
                    obj.addProperty("sourcePlugin", src.sourcePlugin());
                    return obj;
                })
                .create();
    }

    /**
     * Sends the initial server-connect request to RaindropCentral.
     *
     * @param apiKey active server API key
     * @param serverUuid unique identifier for the server instance
     * @param serverVersion reported Minecraft server version
     * @param pluginVersion RCore plugin version
     * @param playerUuid UUID of the player performing the connect flow
     * @param playerName username of the player performing the connect flow
     * @param maxPlayers current configured maximum player count
     * @return asynchronous transport response
     */
    public CompletableFuture<ApiResponse> connectServer(
            final @NotNull String apiKey,
            final @NotNull String serverUuid,
            final @NotNull String serverVersion,
            final @NotNull String pluginVersion,
            final @NotNull String playerUuid,
            final @NotNull String playerName,
            final int maxPlayers
    ) {
        var payload = new JsonObject();
        payload.addProperty("serverUuid", serverUuid);
        payload.addProperty("serverVersion", serverVersion);
        payload.addProperty("pluginVersion", pluginVersion);
        payload.addProperty("minecraftUuid", playerUuid);
        payload.addProperty("minecraftUsername", playerName);
        payload.addProperty("maxPlayers", maxPlayers);

        return sendRequest("/api/server-data/connect", "POST", apiKey, payload);
    }

    /**
     * Sends a server heartbeat update.
     *
     * @param apiKey active server API key
     * @param currentPlayers current online player count
     * @param maxPlayers current configured maximum player count
     * @param tps reported server TPS snapshot
     * @param playerList optional serialized player list
     * @return asynchronous transport response
     */
    public CompletableFuture<ApiResponse> sendHeartbeat(
            final @NotNull String apiKey,
            final int currentPlayers,
            final int maxPlayers,
            final double tps,
            final @Nullable String playerList
    ) {
        var payload = new JsonObject();
        payload.addProperty("currentPlayers", currentPlayers);
        payload.addProperty("maxPlayers", maxPlayers);
        payload.addProperty("tps", tps);
        if (playerList != null) {
            payload.addProperty("playerList", playerList);
        }

        return sendRequest("/api/server-data/heartbeat", "POST", apiKey, payload);
    }

    /**
     * Sends a disconnect notification for the currently connected server.
     *
     * @param apiKey active server API key
     * @return asynchronous transport response
     */
    public CompletableFuture<ApiResponse> disconnectServer(final @NotNull String apiKey) {
        return sendRequest("/api/server-data/disconnect", "POST", apiKey, null);
    }

    /**
     * Sends a shutdown notification so the backend can mark the server offline.
     *
     * @param apiKey active server API key
     * @param serverUuid unique identifier for the server instance
     * @return asynchronous transport response
     */
    public CompletableFuture<ApiResponse> shutdownServer(
            final @NotNull String apiKey,
            final @NotNull String serverUuid
    ) {
        var payload = new JsonObject();
        payload.addProperty("serverUuid", serverUuid);
        payload.addProperty("reason", "Server stopping");

        return sendRequest("/api/server-data/shutdown", "POST", apiKey, payload);
    }

    /**
     * Sends a wake-up notification used when reconnecting an already registered server.
     *
     * @param apiKey active server API key
     * @param serverUuid unique identifier for the server instance
     * @param serverVersion reported Minecraft server version
     * @param pluginVersion RCore plugin version
     * @param maxPlayers current configured maximum player count
     * @param minecraftUuid optional UUID of the player waking the server
     * @param minecraftUsername optional username of the player waking the server
     * @return asynchronous transport response
     */
    public CompletableFuture<ApiResponse> wakeupServer(
            final @NotNull String apiKey,
            final @NotNull String serverUuid,
            final @NotNull String serverVersion,
            final @NotNull String pluginVersion,
            final int maxPlayers,
            final @Nullable String minecraftUuid,
            final @Nullable String minecraftUsername
    ) {
        var payload = new JsonObject();
        payload.addProperty("serverUuid", serverUuid);
        payload.addProperty("serverVersion", serverVersion);
        payload.addProperty("pluginVersion", pluginVersion);
        payload.addProperty("maxPlayers", maxPlayers);
        if (minecraftUuid != null) {
            payload.addProperty("minecraftUuid", minecraftUuid);
        }
        if (minecraftUsername != null) {
            payload.addProperty("minecraftUsername", minecraftUsername);
        }

        return sendRequest("/api/server-data/wakeup", "POST", apiKey, payload);
    }

    /**
     * Requests the effective droplet-store allowlist for the connected server.
     *
     * @param apiKey active server API key
     * @return asynchronous parsed API response
     */
    public CompletableFuture<ParsedApiResponse<DropletStoreAllowlistData>> getDropletStoreAllowlist(
            final @NotNull String apiKey
    ) {
        return sendTypedRequest(
                "/api/server-data/droplet-store/allowlist",
                "GET",
                apiKey,
                null,
                DropletStoreAllowlistData.class
        );
    }

    /**
     * Requests unclaimed droplet-store purchases for a player.
     *
     * @param apiKey active server API key
     * @param playerUuid player UUID linked to RaindropCentral
     * @return asynchronous parsed API response
     */
    public CompletableFuture<ParsedApiResponse<List<DropletStorePurchaseData>>> getUnclaimedDropletPurchases(
            final @NotNull String apiKey,
            final @NotNull UUID playerUuid
    ) {
        return sendTypedRequest(
                "/api/server-data/droplet-store/players/" + playerUuid + "/purchases/unclaimed",
                "GET",
                apiKey,
                null,
                new TypeToken<List<DropletStorePurchaseData>>() {}.getType()
        );
    }

    /**
     * Marks a droplet-store purchase as claimed for a player.
     *
     * @param apiKey active server API key
     * @param playerUuid player UUID linked to RaindropCentral
     * @param purchaseId purchase identifier to claim
     * @return asynchronous parsed API response
     */
    public CompletableFuture<ParsedApiResponse<DropletStorePurchaseData>> claimDropletPurchase(
            final @NotNull String apiKey,
            final @NotNull UUID playerUuid,
            final long purchaseId
    ) {
        return sendTypedRequest(
                "/api/server-data/droplet-store/players/" + playerUuid + "/purchases/" + purchaseId + "/claim",
                "POST",
                apiKey,
                null,
                DropletStorePurchaseData.class
        );
    }

    // ==================== Statistics Delivery Methods ====================

    /**
     * Delivers statistics to the RaindropCentral backend.
     *
     * @param apiKey  the API key
     * @param payload the batch payload to deliver
     * @return a future containing the delivery receipt
     */
    public CompletableFuture<DeliveryReceipt> deliverStatistics(
            final @NotNull String apiKey,
            final @NotNull BatchPayload payload
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String json = gson.toJson(payload);
                
                // Debug logging to verify serialization
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine("Sending statistics JSON: " + json.substring(0, Math.min(500, json.length())));
                }
                
                var request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/statistics/deliver"))
                        .header("X-API-Key", apiKey)
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return gson.fromJson(response.body(), DeliveryReceipt.class);
                } else {
                    throw new RuntimeException("Delivery failed with status " + response.statusCode() +
                            ": " + response.body());
                }

            } catch (IOException | InterruptedException e) {
                if (e instanceof ConnectException) {
                    logger.log(Level.INFO, "Backend not reachable of " + baseUrl + ". If you think this is an issue contact the administrator of the plugin.");
                    return null;
                }

                logger.log(Level.WARNING, "Statistics delivery failed", e);
                throw new RuntimeException("Statistics delivery failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Delivers compressed statistics to the RaindropCentral backend.
     *
     * @param apiKey            the API key
     * @param compressedPayload the GZIP compressed payload
     * @param batchId           the batch ID
     * @return a future containing the delivery receipt
     */
    public CompletableFuture<DeliveryReceipt> deliverStatisticsCompressed(
            final @NotNull String apiKey,
            final byte @NotNull [] compressedPayload,
            final @NotNull String batchId
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/statistics/deliver"))
                        .header("X-API-Key", apiKey)
                        .header("Content-Type", "application/json")
                        .header("Content-Encoding", "gzip")
                        .header("X-Batch-Id", batchId)
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.ofByteArray(compressedPayload))
                        .build();

                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return gson.fromJson(response.body(), DeliveryReceipt.class);
                } else {
                    throw new RuntimeException("Compressed delivery failed with status " +
                            response.statusCode() + ": " + response.body());
                }

            } catch (IOException | InterruptedException e) {
                logger.log(Level.WARNING, "Compressed statistics delivery failed", e);
                throw new RuntimeException("Compressed statistics delivery failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Requests player statistics from the backend for cross-server sync.
     *
     * @param apiKey     the API key
     * @param playerUuid the player UUID
     * @return a future containing the list of statistic entries
     */
    public CompletableFuture<List<StatisticEntry>> requestPlayerStatistics(
            final @NotNull String apiKey,
            final @NotNull UUID playerUuid
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/statistics/player/" + playerUuid))
                        .header("X-API-Key", apiKey)
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();

                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    // Try to parse as array first (expected format)
                    try {
                        return gson.fromJson(response.body(),
                                new TypeToken<List<StatisticEntry>>(){}.getType());
                    } catch (com.google.gson.JsonSyntaxException e) {
                        // Backend returned an object instead of array - handle gracefully
                        logger.log(Level.FINE, "Backend returned object format instead of array for player " + 
                                playerUuid + ", returning empty list");
                        return List.of();
                    }
                } else if (response.statusCode() == 404) {
                    return List.of(); // No statistics found for player
                } else {
                    throw new RuntimeException("Request failed with status " + response.statusCode() +
                            ": " + response.body());
                }

            } catch (IOException | InterruptedException e) {
                if (e instanceof ConnectException) {
                    logger.log(Level.INFO, "Backend not reachable of " + baseUrl + ". If you think this is an issue contact the administrator of the plugin.");
                    return List.of();
                }

                logger.log(Level.WARNING, "Player statistics request failed for " + playerUuid, e);
                throw new RuntimeException("Player statistics request failed: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Submits vanilla Minecraft statistics to the backend API.
     * <p>
     * This method sends statistics to the {@code /api/v1/statistics/vanilla} endpoint
     * which is specifically designed for vanilla Minecraft statistics collection.
     * </p>
     *
     * @param apiKey       the API key for authentication
     * @param serverUuid   the unique server identifier
     * @param payload      the batch payload containing vanilla statistics
     * @return a future containing the delivery receipt
     */
    public CompletableFuture<DeliveryReceipt> submitVanillaStatistics(
            final @NotNull String apiKey,
            final @NotNull UUID serverUuid,
            final @NotNull BatchPayload payload
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String json = gson.toJson(payload);
                var request = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/v1/statistics/vanilla"))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("X-Server-Id", serverUuid.toString())
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(30))
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    // Parse the response as DeliveryReceipt
                    return gson.fromJson(response.body(), DeliveryReceipt.class);
                } else {
                    logger.warning("Vanilla statistics submission failed with status " + 
                        response.statusCode() + ": " + response.body());
                    throw new RuntimeException("Vanilla statistics submission failed with status " + 
                        response.statusCode() + ": " + response.body());
                }

            } catch (IOException | InterruptedException e) {
                if (e instanceof ConnectException) {
                    logger.log(Level.INFO, "Backend not reachable at " + baseUrl + 
                        ". If you think this is an issue contact the administrator of the plugin.");
                    return null;
                }

                logger.log(Level.WARNING, "Vanilla statistics submission failed", e);
                throw new RuntimeException("Vanilla statistics submission failed: " + e.getMessage(), e);
            }
        });
    }

    // ==================== Internal Methods ====================

    private CompletableFuture<ApiResponse> sendRequest(
            final @NotNull String endpoint,
            final @NotNull String method,
            final @NotNull String apiKey,
            final @Nullable JsonObject payload
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + endpoint))
                        .header("X-API-Key", apiKey)
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(30));

                if ("POST".equals(method) && payload != null) {
                    requestBuilder.POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)));
                } else if ("POST".equals(method)) {
                    requestBuilder.POST(HttpRequest.BodyPublishers.noBody());
                } else {
                    requestBuilder.GET();
                }

                var response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
                return new ApiResponse(response.statusCode(), response.body(), null);

            } catch (IOException | InterruptedException e) {
                if (e instanceof ConnectException) {
                    logger.log(Level.INFO, "Backend not reachable of " + baseUrl + endpoint + ". If you think this is an issue contact the administrator of the plugin.");
                    return new ApiResponse(0, null, null);
                }

                logger.log(Level.WARNING, "API request failed: " + endpoint, e);
                return new ApiResponse(0, null, e.getMessage());
            }
        });
    }

    private <T> CompletableFuture<ParsedApiResponse<T>> sendTypedRequest(
            final @NotNull String endpoint,
            final @NotNull String method,
            final @NotNull String apiKey,
            final @Nullable JsonObject payload,
            final @NotNull Type dataType
    ) {
        return sendRequest(endpoint, method, apiKey, payload)
                .thenApply(response -> this.parseTypedResponse(response, dataType));
    }

    private <T> @NotNull ParsedApiResponse<T> parseTypedResponse(
            final @NotNull ApiResponse response,
            final @NotNull Type dataType
    ) {
        if (response.body() == null || response.body().isBlank()) {
            return new ParsedApiResponse<>(response.statusCode(), null, null, response.error());
        }

        try {
            final JsonObject root = this.gson.fromJson(response.body(), JsonObject.class);
            if (root == null) {
                return new ParsedApiResponse<>(response.statusCode(), null, null, response.error());
            }

            final String message = this.extractMessage(root);
            final JsonElement dataElement = root.get("data");
            final T data = dataElement == null || dataElement.isJsonNull()
                    ? null
                    : this.gson.fromJson(dataElement, dataType);

            return new ParsedApiResponse<>(response.statusCode(), data, message, response.error());
        } catch (RuntimeException exception) {
            this.logger.log(Level.WARNING, "Failed to parse API response body", exception);
            return new ParsedApiResponse<>(
                    response.statusCode(),
                    null,
                    response.body(),
                    response.error() == null ? exception.getMessage() : response.error()
            );
        }
    }

    private @Nullable String extractMessage(final @NotNull JsonObject root) {
        final JsonElement messageElement = root.get("message");
        if (messageElement != null && !messageElement.isJsonNull()) {
            return messageElement.getAsString();
        }

        final JsonElement errorElement = root.get("error");
        if (errorElement != null && !errorElement.isJsonNull()) {
            return errorElement.getAsString();
        }

        return null;
    }

    /**
     * Lightweight transport response returned by non-typed backend endpoints.
     *
     * @param statusCode HTTP status code returned by the backend
     * @param body raw response body, if any
     * @param error local transport error, if one occurred
     */
    public record ApiResponse(int statusCode, @Nullable String body, @Nullable String error) {
        /**
         * Returns whether the backend response was successful.
         *
         * @return {@code true} for HTTP 2xx responses
         */
        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }

    /**
     * Parsed wrapper around the backend API response envelope.
     *
     * @param statusCode HTTP status code returned by the backend
     * @param data parsed response payload
     * @param message response message from the backend envelope
     * @param error local transport or parse error, if present
     * @param <T> parsed data type
     */
    public record ParsedApiResponse<T>(
            int statusCode,
            @Nullable T data,
            @Nullable String message,
            @Nullable String error
    ) {
        /**
         * Returns whether the backend response was successful.
         *
         * @return {@code true} for HTTP 2xx responses
         */
        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }

    /**
     * Lightweight droplet-store purchase payload used by the in-game claim flow.
     *
     * @param id purchase identifier
     * @param dropletStoreItemId numeric store item identifier
     * @param purchaseDate purchase timestamp string from the backend
     * @param amountSpent droplets spent on the purchase
     * @param itemCode store item code
     * @param itemName store item display name
     * @param claimedAt claim timestamp string from the backend
     * @param claimedServerName claim server snapshot
     */
    public record DropletStorePurchaseData(
            long id,
            @Nullable Long dropletStoreItemId,
            @Nullable String purchaseDate,
            int amountSpent,
            @Nullable String itemCode,
            @Nullable String itemName,
            @Nullable String claimedAt,
            @Nullable String claimedServerName
    ) {
        /**
         * Returns the best available item code for UI rendering.
         *
         * @return non-blank item code, or an empty string when unavailable
         */
        public @NotNull String itemCodeOrBlank() {
            return itemCode == null ? "" : itemCode;
        }

        /**
         * Returns the best available item display name for UI rendering.
         *
         * @return item name, or the item code when the name is unavailable
         */
        public @NotNull String itemNameOrCode() {
            if (itemName != null && !itemName.isBlank()) {
                return itemName;
            }
            return itemCodeOrBlank();
        }
    }

    /**
     * Effective droplet-store allowlist payload returned to RCore.
     *
     * @param allowedItemCodes effective item codes currently allowed for this server
     */
    public record DropletStoreAllowlistData(
            @Nullable List<String> allowedItemCodes
    ) {
        /**
         * Returns the allowlist as a non-null immutable list.
         *
         * @return allowlist item codes
         */
        public @NotNull List<String> allowedItemCodesOrEmpty() {
            return allowedItemCodes == null ? List.of() : List.copyOf(allowedItemCodes);
        }
    }
}
