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
 * Versioned proxy action message exchanged between Paper modules and proxy handlers.
 *
 * @param requestId correlation identifier for this action request
 * @param protocolVersion payload protocol version
 * @param moduleId owning module identifier
 * @param actionId module action identifier
 * @param playerUuid player UUID associated with this action
 * @param sourceServerId source server route identifier
 * @param targetServerId target server route identifier
 * @param actionToken optional pending-arrival token identifier
 * @param payload action payload values
 * @param createdAtEpochMilli creation timestamp in epoch milliseconds
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

    /**
     * Creates a normalized proxy action envelope.
     *
     * @param requestId correlation identifier for this action request
     * @param protocolVersion payload protocol version
     * @param moduleId owning module identifier
     * @param actionId module action identifier
     * @param playerUuid player UUID associated with this action
     * @param sourceServerId source server route identifier
     * @param targetServerId target server route identifier
     * @param actionToken optional pending-arrival token identifier
     * @param payload action payload values
     * @param createdAtEpochMilli creation timestamp in epoch milliseconds
     */
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
     * Creates a proxy action envelope with empty token and payload values.
     *
     * @param requestId correlation identifier for this action request
     * @param protocolVersion payload protocol version
     * @param moduleId owning module identifier
     * @param actionId module action identifier
     * @param playerUuid player UUID associated with this action
     * @param sourceServerId source server route identifier
     * @param targetServerId target server route identifier
     * @param createdAtEpochMilli creation timestamp in epoch milliseconds
     */
    public ProxyActionEnvelope(
        final @NotNull UUID requestId,
        final int protocolVersion,
        final @NotNull String moduleId,
        final @NotNull String actionId,
        final @NotNull UUID playerUuid,
        final @NotNull String sourceServerId,
        final @NotNull String targetServerId,
        final long createdAtEpochMilli
    ) {
        this(
            requestId,
            protocolVersion,
            moduleId,
            actionId,
            playerUuid,
            sourceServerId,
            targetServerId,
            "",
            Map.of(),
            createdAtEpochMilli
        );
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
