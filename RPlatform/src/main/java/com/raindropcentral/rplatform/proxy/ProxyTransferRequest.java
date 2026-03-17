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
import java.util.UUID;

/**
 * Player transfer request routed through the proxy bridge.
 *
 * @param playerUuid player UUID to transfer
 * @param sourceServerId origin server route identifier
 * @param targetServerId destination server route identifier
 * @param actionToken optional pending-arrival token identifier
 * @param metadata optional metadata associated with this transfer
 */
public record ProxyTransferRequest(
    @NotNull UUID playerUuid,
    @NotNull String sourceServerId,
    @NotNull String targetServerId,
    @NotNull String actionToken,
    @NotNull Map<String, String> metadata
) {

    /**
     * Creates a normalized transfer request.
     *
     * @param playerUuid player UUID to transfer
     * @param sourceServerId origin server route identifier
     * @param targetServerId destination server route identifier
     * @param actionToken optional pending-arrival token identifier
     * @param metadata optional metadata associated with this transfer
     */
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
     * @param playerUuid player UUID to transfer
     * @param sourceServerId origin server route identifier
     * @param targetServerId destination server route identifier
     * @param actionToken optional pending-arrival token identifier
     */
    public ProxyTransferRequest(
        final @NotNull UUID playerUuid,
        final @NotNull String sourceServerId,
        final @NotNull String targetServerId,
        final @NotNull String actionToken
    ) {
        this(playerUuid, sourceServerId, targetServerId, actionToken, Map.of());
    }

    private static @NotNull String normalizeRequired(final @NotNull String value, final @NotNull String fieldName) {
        final String normalized = Objects.requireNonNull(value, fieldName + " cannot be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return normalized;
    }

    private static @NotNull String normalizeOptional(final @NotNull String value) {
        return Objects.requireNonNull(value, "actionToken cannot be null").trim();
    }

    private static @NotNull Map<String, String> normalizeMetadata(final @NotNull Map<String, String> metadata) {
        final Map<String, String> normalized = new LinkedHashMap<>();
        for (final Map.Entry<String, String> entry : Objects.requireNonNull(metadata, "metadata cannot be null").entrySet()) {
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
