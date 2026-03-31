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

import java.util.Objects;

/**
 * Immutable network-aware location carrying authoritative server identity and coordinates.
 *
 * @param serverId authoritative server route identifier
 * @param worldName world identifier on the owning server
 * @param x X coordinate
 * @param y Y coordinate
 * @param z Z coordinate
 * @param yaw yaw rotation
 * @param pitch pitch rotation
 */
public record NetworkLocation(
    @NotNull String serverId,
    @NotNull String worldName,
    double x,
    double y,
    double z,
    float yaw,
    float pitch
) {

    /**
     * Creates a normalized network location.
     *
     * @param serverId authoritative server route identifier
     * @param worldName world identifier on the owning server
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @param yaw yaw rotation
     * @param pitch pitch rotation
     */
    public NetworkLocation {
        serverId = normalizeRequired(serverId, "serverId");
        worldName = normalizeWorldName(worldName);
    }

    /**
     * Returns a copy using block-center coordinates.
     *
     * @return centered network location
     */
    public @NotNull NetworkLocation toBlockCenter() {
        return new NetworkLocation(
            this.serverId,
            this.worldName,
            Math.floor(this.x) + 0.5D,
            Math.floor(this.y),
            Math.floor(this.z) + 0.5D,
            this.yaw,
            this.pitch
        );
    }

    private static @NotNull String normalizeRequired(final @NotNull String value, final @NotNull String fieldName) {
        final String normalized = Objects.requireNonNull(value, fieldName + " cannot be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return normalized;
    }

    private static @NotNull String normalizeWorldName(final @NotNull String worldName) {
        final String normalized = Objects.requireNonNull(worldName, "worldName cannot be null").trim();
        return normalized.isEmpty() ? "unknown" : normalized;
    }
}
