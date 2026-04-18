package de.jexcellence.jexplatform.proxy;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Versioned proxy action message exchanged between Paper modules and proxy handlers.
 *
 * @param requestId           the correlation identifier for this action request
 * @param protocolVersion     the payload protocol version
 * @param moduleId            the owning module identifier
 * @param actionId            the module action identifier
 * @param playerUuid          the player UUID associated with this action
 * @param sourceServerId      the source server route identifier
 * @param targetServerId      the target server route identifier
 * @param actionToken         the optional pending-arrival token identifier
 * @param payload             the action payload values
 * @param createdAtEpochMilli the creation timestamp in epoch milliseconds
 * @author JExcellence
 * @since 1.0.0
 */
public record ProxyActionEnvelope(
        @NotNull UUID requestId,
        int protocolVersion,
        @NotNull String moduleId,
        @NotNull String actionId,
        @NotNull UUID playerUuid,
        @NotNull String sourceServerId,
        @NotNull String targetServerId,
        @NotNull String actionToken,
        @NotNull Map<String, String> payload,
        long createdAtEpochMilli
) {

    /** Compact constructor with normalization and validation. */
    public ProxyActionEnvelope {
        requestId = Objects.requireNonNull(requestId, "requestId cannot be null");
        protocolVersion = Math.max(1, protocolVersion);
        moduleId = normalizeRequired(moduleId, "moduleId");
        actionId = normalizeRequired(actionId, "actionId");
        playerUuid = Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        sourceServerId = normalizeRequired(sourceServerId, "sourceServerId");
        targetServerId = normalizeRequired(targetServerId, "targetServerId");
        actionToken = normalizeOptional(actionToken);
        payload = normalizePayload(payload);
        createdAtEpochMilli = Math.max(0L, createdAtEpochMilli);
    }

    /**
     * Creates a proxy action envelope with empty token and payload.
     *
     * @param requestId           the correlation identifier
     * @param protocolVersion     the protocol version
     * @param moduleId            the module identifier
     * @param actionId            the action identifier
     * @param playerUuid          the player UUID
     * @param sourceServerId      the source server identifier
     * @param targetServerId      the target server identifier
     * @param createdAtEpochMilli the creation timestamp
     */
    public ProxyActionEnvelope(
            @NotNull UUID requestId, int protocolVersion,
            @NotNull String moduleId, @NotNull String actionId,
            @NotNull UUID playerUuid,
            @NotNull String sourceServerId, @NotNull String targetServerId,
            long createdAtEpochMilli) {
        this(requestId, protocolVersion, moduleId, actionId, playerUuid,
                sourceServerId, targetServerId, "", Map.of(), createdAtEpochMilli);
    }

    private static @NotNull String normalizeRequired(@NotNull String value,
                                                     @NotNull String fieldName) {
        var normalized = Objects.requireNonNull(value, fieldName + " cannot be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return normalized;
    }

    private static @NotNull String normalizeOptional(@NotNull String value) {
        return Objects.requireNonNull(value, "actionToken cannot be null").trim();
    }

    private static @NotNull Map<String, String> normalizePayload(
            @NotNull Map<String, String> payload) {
        var normalized = new LinkedHashMap<String, String>();
        for (var entry : Objects.requireNonNull(payload, "payload cannot be null").entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            var key = entry.getKey().trim();
            var value = entry.getValue().trim();
            if (!key.isEmpty()) {
                normalized.put(key, value);
            }
        }
        return Map.copyOf(normalized);
    }
}
