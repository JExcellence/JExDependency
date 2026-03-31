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

package com.raindropcentral.core.proxy;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration snapshot for RCore proxy coordination.
 *
 * @param channelName proxy plugin-message channel name
 * @param protocolVersion proxy payload protocol version
 * @param requestTimeoutMillis request timeout in milliseconds
 * @param tokenTtlMillis pending-arrival token TTL in milliseconds
 * @param serverRouteId local server route identifier
 * @param routeAliases optional route alias mapping
 */
public record ProxyHostConfig(
    @NotNull String channelName,
    int protocolVersion,
    long requestTimeoutMillis,
    long tokenTtlMillis,
    @NotNull String serverRouteId,
    @NotNull Map<String, String> routeAliases
) {

    private static final String DEFAULT_CHANNEL_NAME = "raindrop:proxy";
    private static final int DEFAULT_PROTOCOL_VERSION = 1;
    private static final long DEFAULT_REQUEST_TIMEOUT_MILLIS = 5_000L;
    private static final long DEFAULT_TOKEN_TTL_MILLIS = 120_000L;

    /**
     * Creates a normalized proxy host config.
     *
     * @param channelName proxy plugin-message channel name
     * @param protocolVersion proxy payload protocol version
     * @param requestTimeoutMillis request timeout in milliseconds
     * @param tokenTtlMillis pending-arrival token TTL in milliseconds
     * @param serverRouteId local server route identifier
     * @param routeAliases optional route alias mapping
     */
    public ProxyHostConfig {
        channelName = normalizeRequired(channelName, "channelName", DEFAULT_CHANNEL_NAME);
        protocolVersion = Math.max(DEFAULT_PROTOCOL_VERSION, protocolVersion);
        requestTimeoutMillis = requestTimeoutMillis <= 0L ? DEFAULT_REQUEST_TIMEOUT_MILLIS : requestTimeoutMillis;
        tokenTtlMillis = tokenTtlMillis <= 0L ? DEFAULT_TOKEN_TTL_MILLIS : tokenTtlMillis;
        serverRouteId = normalizeRequired(serverRouteId, "serverRouteId", "server");
        routeAliases = normalizeAliases(routeAliases);
    }

    /**
     * Creates a default proxy host config for one route identifier.
     *
     * @param serverRouteId local server route identifier
     * @return default proxy host config
     */
    public static @NotNull ProxyHostConfig defaults(final @NotNull String serverRouteId) {
        return new ProxyHostConfig(
            DEFAULT_CHANNEL_NAME,
            DEFAULT_PROTOCOL_VERSION,
            DEFAULT_REQUEST_TIMEOUT_MILLIS,
            DEFAULT_TOKEN_TTL_MILLIS,
            serverRouteId,
            Map.of()
        );
    }

    /**
     * Resolves a canonical route identifier from a route or alias value.
     *
     * @param routeOrAlias route identifier or alias value
     * @return canonical route identifier
     */
    public @NotNull String resolveRoute(final @NotNull String routeOrAlias) {
        final String normalized = routeOrAlias == null ? "" : routeOrAlias.trim();
        if (normalized.isEmpty()) {
            return this.serverRouteId;
        }
        return this.routeAliases.getOrDefault(normalized.toLowerCase(java.util.Locale.ROOT), normalized);
    }

    private static @NotNull String normalizeRequired(
        final @NotNull String value,
        final @NotNull String fieldName,
        final @NotNull String fallback
    ) {
        final String normalized = Objects.requireNonNull(value, fieldName + " cannot be null").trim();
        return normalized.isEmpty() ? fallback : normalized;
    }

    private static @NotNull Map<String, String> normalizeAliases(final @NotNull Map<String, String> routeAliases) {
        final Map<String, String> normalized = new LinkedHashMap<>();
        for (final Map.Entry<String, String> entry : Objects.requireNonNull(routeAliases, "routeAliases cannot be null").entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            final String alias = entry.getKey().trim().toLowerCase(java.util.Locale.ROOT);
            final String route = entry.getValue().trim();
            if (alias.isEmpty() || route.isEmpty()) {
                continue;
            }
            normalized.put(alias, route);
        }
        return Map.copyOf(normalized);
    }
}
