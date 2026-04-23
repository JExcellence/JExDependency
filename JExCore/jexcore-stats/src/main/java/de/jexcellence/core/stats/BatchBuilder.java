package de.jexcellence.core.stats;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Serialises a list of {@link StatisticEntry} records into a
 * {@link BatchPayload}. JSON-first, gzipped, optionally HMAC-signed.
 */
final class BatchBuilder {

    private final ObjectMapper mapper = new ObjectMapper();
    private final UUID serverUuid;
    private final PayloadSigner signer;

    BatchBuilder(@NotNull UUID serverUuid, @Nullable PayloadSigner signer) {
        this.serverUuid = serverUuid;
        this.signer = signer;
    }

    @NotNull BatchPayload build(@NotNull List<StatisticEntry> entries) {
        if (entries.isEmpty()) throw new IllegalArgumentException("empty batch");

        final UUID batchId = UUID.randomUUID();
        final Instant now = Instant.now();

        final Map<String, Object> root = new LinkedHashMap<>();
        root.put("batchId", batchId.toString());
        root.put("serverUuid", this.serverUuid.toString());
        root.put("createdAt", now.toString());

        final List<Map<String, Object>> serialised = new ArrayList<>(entries.size());
        for (final StatisticEntry e : entries) {
            final Map<String, Object> row = new LinkedHashMap<>();
            row.put("plugin", e.plugin());
            row.put("identifier", e.identifier());
            if (e.playerId() != null) row.put("playerId", e.playerId().toString());
            row.put("value", e.value());
            if (!e.attributes().isEmpty()) row.put("attributes", e.attributes());
            row.put("timestamp", e.timestamp().toString());
            row.put("priority", e.priority().name());
            serialised.add(row);
        }
        root.put("entries", serialised);

        final byte[] raw;
        try {
            raw = this.mapper.writeValueAsBytes(root);
        } catch (final com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("JSON encode failed", ex);
        }
        final byte[] body = PayloadCompressor.gzip(raw);
        final String sig = this.signer != null ? this.signer.sign(body) : null;
        return new BatchPayload(batchId, this.serverUuid, now, List.copyOf(entries), body, sig);
    }
}
