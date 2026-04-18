package de.jexcellence.jexplatform.proxy;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Player transfer request routed through the proxy bridge.
 *
 * @param playerUuid     the player UUID to transfer
 * @param sourceServerId the origin server route identifier
 * @param targetServerId the destination server route identifier
 * @param actionToken    the optional pending-arrival token identifier
 * @param metadata       optional metadata associated with this transfer
 * @author JExcellence
 * @since 1.0.0
 */
public record ProxyTransferRequest(
        @NotNull UUID playerUuid,
        @NotNull String sourceServerId,
        @NotNull String targetServerId,
        @NotNull String actionToken,
        @NotNull Map<String, String> metadata
) {

    /** Compact constructor with normalization. */
    public ProxyTransferRequest {
        playerUuid = Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        sourceServerId = normalizeRequired(sourceServerId, "sourceServerId");
        targetServerId = normalizeRequired(targetServerId, "targetServerId");
        actionToken = normalizeOptional(actionToken);
        metadata = normalizeMetadata(metadata);
    }

    /**
     * Creates a transfer request with empty metadata.
     *
     * @param playerUuid     the player UUID to transfer
     * @param sourceServerId the origin server route identifier
     * @param targetServerId the destination server route identifier
     * @param actionToken    the optional pending-arrival token identifier
     */
    public ProxyTransferRequest(@NotNull UUID playerUuid, @NotNull String sourceServerId,
                                @NotNull String targetServerId, @NotNull String actionToken) {
        this(playerUuid, sourceServerId, targetServerId, actionToken, Map.of());
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

    private static @NotNull Map<String, String> normalizeMetadata(
            @NotNull Map<String, String> metadata) {
        var normalized = new LinkedHashMap<String, String>();
        for (var entry : Objects.requireNonNull(metadata, "metadata cannot be null").entrySet()) {
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
