package de.jexcellence.jexplatform.proxy;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Result payload returned by proxy action handlers.
 *
 * @param success    whether the action succeeded
 * @param statusCode a stable machine-readable status code
 * @param message    an operator-facing message
 * @param payload    optional response payload
 * @author JExcellence
 * @since 1.0.0
 */
public record ProxyActionResult(
        boolean success,
        @NotNull String statusCode,
        @NotNull String message,
        @NotNull Map<String, String> payload
) {

    /** Compact constructor with normalization. */
    public ProxyActionResult {
        statusCode = normalizeRequired(statusCode, "statusCode");
        message = Objects.requireNonNull(message, "message cannot be null").trim();
        payload = normalizePayload(payload);
    }

    /**
     * Creates a success result with no payload.
     *
     * @param message the operator-facing message
     * @return a success result
     */
    public static @NotNull ProxyActionResult success(@NotNull String message) {
        return new ProxyActionResult(true, "ok", message, Map.of());
    }

    /**
     * Creates a success result with payload.
     *
     * @param message the operator-facing message
     * @param payload the response payload
     * @return a success result
     */
    public static @NotNull ProxyActionResult success(@NotNull String message,
                                                     @NotNull Map<String, String> payload) {
        return new ProxyActionResult(true, "ok", message, payload);
    }

    /**
     * Creates a failure result with no payload.
     *
     * @param statusCode the stable machine-readable status code
     * @param message    the operator-facing message
     * @return a failure result
     */
    public static @NotNull ProxyActionResult failure(@NotNull String statusCode,
                                                     @NotNull String message) {
        return new ProxyActionResult(false, statusCode, message, Map.of());
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
