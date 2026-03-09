package com.raindropcentral.rdr.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.database.entity.RTradeDelivery;
import com.raindropcentral.rdr.database.entity.RTradeSession;
import com.raindropcentral.rdr.database.repository.RRTradeDelivery;
import com.raindropcentral.rdr.database.repository.RRTradeSession;
import com.raindropcentral.rdr.view.TradeSessionView;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
        this.watchedTradeViewers.computeIfAbsent(validatedTradeUuid, ignored -> new HashSet<>()).add(validatedPlayerUuid);
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

            for (final UUID viewerUuid : viewers) {
                final Player viewer = Bukkit.getPlayer(viewerUuid);
                if (viewer == null || !viewer.isOnline()) {
                    continue;
                }

                this.plugin.getScheduler().runSync(() -> this.plugin.getViewFrame().open(
                    TradeSessionView.class,
                    viewer,
                    Map.of("plugin", this.plugin, "trade_uuid", tradeUuid)
                ));
            }
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
}
