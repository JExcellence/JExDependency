package de.jexcellence.core.stats;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * JDK {@link HttpClient}-backed transport. POSTs the gzip body with
 * standard headers; forwards the signature as {@code X-JExCore-Signature}
 * and the api key as a bearer token.
 */
public final class HttpCentralTransport implements CentralTransport {

    private final HttpClient client;
    private final URI endpoint;
    private final String apiKey;
    private final Duration requestTimeout;

    public HttpCentralTransport(@NotNull StatisticsConfig config) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.endpoint = config.endpoint();
        this.apiKey = config.apiKey();
        this.requestTimeout = config.requestTimeout();
    }

    @Override
    public @NotNull DeliveryResult send(@NotNull BatchPayload batch) {
        final HttpRequest.Builder req = HttpRequest.newBuilder(this.endpoint)
                .timeout(this.requestTimeout)
                .header("Content-Type", "application/json")
                .header("Content-Encoding", "gzip")
                .header("X-JExCore-Batch-Id", batch.batchId().toString())
                .header("X-JExCore-Server", batch.serverUuid().toString())
                .POST(HttpRequest.BodyPublishers.ofByteArray(batch.body()));

        if (this.apiKey != null) req.header("Authorization", "Bearer " + this.apiKey);
        if (batch.signature() != null) req.header("X-JExCore-Signature", batch.signature());

        try {
            final HttpResponse<Void> resp = this.client.send(req.build(), HttpResponse.BodyHandlers.discarding());
            final int code = resp.statusCode();
            if (code >= 200 && code < 300) return DeliveryResult.success(code);
            if (code == 401 || code == 403 || code == 400 || code == 422) {
                return DeliveryResult.fatal(code, "non-retryable HTTP status");
            }
            return DeliveryResult.retry(code, "HTTP " + code);
        } catch (final IOException ex) {
            return DeliveryResult.retry(-1, "io: " + ex.getClass().getSimpleName());
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
            return DeliveryResult.retry(-1, "interrupted");
        }
    }
}
