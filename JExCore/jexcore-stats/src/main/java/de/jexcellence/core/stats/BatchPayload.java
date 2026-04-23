package de.jexcellence.core.stats;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A batch of {@link StatisticEntry} records ready for HTTP delivery.
 *
 * @param batchId unique id so the backend can de-duplicate retries
 * @param serverUuid originating server UUID
 * @param createdAt batch assembly timestamp
 * @param entries batched statistic entries (never empty)
 * @param body compressed JSON body, ready for transport
 * @param signature hex-encoded HMAC-SHA256 of {@code body}, or {@code null}
 */
public record BatchPayload(
        @NotNull UUID batchId,
        @NotNull UUID serverUuid,
        @NotNull Instant createdAt,
        @NotNull List<StatisticEntry> entries,
        byte @NotNull [] body,
        @Nullable String signature
) {
    public int size() {
        return this.entries.size();
    }

    public int bytes() {
        return this.body.length;
    }
}
