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
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.raindropcentral.core.service.statistics.delivery.BatchPayload;
import com.raindropcentral.core.service.statistics.delivery.DeliveryReceipt;
import com.raindropcentral.core.service.statistics.delivery.StatisticEntry;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
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

    public RCentralApiClient(final @NotNull Plugin plugin, final @NotNull String baseUrl) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL) // Follow 301, 302, 303, 307, 308 redirects
                .build();
        // Configure Gson to handle ISO timestamp strings as longs
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
                .create();
    }

    /**
     * Performs connectServer.
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
     * Performs sendHeartbeat.
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
     * Performs disconnectServer.
     */
    public CompletableFuture<ApiResponse> disconnectServer(final @NotNull String apiKey) {
        return sendRequest("/api/server-data/disconnect", "POST", apiKey, null);
    }

    /**
     * Performs shutdownServer.
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
     * Performs wakeupServer.
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
                    return gson.fromJson(response.body(),
                            new TypeToken<List<StatisticEntry>>(){}.getType());
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
                    /**
                     * Represents the type API type.
                     */
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

    /**
     * Represents the ApiResponse API type.
     */
    public record ApiResponse(int statusCode, @Nullable String body, @Nullable String error) {
        /**
         * Returns whether success.
         */
        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }
}
