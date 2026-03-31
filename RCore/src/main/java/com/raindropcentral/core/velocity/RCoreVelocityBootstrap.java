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

package com.raindropcentral.core.velocity;

import com.google.inject.Inject;
import com.raindropcentral.core.proxy.ProxyActionEnvelopeCodec;
import com.raindropcentral.core.proxy.ProxyHostConfig;
import com.raindropcentral.core.proxy.RCoreProxyCoordinator;
import com.raindropcentral.rplatform.proxy.ProxyActionEnvelope;
import com.raindropcentral.rplatform.proxy.ProxyTransferRequest;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Velocity bootstrap for RCore proxy coordination.
 */
@Plugin(
    id = "rcore",
    name = "RCore",
    version = "2.0.0",
    description = "Raindrop proxy coordinator"
)
public final class RCoreVelocityBootstrap {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final ProxyActionEnvelopeCodec envelopeCodec;

    private RCoreProxyCoordinator coordinator;
    private ChannelIdentifier channelIdentifier;

    /**
     * Creates the Velocity bootstrap.
     *
     * @param proxyServer Velocity proxy server
     * @param logger bootstrap logger
     */
    @Inject
    public RCoreVelocityBootstrap(
        final @NotNull ProxyServer proxyServer,
        final @NotNull Logger logger
    ) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.envelopeCodec = new ProxyActionEnvelopeCodec();
    }

    /**
     * Initializes the proxy coordinator and registers event listeners.
     *
     * @param event proxy initialize event
     */
    @Subscribe
    public void onProxyInitialize(final @NotNull ProxyInitializeEvent event) {
        this.coordinator = new RCoreProxyCoordinator(this.resolveConfig());
        this.coordinator.setTransferExecutor(this::requestTransferOnVelocity);

        this.channelIdentifier = MinecraftChannelIdentifier.from(this.coordinator.channelName());
        this.proxyServer.getChannelRegistrar().register(this.channelIdentifier);
        this.logger.info(
            "RCore proxy host initialized. channel={}, protocolVersion={}",
            this.coordinator.channelName(),
            this.coordinator.protocolVersion()
        );
    }

    /**
     * Unregisters proxy channels on proxy shutdown.
     *
     * @param event proxy shutdown event
     */
    @Subscribe
    public void onProxyShutdown(final @NotNull ProxyShutdownEvent event) {
        if (this.channelIdentifier != null) {
            this.proxyServer.getChannelRegistrar().unregister(this.channelIdentifier);
        }
    }

    /**
     * Updates presence snapshots when players log in.
     *
     * @param event post-login event
     */
    @Subscribe
    public void onPostLogin(final @NotNull PostLoginEvent event) {
        this.updatePlayerPresence(event.getPlayer());
    }

    /**
     * Updates presence snapshots when players change backend servers.
     *
     * @param event server-connected event
     */
    @Subscribe
    public void onServerConnected(final @NotNull ServerConnectedEvent event) {
        this.updatePlayerPresence(event.getPlayer());
    }

    /**
     * Removes presence snapshots when players disconnect.
     *
     * @param event disconnect event
     */
    @Subscribe
    public void onDisconnect(final @NotNull DisconnectEvent event) {
        if (this.coordinator != null) {
            this.coordinator.removePresence(event.getPlayer().getUniqueId());
        }
    }

    /**
     * Handles incoming proxy action payloads on the configured plugin channel.
     *
     * @param event plugin-message event
     */
    @Subscribe
    public void onPluginMessage(final @NotNull PluginMessageEvent event) {
        if (this.coordinator == null || this.channelIdentifier == null) {
            return;
        }
        if (!event.getIdentifier().equals(this.channelIdentifier)) {
            return;
        }

        try {
            final ProxyActionEnvelope envelope = this.envelopeCodec.decode(event.getData());
            this.coordinator.sendAction(envelope);
            event.setResult(PluginMessageEvent.ForwardResult.handled());
        } catch (RuntimeException exception) {
            this.logger.warn("Failed to decode proxy action payload", exception);
        }
    }

    private @NotNull CompletableFuture<Boolean> requestTransferOnVelocity(
        final @NotNull ProxyTransferRequest transferRequest
    ) {
        final Optional<Player> player = this.proxyServer.getPlayer(transferRequest.playerUuid());
        if (player.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        final var targetServer = this.proxyServer.getServer(transferRequest.targetServerId());
        if (targetServer.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        return player.get()
            .createConnectionRequest(targetServer.get())
            .connect()
            .thenApply(ConnectionRequestBuilder.Result::isSuccessful)
            .exceptionally(error -> false);
    }

    private void updatePlayerPresence(final @NotNull Player player) {
        if (this.coordinator == null) {
            return;
        }
        final String serverId = player.getCurrentServer()
            .map(connection -> connection.getServerInfo().getName())
            .orElse(this.coordinator.config().serverRouteId());
        this.coordinator.updatePresence(player.getUniqueId(), serverId, true);
    }

    private @NotNull ProxyHostConfig resolveConfig() {
        final String channelName = System.getProperty("raindrop.proxy.channel", "raindrop:proxy");
        final int protocolVersion = parseIntProperty("raindrop.proxy.protocol-version", 1);
        final long requestTimeoutMillis = parseLongProperty("raindrop.proxy.request-timeout-millis", 5_000L);
        final long tokenTtlMillis = parseLongProperty("raindrop.proxy.token-ttl-millis", 120_000L);
        final String serverRouteId = System.getProperty("raindrop.proxy.server-route-id", "proxy");
        return new ProxyHostConfig(
            channelName,
            protocolVersion,
            requestTimeoutMillis,
            tokenTtlMillis,
            serverRouteId,
            Map.of()
        );
    }

    private static int parseIntProperty(final @NotNull String key, final int fallback) {
        final String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static long parseLongProperty(final @NotNull String key, final long fallback) {
        final String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
