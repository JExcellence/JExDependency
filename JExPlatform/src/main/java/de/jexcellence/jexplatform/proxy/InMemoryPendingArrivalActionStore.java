package de.jexcellence.jexplatform.proxy;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link PendingArrivalActionStore} implementation.
 *
 * <p>Process-local; best suited for no-proxy fallback or unit tests.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class InMemoryPendingArrivalActionStore implements PendingArrivalActionStore {

    private static final long MIN_TTL_MILLIS = 1_000L;

    private final ConcurrentHashMap<String, PendingArrivalToken> tokensById =
            new ConcurrentHashMap<>();

    @Override
    public @NotNull PendingArrivalToken issueToken(
            @NotNull UUID playerUuid, @NotNull String moduleId,
            @NotNull String actionId, @NotNull NetworkLocation destination,
            @NotNull Duration ttl, @NotNull Map<String, String> payload) {
        var now = System.currentTimeMillis();
        var ttlMillis = Math.max(MIN_TTL_MILLIS, ttl.toMillis());
        var token = new PendingArrivalToken(
                UUID.randomUUID().toString(),
                playerUuid, moduleId, actionId, destination,
                payload, now, now + ttlMillis);
        tokensById.put(token.tokenId(), token);
        return token;
    }

    @Override
    public @NotNull Optional<PendingArrivalToken> consumeToken(@NotNull String tokenId) {
        return Optional.ofNullable(tokensById.remove(tokenId));
    }

    @Override
    public @NotNull Optional<PendingArrivalToken> consumeFirstForPlayer(
            @NotNull UUID playerUuid, @NotNull String moduleId,
            @NotNull String actionId, @NotNull String destinationServerId) {
        var candidates = new ArrayList<>(tokensById.values());
        candidates.sort(Comparator.comparingLong(PendingArrivalToken::issuedAtEpochMilli));

        for (var token : candidates) {
            if (!token.matches(playerUuid, moduleId, actionId, destinationServerId)) {
                continue;
            }
            if (tokensById.remove(token.tokenId(), token)) {
                return Optional.of(token);
            }
        }
        return Optional.empty();
    }

    @Override
    public int cleanupExpired(long nowEpochMilli) {
        var removed = 0;
        for (var entry : tokensById.entrySet()) {
            if (entry.getValue().isExpired(nowEpochMilli)
                    && tokensById.remove(entry.getKey(), entry.getValue())) {
                removed++;
            }
        }
        return removed;
    }

    /**
     * Returns the current number of stored tokens.
     *
     * @return the token count
     */
    int size() {
        return tokensById.size();
    }
}
