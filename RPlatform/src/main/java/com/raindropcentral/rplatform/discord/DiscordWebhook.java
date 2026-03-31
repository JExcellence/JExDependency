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

/**
 * Provides a lightweight client for dispatching Discord webhook messages from Bukkit-based plugins.
 *
 * <p>This helper supports simple text and embed payloads, performing the HTTP work asynchronously on a
 * cached thread pool. Failures are logged using the plugin logger so callers can monitor delivery
 * health without blocking the main server thread.
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public class DiscordWebhook {

    /**
     * Dedicated executor used to offload webhook HTTP calls so they do not block the server thread.
     */
    private static final Executor EXECUTOR = Executors.newCachedThreadPool();
    /**
     * JSON content type header applied to every webhook request sent by this client.
     */
    private static final String CONTENT_TYPE = "application/json";
    /**
     * Custom user-agent that identifies webhook calls originating from the RPlatform integration.
     */
    private static final String USER_AGENT = "RPlatform-Discord-Webhook/2.0";
    /**
     * Connection and read timeout used for webhook requests, expressed in milliseconds.
     */
    private static final int TIMEOUT_MS = 5000;

    private final Logger logger;
    private final String webhookUrl;

    /**
     * Creates a new webhook client bound to the supplied plugin logger and Discord endpoint URL.
     *
     * @param plugin     plugin providing the logger used for error reporting
     * @param webhookUrl full Discord webhook URL that receives payloads
     */
    public DiscordWebhook(final @NotNull JavaPlugin plugin, final @NotNull String webhookUrl) {
        this.logger = plugin.getLogger();
        this.webhookUrl = webhookUrl;
    }

    /**
     * Sends a plain text message to the configured Discord webhook asynchronously.
 *
 * <p>The payload is encoded as JSON using the {@code content} field and executed on the shared
     * executor so Bukkit's main thread remains responsive. Any I/O failures are caught, logged at the
     * warning level, and result in a {@code false} outcome within the returned future.
     *
     * @param content message body to deliver, mapped to the Discord {@code content} field
     * @return future that completes with {@code true} when Discord responds with a 2xx status
     */
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

    /**
     * Sends a single-field embed to the Discord webhook asynchronously using the shared executor.
 *
 * <p>The generated JSON payload includes the title, description, and color fields expected by the
     * Discord embeds API. When an exception occurs during transmission the error is logged and the
     * resulting future resolves to {@code false}.
     *
     * @param title       embed title text
     * @param description embed body text
     * @param color       decimal color value applied to the embed sidebar
     * @return future that completes with {@code true} for successful HTTP 2xx responses
     */
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

    /**
     * Fluent builder that enforces providing both the plugin context and webhook URL before.
     * instantiating a {@link DiscordWebhook}.
     */
    public static final class Builder {

        private JavaPlugin plugin;
        private String webhookUrl;

        /**
         * Applies the plugin reference whose logger should be used for webhook error reporting.
         *
         * @param plugin plugin providing the logger backing the webhook client
         * @return this builder instance for fluent chaining
         */
        public @NotNull Builder plugin(final @NotNull JavaPlugin plugin) {
            this.plugin = plugin;
            return this;
        }

        /**
         * Supplies the Discord webhook URL that the client should post to.
         *
         * @param webhookUrl full Discord webhook URL
         * @return this builder instance for fluent chaining
         */
        public @NotNull Builder webhookUrl(final @NotNull String webhookUrl) {
            this.webhookUrl = webhookUrl;
            return this;
        }

        /**
         * Constructs a {@link DiscordWebhook} when all required values are present.
         *
         * @return webhook client instance, or {@code null} when validation fails
         */
        public @Nullable DiscordWebhook build() {
            if (plugin == null || webhookUrl == null || webhookUrl.isBlank()) {
                return null;
            }
            return new DiscordWebhook(plugin, webhookUrl);
        }
    }
}
