package de.jexcellence.jexplatform.proxy;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Immutable network-aware location carrying authoritative server identity and coordinates.
 *
 * @param serverId  the authoritative server route identifier
 * @param worldName the world identifier on the owning server
 * @param x         the X coordinate
 * @param y         the Y coordinate
 * @param z         the Z coordinate
 * @param yaw       the yaw rotation
 * @param pitch     the pitch rotation
 * @author JExcellence
 * @since 1.0.0
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

    /** Compact constructor with normalization. */
    public NetworkLocation {
        serverId = normalizeRequired(serverId, "serverId");
        worldName = normalizeWorldName(worldName);
    }

    /**
     * Returns a copy using block-center coordinates.
     *
     * @return a centered network location
     */
    public @NotNull NetworkLocation toBlockCenter() {
        return new NetworkLocation(
                serverId, worldName,
                Math.floor(x) + 0.5D, Math.floor(y), Math.floor(z) + 0.5D,
                yaw, pitch);
    }

    private static @NotNull String normalizeRequired(@NotNull String value,
                                                     @NotNull String fieldName) {
        var normalized = Objects.requireNonNull(value, fieldName + " cannot be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return normalized;
    }

    private static @NotNull String normalizeWorldName(@NotNull String worldName) {
        var normalized = Objects.requireNonNull(worldName, "worldName cannot be null").trim();
        return normalized.isEmpty() ? "unknown" : normalized;
    }
}
