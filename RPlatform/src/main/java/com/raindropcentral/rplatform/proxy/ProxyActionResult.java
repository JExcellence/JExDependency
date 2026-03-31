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

package com.raindropcentral.rplatform.proxy;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Result payload returned by proxy action handlers.
 *
 * @param success whether the action succeeded
 * @param statusCode stable machine-readable status code
 * @param message operator-facing message
 * @param payload optional response payload
 */
public record ProxyActionResult(
    boolean success,
    @NotNull String statusCode,
    @NotNull String message,
    @NotNull Map<String, String> payload
) {

    /**
     * Creates a normalized action result.
     *
     * @param success whether the action succeeded
     * @param statusCode stable machine-readable status code
     * @param message operator-facing message
     * @param payload optional response payload
     */
    public ProxyActionResult {
        statusCode = normalizeRequired(statusCode, "statusCode");
        message = Objects.requireNonNull(message, "message cannot be null").trim();
        payload = normalizePayload(payload);
    }

    /**
     * Creates a success result with no payload.
     *
     * @param message operator-facing message
     * @return success result
     */
    public static @NotNull ProxyActionResult success(final @NotNull String message) {
        return new ProxyActionResult(true, "ok", message, Map.of());
    }

    /**
     * Creates a success result with payload.
     *
     * @param message operator-facing message
     * @param payload optional response payload
     * @return success result
     */
    public static @NotNull ProxyActionResult success(
        final @NotNull String message,
        final @NotNull Map<String, String> payload
    ) {
        return new ProxyActionResult(true, "ok", message, payload);
    }

    /**
     * Creates a failure result with no payload.
     *
     * @param statusCode stable machine-readable status code
     * @param message operator-facing message
     * @return failure result
     */
    public static @NotNull ProxyActionResult failure(
        final @NotNull String statusCode,
        final @NotNull String message
    ) {
        return new ProxyActionResult(false, statusCode, message, Map.of());
    }

    private static @NotNull String normalizeRequired(final @NotNull String value, final @NotNull String fieldName) {
        final String normalized = Objects.requireNonNull(value, fieldName + " cannot be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return normalized;
    }

    private static @NotNull Map<String, String> normalizePayload(final @NotNull Map<String, String> payload) {
        final Map<String, String> normalized = new LinkedHashMap<>();
        for (final Map.Entry<String, String> entry : Objects.requireNonNull(payload, "payload cannot be null").entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            final String key = entry.getKey().trim();
            final String value = entry.getValue().trim();
            if (!key.isEmpty()) {
                normalized.put(key, value);
            }
        }
        return Map.copyOf(normalized);
    }
}
