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
 * Pending arrival token issued before cross-server player transfer.
 *
 * @param tokenId stable token identifier
 * @param playerUuid player UUID targeted by this token
 * @param moduleId owning module identifier
 * @param actionId destination action identifier
 * @param destination destination network location
 * @param payload optional payload values for the destination handler
 * @param issuedAtEpochMilli issue timestamp in epoch milliseconds
 * @param expiresAtEpochMilli expiry timestamp in epoch milliseconds
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

    /**
     * Creates a normalized pending arrival token.
     *
     * @param tokenId stable token identifier
     * @param playerUuid player UUID targeted by this token
     * @param moduleId owning module identifier
     * @param actionId destination action identifier
     * @param destination destination network location
     * @param payload optional payload values for the destination handler
     * @param issuedAtEpochMilli issue timestamp in epoch milliseconds
     * @param expiresAtEpochMilli expiry timestamp in epoch milliseconds
     */
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
     * @param nowEpochMilli comparison timestamp in epoch milliseconds
     * @return {@code true} when expired
     */
    public boolean isExpired(final long nowEpochMilli) {
        return nowEpochMilli >= this.expiresAtEpochMilli;
    }

    /**
     * Returns whether this token matches one module action for one player and destination server.
     *
     * @param targetPlayerUuid player UUID to match
     * @param targetModuleId module identifier to match
     * @param targetActionId action identifier to match
     * @param destinationServerId destination route identifier to match
     * @return {@code true} when all fields match
     */
    public boolean matches(
        final @NotNull UUID targetPlayerUuid,
        final @NotNull String targetModuleId,
        final @NotNull String targetActionId,
        final @NotNull String destinationServerId
    ) {
        return this.playerUuid.equals(targetPlayerUuid)
            && this.moduleId.equalsIgnoreCase(targetModuleId)
            && this.actionId.equalsIgnoreCase(targetActionId)
            && this.destination.serverId().equalsIgnoreCase(destinationServerId);
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
