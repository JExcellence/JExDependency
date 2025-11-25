package com.raindropcentral.core.service.central;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
                .build();
        this.gson = new Gson();
    }

    public CompletableFuture<ApiResponse> connectServer(
            final @NotNull String apiKey,
            final @NotNull String serverUuid,
            final @NotNull String serverVersion,
            final @NotNull String pluginVersion,
            final @NotNull String playerUuid,
            final @NotNull String playerName
    ) {
        var payload = new JsonObject();
        payload.addProperty("serverUuid", serverUuid);
        payload.addProperty("serverVersion", serverVersion);
        payload.addProperty("pluginVersion", pluginVersion);
        payload.addProperty("minecraftUuid", playerUuid);
        payload.addProperty("minecraftUsername", playerName);

        return sendRequest("/api/server-data/connect", "POST", apiKey, payload);
    }

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

    public CompletableFuture<ApiResponse> disconnectServer(final @NotNull String apiKey) {
        return sendRequest("/api/server-data/disconnect", "POST", apiKey, null);
    }

    public CompletableFuture<ApiResponse> shutdownServer(
            final @NotNull String apiKey,
            final @NotNull String serverUuid
    ) {
        var payload = new JsonObject();
        payload.addProperty("serverUuid", serverUuid);
        payload.addProperty("reason", "Server stopping");

        return sendRequest("/api/server-data/shutdown", "POST", apiKey, payload);
    }

    public CompletableFuture<ApiResponse> wakeupServer(
            final @NotNull String apiKey,
            final @NotNull String serverUuid,
            final @NotNull String serverVersion,
            final @NotNull String pluginVersion
    ) {
        var payload = new JsonObject();
        payload.addProperty("serverUuid", serverUuid);
        payload.addProperty("serverVersion", serverVersion);
        payload.addProperty("pluginVersion", pluginVersion);

        return sendRequest("/api/server-data/wakeup", "POST", apiKey, payload);
    }

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
                        .header("Authorization", "Bearer " + apiKey)
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
                logger.log(Level.WARNING, "API request failed: " + endpoint, e);
                return new ApiResponse(0, null, e.getMessage());
            }
        });
    }

    public record ApiResponse(int statusCode, @Nullable String body, @Nullable String error) {
        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }
}
