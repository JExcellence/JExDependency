package de.jexcellence.jexplatform.proxy;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Pending arrival token issued before cross-server player transfer.
 *
 * @param tokenId             the stable token identifier
 * @param playerUuid          the player UUID targeted by this token
 * @param moduleId            the owning module identifier
 * @param actionId            the destination action identifier
 * @param destination         the destination network location
 * @param payload             optional payload values for the destination handler
 * @param issuedAtEpochMilli  the issue timestamp in epoch milliseconds
 * @param expiresAtEpochMilli the expiry timestamp in epoch milliseconds
 * @author JExcellence
 * @since 1.0.0
 */
public record PendingArrivalToken(
        @NotNull String tokenId,
        @NotNull UUID playerUuid,
        @NotNull String moduleId,
        @NotNull String actionId,
        @NotNull NetworkLocation destination,
        @NotNull Map<String, String> payload,
        long issuedAtEpochMilli,
        long expiresAtEpochMilli
) {

    /** Compact constructor with normalization. */
    public PendingArrivalToken {
        tokenId = normalizeRequired(tokenId, "tokenId");
        playerUuid = Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        moduleId = normalizeRequired(moduleId, "moduleId");
        actionId = normalizeRequired(actionId, "actionId");
        destination = Objects.requireNonNull(destination, "destination cannot be null");
        payload = normalizePayload(payload);
        issuedAtEpochMilli = Math.max(0L, issuedAtEpochMilli);
        expiresAtEpochMilli = Math.max(issuedAtEpochMilli, expiresAtEpochMilli);
    }

    /**
     * Returns whether this token has expired at the supplied timestamp.
     *
     * @param nowEpochMilli the comparison timestamp in epoch milliseconds
     * @return {@code true} when expired
     */
    public boolean isExpired(long nowEpochMilli) {
        return nowEpochMilli >= expiresAtEpochMilli;
    }

    /**
     * Returns whether this token matches a module action for a player and destination.
     *
     * @param targetPlayerUuid    the player UUID to match
     * @param targetModuleId      the module identifier to match
     * @param targetActionId      the action identifier to match
     * @param destinationServerId the destination route identifier to match
     * @return {@code true} when all fields match
     */
    public boolean matches(@NotNull UUID targetPlayerUuid, @NotNull String targetModuleId,
                           @NotNull String targetActionId,
                           @NotNull String destinationServerId) {
        return playerUuid.equals(targetPlayerUuid)
                && moduleId.equalsIgnoreCase(targetModuleId)
                && actionId.equalsIgnoreCase(targetActionId)
                && destination.serverId().equalsIgnoreCase(destinationServerId);
    }

    private static @NotNull String normalizeRequired(@NotNull String value,
                                                     @NotNull String fieldName) {
        var normalized = Objects.requireNonNull(value, fieldName + " cannot be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return normalized;
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
