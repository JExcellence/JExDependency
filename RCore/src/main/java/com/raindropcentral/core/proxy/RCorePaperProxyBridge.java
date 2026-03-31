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

import com.raindropcentral.rplatform.proxy.PendingArrivalActionStore;
import com.raindropcentral.rplatform.proxy.PlayerPresenceSnapshot;
import com.raindropcentral.rplatform.proxy.ProxyActionEnvelope;
import com.raindropcentral.rplatform.proxy.ProxyActionHandler;
import com.raindropcentral.rplatform.proxy.ProxyActionResult;
import com.raindropcentral.rplatform.proxy.ProxyService;
import com.raindropcentral.rplatform.proxy.ProxyTransferRequest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Paper-side proxy bridge backed by the in-process {@link RCoreProxyCoordinator}.
 */
public final class RCorePaperProxyBridge implements ProxyService {

    private static final String BUNGEE_CHANNEL = "BungeeCord";

    private final JavaPlugin plugin;
    private final RCoreProxyCoordinator coordinator;

    /**
     * Creates a Paper-side proxy bridge.
     *
     * @param plugin owning Bukkit plugin
     * @param coordinator in-process proxy coordinator
     */
    public RCorePaperProxyBridge(
        final @NotNull JavaPlugin plugin,
        final @NotNull RCoreProxyCoordinator coordinator
    ) {
        this.plugin = plugin;
        this.coordinator = coordinator;
        this.plugin.getServer().getMessenger().registerOutgoingPluginChannel(this.plugin, BUNGEE_CHANNEL);
        this.coordinator.setTransferExecutor(this::requestTransferViaBungeeChannel);
    }

    /**
     * Shuts down this Paper proxy bridge.
     */
    public void shutdown() {
        this.plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(this.plugin, BUNGEE_CHANNEL);
    }

    /**
     * Returns route identifier for this Paper server.
     *
     * @return local route identifier
     */
    public @NotNull String localRouteId() {
        return this.coordinator.config().serverRouteId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int protocolVersion() {
        return this.coordinator.protocolVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull String channelName() {
        return this.coordinator.channelName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull CompletableFuture<Optional<PlayerPresenceSnapshot>> findPresence(final @NotNull UUID playerUuid) {
        final Player onlinePlayer = Bukkit.getPlayer(playerUuid);
        if (onlinePlayer != null) {
            this.coordinator.updatePresence(playerUuid, this.localRouteId(), true);
        }
        return this.coordinator.findPresence(playerUuid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull CompletableFuture<Map<UUID, PlayerPresenceSnapshot>> findPresence(
        final @NotNull Collection<UUID> playerUuids
    ) {
        final Map<UUID, PlayerPresenceSnapshot> localSnapshots = new LinkedHashMap<>();
        for (final UUID playerUuid : playerUuids) {
            final Player onlinePlayer = Bukkit.getPlayer(playerUuid);
            if (onlinePlayer != null) {
                final PlayerPresenceSnapshot snapshot = PlayerPresenceSnapshot.online(
                    playerUuid,
                    this.localRouteId(),
                    System.currentTimeMillis()
                );
                localSnapshots.put(playerUuid, snapshot);
                this.coordinator.updatePresence(playerUuid, this.localRouteId(), true);
            }
        }

        return this.coordinator.findPresence(playerUuids).thenApply(snapshots -> {
            if (localSnapshots.isEmpty()) {
                return snapshots;
            }
            final Map<UUID, PlayerPresenceSnapshot> merged = new LinkedHashMap<>(snapshots);
            merged.putAll(localSnapshots);
            return Map.copyOf(merged);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull CompletableFuture<Boolean> requestPlayerTransfer(final @NotNull ProxyTransferRequest transferRequest) {
        return this.coordinator.requestPlayerTransfer(transferRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull CompletableFuture<ProxyActionResult> sendAction(final @NotNull ProxyActionEnvelope envelope) {
        return this.coordinator.sendAction(envelope);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerActionHandler(
        final @NotNull String moduleId,
        final @NotNull String actionId,
        final @NotNull ProxyActionHandler handler
    ) {
        this.coordinator.registerActionHandler(moduleId, actionId, handler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterActionHandler(final @NotNull String moduleId, final @NotNull String actionId) {
        this.coordinator.unregisterActionHandler(moduleId, actionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NotNull PendingArrivalActionStore pendingArrivals() {
        return this.coordinator.pendingArrivals();
    }

    private @NotNull CompletableFuture<Boolean> requestTransferViaBungeeChannel(
        final @NotNull ProxyTransferRequest transferRequest
    ) {
        final Player player = Bukkit.getPlayer(transferRequest.playerUuid());
        if (player == null || !player.isOnline()) {
            return CompletableFuture.completedFuture(false);
        }
        if (transferRequest.targetServerId().equalsIgnoreCase(this.localRouteId())) {
            return CompletableFuture.completedFuture(true);
        }

        try {
            final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            try (DataOutputStream outputStream = new DataOutputStream(byteStream)) {
                outputStream.writeUTF("Connect");
                outputStream.writeUTF(transferRequest.targetServerId());
            }
            player.sendPluginMessage(this.plugin, BUNGEE_CHANNEL, byteStream.toByteArray());
            return CompletableFuture.completedFuture(true);
        } catch (IOException exception) {
            return CompletableFuture.completedFuture(false);
        }
    }
}
