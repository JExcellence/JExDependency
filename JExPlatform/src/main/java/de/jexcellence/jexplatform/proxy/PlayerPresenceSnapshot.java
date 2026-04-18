package de.jexcellence.jexplatform.proxy;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable cross-server presence snapshot for one player.
 *
 * @param playerUuid          the player UUID
 * @param serverId            the current server route identifier
 * @param online              whether the player is currently online
 * @param capturedAtEpochMilli the snapshot timestamp in epoch milliseconds
 * @author JExcellence
 * @since 1.0.0
 */
public record PlayerPresenceSnapshot(
        @NotNull UUID playerUuid,
        @NotNull String serverId,
        boolean online,
        long capturedAtEpochMilli
) {

    /** Compact constructor with normalization. */
    public PlayerPresenceSnapshot {
        playerUuid = Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        serverId = normalizeServerId(serverId);
        capturedAtEpochMilli = Math.max(0L, capturedAtEpochMilli);
    }

    /**
     * Creates an online presence snapshot.
     *
     * @param playerUuid          the player UUID
     * @param serverId            the server route identifier
     * @param capturedAtEpochMilli the snapshot timestamp
     * @return an online snapshot
     */
    public static @NotNull PlayerPresenceSnapshot online(
            @NotNull UUID playerUuid, @NotNull String serverId, long capturedAtEpochMilli) {
        return new PlayerPresenceSnapshot(playerUuid, serverId, true, capturedAtEpochMilli);
    }

    /**
     * Creates an offline presence snapshot.
     *
     * @param playerUuid          the player UUID
     * @param capturedAtEpochMilli the snapshot timestamp
     * @return an offline snapshot
     */
    public static @NotNull PlayerPresenceSnapshot offline(
            @NotNull UUID playerUuid, long capturedAtEpochMilli) {
        return new PlayerPresenceSnapshot(playerUuid, "offline", false, capturedAtEpochMilli);
    }

    private static @NotNull String normalizeServerId(@NotNull String serverId) {
        var normalized = Objects.requireNonNull(serverId, "serverId cannot be null").trim();
        return normalized.isEmpty() ? "offline" : normalized;
    }
}
