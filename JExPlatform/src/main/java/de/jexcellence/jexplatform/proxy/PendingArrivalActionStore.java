package de.jexcellence.jexplatform.proxy;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Store for pending arrival tokens used by cross-server action handoff.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public interface PendingArrivalActionStore {

    /**
     * Issues a new pending arrival token.
     *
     * @param playerUuid  the player UUID targeted by this token
     * @param moduleId    the owning module identifier
     * @param actionId    the destination action identifier
     * @param destination the destination network location
     * @param ttl         the token lifetime
     * @param payload     optional destination payload
     * @return the issued pending arrival token
     */
    @NotNull PendingArrivalToken issueToken(
            @NotNull UUID playerUuid, @NotNull String moduleId,
            @NotNull String actionId, @NotNull NetworkLocation destination,
            @NotNull Duration ttl, @NotNull Map<String, String> payload);

    /**
     * Consumes one token by token identifier.
     *
     * @param tokenId the token identifier
     * @return the consumed token when present
     */
    @NotNull Optional<PendingArrivalToken> consumeToken(@NotNull String tokenId);

    /**
     * Consumes the first matching token for a player/module/action on a destination server.
     *
     * @param playerUuid          the player UUID
     * @param moduleId            the module identifier
     * @param actionId            the action identifier
     * @param destinationServerId the destination server route identifier
     * @return the consumed matching token when present
     */
    @NotNull Optional<PendingArrivalToken> consumeFirstForPlayer(
            @NotNull UUID playerUuid, @NotNull String moduleId,
            @NotNull String actionId, @NotNull String destinationServerId);

    /**
     * Removes expired tokens and returns the number removed.
     *
     * @param nowEpochMilli the comparison timestamp in epoch milliseconds
     * @return the count of removed tokens
     */
    int cleanupExpired(long nowEpochMilli);
}
