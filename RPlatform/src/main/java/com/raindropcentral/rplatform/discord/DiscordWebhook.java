package com.raindropcentral.rplatform.discord;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DiscordWebhook {

    private static final Executor EXECUTOR = Executors.newCachedThreadPool();
    private static final String CONTENT_TYPE = "application/json";
    private static final String USER_AGENT = "RPlatform-Discord-Webhook/2.0";
    private static final int TIMEOUT_MS = 5000;

    private final Logger logger;
    private final String webhookUrl;

    public DiscordWebhook(final @NotNull JavaPlugin plugin, final @NotNull String webhookUrl) {
        this.logger = plugin.getLogger();
        this.webhookUrl = webhookUrl;
    }

    public @NotNull CompletableFuture<Boolean> sendMessage(final @NotNull String content) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final String payload = buildJsonPayload(content);
                return sendPayload(payload);
            } catch (final Exception e) {
                logger.log(Level.WARNING, "Failed to send Discord message", e);
                return false;
            }
        }, EXECUTOR);
    }

    public @NotNull CompletableFuture<Boolean> sendEmbed(
            final @NotNull String title,
            final @NotNull String description,
            final int color
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final String payload = buildEmbedPayload(title, description, color);
                return sendPayload(payload);
            } catch (final Exception e) {
                logger.log(Level.WARNING, "Failed to send Discord embed", e);
                return false;
            }
        }, EXECUTOR);
    }

    private boolean sendPayload(final @NotNull String payload) throws Exception {
        final HttpURLConnection connection = (HttpURLConnection) URI.create(webhookUrl)
                .toURL()
                .openConnection();

        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", CONTENT_TYPE);
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setDoOutput(true);

            try (final OutputStream output = connection.getOutputStream()) {
                output.write(payload.getBytes(StandardCharsets.UTF_8));
                output.flush();
            }

            final int responseCode = connection.getResponseCode();
            return responseCode >= 200 && responseCode < 300;
        } finally {
            connection.disconnect();
        }
    }

    private @NotNull String buildJsonPayload(final @NotNull String content) {
        return String.format("{\"content\":\"%s\"}", escapeJson(content));
    }

    private @NotNull String buildEmbedPayload(
            final @NotNull String title,
            final @NotNull String description,
            final int color
    ) {
        return String.format(
                "{\"embeds\":[{\"title\":\"%s\",\"description\":\"%s\",\"color\":%d}]}",
                escapeJson(title),
                escapeJson(description),
                color
        );
    }

    private @NotNull String escapeJson(final @NotNull String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static final class Builder {

        private JavaPlugin plugin;
        private String webhookUrl;

        public @NotNull Builder plugin(final @NotNull JavaPlugin plugin) {
            this.plugin = plugin;
            return this;
        }

        public @NotNull Builder webhookUrl(final @NotNull String webhookUrl) {
            this.webhookUrl = webhookUrl;
            return this;
        }

        public @Nullable DiscordWebhook build() {
            if (plugin == null || webhookUrl == null || webhookUrl.isBlank()) {
                return null;
            }
            return new DiscordWebhook(plugin, webhookUrl);
        }
    }
}
