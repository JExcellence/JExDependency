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

package com.raindropcentral.rdr.service;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.database.entity.RTradeDelivery;
import com.raindropcentral.rdr.database.entity.RTradeSession;
import com.raindropcentral.rdr.database.repository.RRTradeDelivery;
import com.raindropcentral.rdr.database.repository.RRTradeSession;
import com.raindropcentral.rdr.view.TradeSessionView;
import com.raindropcentral.rplatform.proxy.ProxyActionEnvelope;
import com.raindropcentral.rplatform.proxy.ProxyActionResult;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Poll-based cross-server trade inbox synchronization service.
 *
 * <p>This scheduler periodically polls pending invites, active watched-session revisions, and pending
 * delivery counts using only shared database reads.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class TradeInboxPollService {

    private static final int DEFAULT_EXPIRY_CHECK_INTERVAL_POLLS = 30;
    private static final String ACTION_MODULE_ID = "rdr";
    private static final String ACTION_ID_TRADE_SESSION_REFRESH = "trade_session_live_refresh";
    private static final String ACTION_PAYLOAD_KEY_TRADE_UUID = "trade_uuid";
    private static final String OFFLINE_SERVER_ID = "offline";
    private static final String UNKNOWN_SERVER_ID = "unknown";

    private final RDR plugin;
    private final Map<String, Long> inviteRevisionCache = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> deliveryCountCache = new ConcurrentHashMap<>();
    private final Map<UUID, Long> watchedTradeRevisionCache = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> watchedTradeViewers = new ConcurrentHashMap<>();
    private boolean running;
    private int pollCounter;

    /**
     * Creates a trade inbox poller for the active plugin runtime.
     *
     * @param plugin active plugin runtime
     * @throws NullPointerException if {@code plugin} is {@code null}
     */
    public TradeInboxPollService(final @NotNull RDR plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Starts recurring DB polling using configured trade poll interval.
     */
    public void start() {
        if (this.running || this.plugin.getScheduler() == null) {
            return;
        }

        final long pollInterval = Math.max(1L, this.plugin.getDefaultConfig().getTradePollIntervalTicks());
        this.plugin.getScheduler().runRepeatingAsync(this::poll, pollInterval, pollInterval);
        this.plugin.getProxyService().registerActionHandler(
            ACTION_MODULE_ID,
            ACTION_ID_TRADE_SESSION_REFRESH,
            this::handleProxyTradeSessionRefresh
        );
        this.running = true;
    }

    /**
     * Stops this poller and clears local cache state.
     */
    public void shutdown() {
        this.running = false;
        this.inviteRevisionCache.clear();
        this.deliveryCountCache.clear();
        this.watchedTradeRevisionCache.clear();
        this.watchedTradeViewers.clear();
        this.plugin.getProxyService().unregisterActionHandler(
            ACTION_MODULE_ID,
            ACTION_ID_TRADE_SESSION_REFRESH
        );
    }

    /**
     * Registers one player as an active viewer of a trade session.
     *
     * @param tradeUuid watched trade UUID
     * @param playerUuid viewer UUID
     * @throws NullPointerException if any argument is {@code null}
     */
    public void watchTradeSession(
        final @NotNull UUID tradeUuid,
        final @NotNull UUID playerUuid
    ) {
        final UUID validatedTradeUuid = Objects.requireNonNull(tradeUuid, "tradeUuid cannot be null");
        final UUID validatedPlayerUuid = Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        this.watchedTradeViewers.computeIfAbsent(validatedTradeUuid, ignored -> ConcurrentHashMap.newKeySet())
            .add(validatedPlayerUuid);
    }

    /**
     * Removes one player as an active viewer of a trade session.
     *
     * @param tradeUuid watched trade UUID
     * @param playerUuid viewer UUID
     * @throws NullPointerException if any argument is {@code null}
     */
    public void unwatchTradeSession(
        final @NotNull UUID tradeUuid,
        final @NotNull UUID playerUuid
    ) {
        final UUID validatedTradeUuid = Objects.requireNonNull(tradeUuid, "tradeUuid cannot be null");
        final UUID validatedPlayerUuid = Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        final Set<UUID> viewers = this.watchedTradeViewers.get(validatedTradeUuid);
        if (viewers == null) {
            return;
        }

        viewers.remove(validatedPlayerUuid);
        if (viewers.isEmpty()) {
            this.watchedTradeViewers.remove(validatedTradeUuid);
            this.watchedTradeRevisionCache.remove(validatedTradeUuid);
        }
    }

    /**
     * Immediately refreshes open watched-session views for one trade UUID on this server.
     *
     * <p>This is used by live trade UI mutations so participants can see partner offer changes
     * without waiting for the next asynchronous poll cycle.</p>
     *
     * @param tradeUuid watched trade UUID
     * @return {@code true} when at least one local viewer refresh was scheduled
     * @throws NullPointerException if {@code tradeUuid} is {@code null}
     */
    public boolean refreshWatchedTradeSession(final @NotNull UUID tradeUuid) {
        final UUID validatedTradeUuid = Objects.requireNonNull(tradeUuid, "tradeUuid cannot be null");
        final Set<UUID> viewers = this.watchedTradeViewers.get(validatedTradeUuid);
        if (viewers == null || viewers.isEmpty()) {
            return false;
        }
        return this.refreshWatchedTradeSessionViewers(validatedTradeUuid, viewers);
    }

    /**
     * Publishes one live trade-session refresh to local viewers and remote participant servers.
     *
     * @param session updated trade session snapshot
     * @param actorUuid participant UUID who triggered the mutation
     * @return {@code true} when at least one local viewer refresh was scheduled
     * @throws NullPointerException if any argument is {@code null}
     */
    public boolean publishLiveTradeSessionRefresh(
        final @NotNull RTradeSession session,
        final @NotNull UUID actorUuid
    ) {
        final RTradeSession validatedSession = Objects.requireNonNull(session, "session cannot be null");
        final UUID validatedActorUuid = Objects.requireNonNull(actorUuid, "actorUuid cannot be null");
        final boolean localRefreshScheduled = this.refreshWatchedTradeSession(validatedSession.getTradeUuid());
        this.publishProxyTradeSessionRefresh(validatedSession, validatedActorUuid);
        return localRefreshScheduled;
    }

    private void poll() {
        if (!this.running) {
            return;
        }

        final List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (onlinePlayers.isEmpty()) {
            return;
        }
        final List<UUID> onlineUuids = onlinePlayers.stream().map(Player::getUniqueId).toList();

        final RRTradeSession sessionRepository = this.plugin.getTradeSessionRepository();
        final RRTradeDelivery deliveryRepository = this.plugin.getTradeDeliveryRepository();
        if (sessionRepository == null || deliveryRepository == null) {
            return;
        }

        this.pollInvites(sessionRepository, onlinePlayers, onlineUuids);
        this.pollDeliveries(deliveryRepository, onlinePlayers, onlineUuids);
        this.pollWatchedSessions(sessionRepository);
        this.pollExpiry(sessionRepository);
    }

    private void pollInvites(
        final @NotNull RRTradeSession sessionRepository,
        final @NotNull List<Player> onlinePlayers,
        final @NotNull List<UUID> onlineUuids
    ) {
        final Map<UUID, Player> onlineByUuid = mapPlayersByUuid(onlinePlayers);
        final List<RTradeSession> invites = sessionRepository.findPendingInvitesForRecipients(onlineUuids, LocalDateTime.now());
        for (final RTradeSession invite : invites) {
            final Player partner = onlineByUuid.get(invite.getPartnerUuid());
            if (partner == null) {
                continue;
            }

            final String cacheKey = invite.getTradeUuid() + ":" + invite.getPartnerUuid();
            final long previousRevision = this.inviteRevisionCache.getOrDefault(cacheKey, -1L);
            if (previousRevision == invite.getRevision()) {
                continue;
            }

            this.inviteRevisionCache.put(cacheKey, invite.getRevision());
            this.plugin.getScheduler().runSync(() -> new I18n.Builder("trade.message.invite_received", partner)
                .withPlaceholder("from", this.resolvePlayerName(invite.getInitiatorUuid()))
                .build()
                .sendMessage());
        }
    }

    private void pollDeliveries(
        final @NotNull RRTradeDelivery deliveryRepository,
        final @NotNull List<Player> onlinePlayers,
        final @NotNull List<UUID> onlineUuids
    ) {
        final Map<UUID, Integer> pendingCounts = new HashMap<>();
        final List<RTradeDelivery> pendingDeliveries = deliveryRepository.findPendingByRecipients(onlineUuids);
        for (final RTradeDelivery delivery : pendingDeliveries) {
            pendingCounts.merge(delivery.getRecipientUuid(), 1, Integer::sum);
        }

        for (final Player onlinePlayer : onlinePlayers) {
            final UUID playerUuid = onlinePlayer.getUniqueId();
            final int count = pendingCounts.getOrDefault(playerUuid, 0);
            final int previousCount = this.deliveryCountCache.getOrDefault(playerUuid, 0);
            this.deliveryCountCache.put(playerUuid, count);
            if (count <= 0 || count == previousCount) {
                continue;
            }

            this.plugin.getScheduler().runSync(() -> new I18n.Builder("trade.message.delivery_pending", onlinePlayer)
                .withPlaceholder("count", count)
                .build()
                .sendMessage());
        }
    }

    private void pollWatchedSessions(final @NotNull RRTradeSession sessionRepository) {
        if (this.watchedTradeViewers.isEmpty()) {
            return;
        }

        final List<UUID> watchedTradeUuids = new ArrayList<>(this.watchedTradeViewers.keySet());
        final List<RTradeSession> watchedSessions = sessionRepository.findByTradeUuids(watchedTradeUuids);
        for (final RTradeSession session : watchedSessions) {
            final UUID tradeUuid = session.getTradeUuid();
            final long previousRevision = this.watchedTradeRevisionCache.getOrDefault(tradeUuid, -1L);
            if (previousRevision == session.getRevision()) {
                continue;
            }
            this.watchedTradeRevisionCache.put(tradeUuid, session.getRevision());

            final Set<UUID> viewers = this.watchedTradeViewers.get(tradeUuid);
            if (viewers == null || viewers.isEmpty()) {
                continue;
            }
            this.refreshWatchedTradeSessionViewers(tradeUuid, viewers);
        }
    }

    private void pollExpiry(final @NotNull RRTradeSession sessionRepository) {
        this.pollCounter++;
        if (this.pollCounter < DEFAULT_EXPIRY_CHECK_INTERVAL_POLLS) {
            return;
        }
        this.pollCounter = 0;
        sessionRepository.expireInvitesAsync(LocalDateTime.now());
    }

    private static @NotNull Map<UUID, Player> mapPlayersByUuid(final @NotNull List<Player> players) {
        final Map<UUID, Player> map = new HashMap<>();
        for (final Player player : players) {
            map.put(player.getUniqueId(), player);
        }
        return map;
    }

    private @NotNull String resolvePlayerName(final @NotNull UUID playerUuid) {
        final Player onlinePlayer = Bukkit.getPlayer(playerUuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }
        final var offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
        return offlinePlayer.getName() == null ? playerUuid.toString() : offlinePlayer.getName();
    }

    private @NotNull CompletableFuture<ProxyActionResult> handleProxyTradeSessionRefresh(
        final @NotNull ProxyActionEnvelope envelope
    ) {
        final String tradeUuidValue = envelope.payload().get(ACTION_PAYLOAD_KEY_TRADE_UUID);
        if (tradeUuidValue == null || tradeUuidValue.isBlank()) {
            return CompletableFuture.completedFuture(
                ProxyActionResult.failure("invalid_payload", "Missing trade session refresh payload.")
            );
        }

        final UUID parsedTradeUuid;
        try {
            parsedTradeUuid = UUID.fromString(tradeUuidValue);
        } catch (IllegalArgumentException exception) {
            return CompletableFuture.completedFuture(
                ProxyActionResult.failure("invalid_trade_uuid", "Trade session refresh payload is invalid.")
            );
        }

        this.refreshWatchedTradeSession(parsedTradeUuid);
        return CompletableFuture.completedFuture(ProxyActionResult.success("Trade session refresh dispatched."));
    }

    private void publishProxyTradeSessionRefresh(
        final @NotNull RTradeSession session,
        final @NotNull UUID actorUuid
    ) {
        if (!this.plugin.getProxyService().isAvailable()) {
            return;
        }

        final String sourceServerId = normalizeServerId(this.plugin.getServerRouteId(), "server");
        final Set<String> targetServerIds = new java.util.LinkedHashSet<>();
        targetServerIds.add(normalizeServerId(session.getInitiatorLastKnownServerId(), OFFLINE_SERVER_ID));
        targetServerIds.add(normalizeServerId(session.getPartnerLastKnownServerId(), OFFLINE_SERVER_ID));
        for (final String targetServerId : targetServerIds) {
            if (targetServerId.isBlank()
                || OFFLINE_SERVER_ID.equalsIgnoreCase(targetServerId)
                || UNKNOWN_SERVER_ID.equalsIgnoreCase(targetServerId)
                || sourceServerId.equalsIgnoreCase(targetServerId)) {
                continue;
            }

            this.plugin.getProxyService().sendAction(new ProxyActionEnvelope(
                UUID.randomUUID(),
                this.plugin.getProxyService().protocolVersion(),
                ACTION_MODULE_ID,
                ACTION_ID_TRADE_SESSION_REFRESH,
                actorUuid,
                sourceServerId,
                targetServerId,
                "",
                Map.of(ACTION_PAYLOAD_KEY_TRADE_UUID, session.getTradeUuid().toString()),
                System.currentTimeMillis()
            ));
        }
    }

    private boolean refreshWatchedTradeSessionViewers(
        final @NotNull UUID tradeUuid,
        final @NotNull Set<UUID> viewers
    ) {
        if (this.plugin.getScheduler() == null || this.plugin.getViewFrame() == null || viewers.isEmpty()) {
            return false;
        }

        final Set<UUID> viewerSnapshot = Set.copyOf(viewers);
        this.plugin.getScheduler().runSync(() -> {
            for (final UUID viewerUuid : viewerSnapshot) {
                final Player viewer = Bukkit.getPlayer(viewerUuid);
                if (viewer == null || !viewer.isOnline()) {
                    continue;
                }

                this.plugin.getViewFrame().open(
                    TradeSessionView.class,
                    viewer,
                    Map.of("plugin", this.plugin, "trade_uuid", tradeUuid)
                );
            }
        });
        return true;
    }

    private static @NotNull String normalizeServerId(final @Nullable String serverId, final @NotNull String fallback) {
        if (serverId == null || serverId.isBlank()) {
            return fallback;
        }
        return serverId.trim();
    }
}
