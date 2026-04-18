package de.jexcellence.jexplatform.proxy;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local fallback {@link ProxyService} for non-proxy (single-server) deployments.
 *
 * <p>Supports local in-process action handlers and pending-arrival tokens while
 * reporting transfer operations as unavailable.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class NoOpProxyService implements ProxyService {

    private static final String DEFAULT_CHANNEL = "jexcellence:proxy";
    private static final int DEFAULT_PROTOCOL = 1;

    private final String channelName;
    private final int protocolVersion;
    private final boolean available;
    private final PendingArrivalActionStore pendingArrivalStore;
    private final ConcurrentHashMap<String, ProxyActionHandler> actionHandlers =
            new ConcurrentHashMap<>();

    /**
     * Creates the default no-op proxy service.
     *
     * @return a default no-op proxy service
     */
    public static @NotNull NoOpProxyService createDefault() {
        return new NoOpProxyService(DEFAULT_CHANNEL, DEFAULT_PROTOCOL, false);
    }

    /**
     * Creates a no-op proxy service.
     *
     * @param channelName     the proxy channel name
     * @param protocolVersion the protocol version
     * @param available       the availability flag exposed by {@link #isAvailable()}
     */
    public NoOpProxyService(@NotNull String channelName, int protocolVersion, boolean available) {
        this.channelName = channelName == null || channelName.isBlank()
                ? DEFAULT_CHANNEL : channelName.trim();
        this.protocolVersion = Math.max(1, protocolVersion);
        this.available = available;
        this.pendingArrivalStore = new InMemoryPendingArrivalActionStore();
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public int protocolVersion() {
        return protocolVersion;
    }

    @Override
    public @NotNull String channelName() {
        return channelName;
    }

    @Override
    public @NotNull CompletableFuture<Optional<PlayerPresenceSnapshot>> findPresence(
            @NotNull UUID playerUuid) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public @NotNull CompletableFuture<Map<UUID, PlayerPresenceSnapshot>> findPresence(
            @NotNull Collection<UUID> playerUuids) {
        return CompletableFuture.completedFuture(Map.of());
    }

    @Override
    public @NotNull CompletableFuture<Boolean> requestPlayerTransfer(
            @NotNull ProxyTransferRequest transferRequest) {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public @NotNull CompletableFuture<ProxyActionResult> sendAction(
            @NotNull ProxyActionEnvelope envelope) {
        if (envelope.protocolVersion() != protocolVersion) {
            return CompletableFuture.completedFuture(
                    ProxyActionResult.failure("protocol_mismatch",
                            "Proxy protocol version mismatch."));
        }

        var handler = actionHandlers.get(
                actionKey(envelope.moduleId(), envelope.actionId()));
        if (handler == null) {
            return CompletableFuture.completedFuture(ProxyActionResult.failure(
                    available ? "handler_missing" : "proxy_unavailable",
                    available ? "No handler registered for proxy action."
                            : "Proxy service is unavailable."));
        }

        try {
            return handler.handle(envelope);
        } catch (RuntimeException ex) {
            return CompletableFuture.completedFuture(ProxyActionResult.failure(
                    "handler_exception",
                    "Proxy action handler failed: " + ex.getMessage()));
        }
    }

    @Override
    public void registerActionHandler(@NotNull String moduleId, @NotNull String actionId,
                                      @NotNull ProxyActionHandler handler) {
        actionHandlers.put(actionKey(moduleId, actionId), handler);
    }

    @Override
    public void unregisterActionHandler(@NotNull String moduleId, @NotNull String actionId) {
        actionHandlers.remove(actionKey(moduleId, actionId));
    }

    @Override
    public @NotNull PendingArrivalActionStore pendingArrivals() {
        return pendingArrivalStore;
    }

    private static @NotNull String actionKey(@NotNull String moduleId,
                                             @NotNull String actionId) {
        return moduleId.trim().toLowerCase(Locale.ROOT)
                + '#' + actionId.trim().toLowerCase(Locale.ROOT);
    }
}
