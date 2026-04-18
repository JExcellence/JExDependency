package de.jexcellence.jexplatform.integration.discord;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Async Discord webhook client using {@link HttpClient}.
 *
 * <p>Built via the fluent {@link Builder}:
 * <pre>{@code
 * var webhook = DiscordWebhook.builder("https://discord.com/api/webhooks/...")
 *         .username("Bot")
 *         .avatarUrl("https://example.com/avatar.png")
 *         .build();
 * webhook.sendMessage("Hello!").join();
 * }</pre>
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class DiscordWebhook {

    private final URI webhookUrl;
    private final String username;
    private final String avatarUrl;
    private final HttpClient httpClient;

    private DiscordWebhook(@NotNull URI webhookUrl, @Nullable String username,
                           @Nullable String avatarUrl, @NotNull HttpClient httpClient) {
        this.webhookUrl = webhookUrl;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.httpClient = httpClient;
    }

    /**
     * Creates a new builder for a Discord webhook.
     *
     * @param webhookUrl the webhook URL
     * @return a new builder
     */
    public static @NotNull Builder builder(@NotNull String webhookUrl) {
        return new Builder(webhookUrl);
    }

    /**
     * Sends a plain text message to the webhook.
     *
     * @param content the message content
     * @return a future completing when the message is sent
     */
    public @NotNull CompletableFuture<Void> sendMessage(@NotNull String content) {
        var json = buildJson(content, null);
        return post(json);
    }

    /**
     * Sends an embed to the webhook.
     *
     * @param title       the embed title
     * @param description the embed description
     * @param color       the embed color (decimal)
     * @return a future completing when the embed is sent
     */
    public @NotNull CompletableFuture<Void> sendEmbed(@NotNull String title,
                                                      @NotNull String description,
                                                      int color) {
        var embedJson = "{\"title\":" + escape(title)
                + ",\"description\":" + escape(description)
                + ",\"color\":" + color + "}";
        var json = buildJson(null, embedJson);
        return post(json);
    }

    private @NotNull String buildJson(@Nullable String content, @Nullable String embedJson) {
        var sb = new StringBuilder("{");
        if (username != null) {
            sb.append("\"username\":").append(escape(username)).append(',');
        }
        if (avatarUrl != null) {
            sb.append("\"avatar_url\":").append(escape(avatarUrl)).append(',');
        }
        if (content != null) {
            sb.append("\"content\":").append(escape(content)).append(',');
        }
        if (embedJson != null) {
            sb.append("\"embeds\":[").append(embedJson).append("],");
        }
        // Remove trailing comma
        if (sb.charAt(sb.length() - 1) == ',') {
            sb.setLength(sb.length() - 1);
        }
        sb.append('}');
        return sb.toString();
    }

    private @NotNull CompletableFuture<Void> post(@NotNull String json) {
        var request = HttpRequest.newBuilder(webhookUrl)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .timeout(Duration.ofSeconds(10))
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApply(r -> null);
    }

    private static @NotNull String escape(@NotNull String value) {
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

    /**
     * Builder for {@link DiscordWebhook}.
     *
     * @author JExcellence
     * @since 1.0.0
     */
    public static final class Builder {

        private final String webhookUrl;
        private String username;
        private String avatarUrl;
        private HttpClient httpClient;

        private Builder(@NotNull String webhookUrl) {
            this.webhookUrl = Objects.requireNonNull(webhookUrl, "webhookUrl cannot be null");
        }

        /**
         * Sets the webhook display username.
         *
         * @param username the username
         * @return this builder
         */
        public @NotNull Builder username(@Nullable String username) {
            this.username = username;
            return this;
        }

        /**
         * Sets the webhook avatar URL.
         *
         * @param avatarUrl the avatar URL
         * @return this builder
         */
        public @NotNull Builder avatarUrl(@Nullable String avatarUrl) {
            this.avatarUrl = avatarUrl;
            return this;
        }

        /**
         * Sets a custom HTTP client.
         *
         * @param httpClient the HTTP client
         * @return this builder
         */
        public @NotNull Builder httpClient(@NotNull HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Builds the webhook.
         *
         * @return a new {@link DiscordWebhook}
         */
        public @NotNull DiscordWebhook build() {
            var client = httpClient != null ? httpClient
                    : HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            return new DiscordWebhook(URI.create(webhookUrl), username, avatarUrl, client);
        }
    }
}
