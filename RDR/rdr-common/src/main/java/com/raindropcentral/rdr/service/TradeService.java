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

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.raindropcentral.rdr.RDR;
import com.raindropcentral.rdr.configs.ConfigSection;
import com.raindropcentral.rdr.database.entity.RServerBank;
import com.raindropcentral.rdr.database.entity.RTradeDelivery;
import com.raindropcentral.rdr.database.entity.RTradeSession;
import com.raindropcentral.rdr.database.entity.TradeSessionStatus;
import com.raindropcentral.rdr.database.repository.RRServerBank;
import com.raindropcentral.rdr.database.repository.RRTradeDelivery;
import com.raindropcentral.rdr.database.repository.RRTradeSession;
import com.raindropcentral.rplatform.proxy.PlayerPresenceSnapshot;
import com.raindropcentral.rplatform.proxy.ProxyTransferRequest;
import com.raindropcentral.rplatform.economy.JExEconomyBridge;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Service facade for DB-first cross-server trade flows and escrow operations.
 *
 * <p>This service orchestrates invite/session mutations through repository async methods and handles
 * local inventory/economy side-effects for escrow updates and delivery claims.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class TradeService {

    private static final double EPSILON = 1.0E-6D;
    private static final double VAULT_STEP = 100.0D;
    private static final String VAULT_CURRENCY_ID = "vault";
    private static final String JEX_ADAPTER_CLASS = "de.jexcellence.economy.adapter.CurrencyAdapter";
    private static final String TRADE_TAX_LEDGER_NOTE = "trade_tax_collection";
    private static final String TRADE_TAX_ROLLBACK_NOTE = "trade_tax_rollback";
    private static final String SERVER_BANK_ADMIN_WITHDRAW_NOTE = "admin_withdrawal";
    private static final String SERVER_BANK_ADMIN_WITHDRAW_ROLLBACK_NOTE = "admin_withdrawal_rollback";
    private static final String OFFLINE_SERVER_ID = "offline";
    private static final String MODULE_ID = "rdr";
    private static final String JOIN_PARTNER_ACTION_ID = "join_partner_server";
    private static final long PROXY_PRESENCE_LOOKUP_TIMEOUT_MILLIS = 350L;

    /**
     * Invite operation result.
     */
    public enum InviteResult {
        /**
         * Invite row was created.
         */
        SUCCESS,
        /**
         * Trade subsystem is disabled in config.
         */
        DISABLED,
        /**
         * Initiator attempted to invite themselves.
         */
        SELF_TARGET,
        /**
         * This participant pair already has a non-terminal trade session.
         */
        PARTICIPANT_BUSY,
        /**
         * Initiator is still on invite cooldown.
         */
        COOLDOWN,
        /**
         * Trade repositories are unavailable.
         */
        UNAVAILABLE
    }

    /**
     * Trade invite creation response including invite status and created trade UUID when available.
     *
     * @param result invite operation result
     * @param tradeUuid created trade UUID, or {@code null} when invite creation did not persist a session
     */
    public record InviteCreateResponse(
        @NotNull InviteResult result,
        @Nullable UUID tradeUuid
    ) {
    }

    /**
     * Session mutation result.
     */
    public enum SessionResult {
        /**
         * Mutation completed.
         */
        SUCCESS,
        /**
         * Waiting for the other participant confirmation.
         */
        WAITING_FOR_PARTNER,
        /**
         * Trade finalized into delivery rows.
         */
        COMPLETED,
        /**
         * Requested item offer requires a non-air main-hand stack.
         */
        NO_ITEM_IN_HAND,
        /**
         * Offer is already at configured slot limit.
         */
        OFFER_FULL,
        /**
         * Requested currency operation lacks required funds.
         */
        INSUFFICIENT_FUNDS,
        /**
         * Requested trade/delivery row was not found.
         */
        MISSING,
        /**
         * Requesting actor is not allowed for this row.
         */
        FORBIDDEN,
        /**
         * Session state does not permit this mutation.
         */
        INVALID_STATE,
        /**
         * Session revision is stale and must be reloaded.
         */
        STALE,
        /**
         * Invite has expired.
         */
        EXPIRED,
        /**
         * Trade repositories are unavailable.
         */
        UNAVAILABLE
    }

    /**
     * Presence state used by trade target discovery and UI rendering.
     */
    public enum PresenceState {
        /**
         * Participant is online on the local server.
         */
        LOCAL_ONLINE,
        /**
         * Participant is online on a different network server.
         */
        REMOTE_ONLINE,
        /**
         * Participant is currently offline.
         */
        OFFLINE
    }

    /**
     * Proxy join-partner operation result.
     */
    public enum JoinPartnerResult {
        /**
         * Player transfer request was accepted by the proxy bridge.
         */
        ROUTING,
        /**
         * Requesting actor is already on the destination server.
         */
        ALREADY_ON_SERVER,
        /**
         * Trade row could not be found.
         */
        MISSING,
        /**
         * Requesting actor is not a participant in the selected trade.
         */
        FORBIDDEN,
        /**
         * Partner is offline or no destination route is available.
         */
        PARTNER_UNAVAILABLE,
        /**
         * Proxy-backed routing is disabled or unavailable.
         */
        UNAVAILABLE
    }

    /**
     * Trade target snapshot combining identity and presence details.
     *
     * @param targetUuid target player UUID
     * @param targetName resolved target display name
     * @param presenceState resolved target presence state
     * @param serverId target server route identifier, or {@code offline}
     */
    public record TradeTargetSnapshot(
        @NotNull UUID targetUuid,
        @NotNull String targetName,
        @NotNull PresenceState presenceState,
        @NotNull String serverId
    ) {
    }

    /**
     * Join-partner routing response with resolved destination metadata.
     *
     * @param result join-partner result
     * @param serverId resolved destination server route identifier
     */
    public record JoinPartnerResponse(
        @NotNull JoinPartnerResult result,
        @NotNull String serverId
    ) {
    }

    /**
     * Immutable server-bank currency snapshot for admin UI rendering.
     *
     * @param currencyId normalized currency identifier
     * @param amount current server-bank balance for the currency
     * @param ledgerCount number of ledger entries recorded for the currency
     * @param latestEntry newest ledger entry for the currency, or {@code null} when none exist
     */
    public record ServerBankCurrencySnapshot(
        @NotNull String currencyId,
        double amount,
        int ledgerCount,
        @Nullable RServerBank.LedgerEntry latestEntry
    ) {

        /**
         * Creates a normalized immutable server-bank currency snapshot.
         *
         * @param currencyId normalized currency identifier
         * @param amount current server-bank balance for the currency
         * @param ledgerCount number of ledger entries recorded for the currency
         * @param latestEntry newest ledger entry for the currency, or {@code null} when none exist
         */
        public ServerBankCurrencySnapshot {
            currencyId = normalizeCurrencyId(currencyId);
            amount = Math.max(0.0D, amount);
            ledgerCount = Math.max(0, ledgerCount);
        }
    }

    private final RDR plugin;
    private final Map<UUID, LocalDateTime> inviteCooldowns = new ConcurrentHashMap<>();

    /**
     * Creates a trade service for the active plugin runtime.
     *
     * @param plugin active plugin runtime
     * @throws NullPointerException if {@code plugin} is {@code null}
     */
    public TradeService(final @NotNull RDR plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /**
     * Returns pending invites for one participant UUID.
     *
     * @param participantUuid participant UUID
     * @return immutable pending-invite list
     * @throws NullPointerException if {@code participantUuid} is {@code null}
     */
    public @NotNull List<RTradeSession> findPendingInvites(final @NotNull UUID participantUuid) {
        final RRTradeSession sessionRepository = this.plugin.getTradeSessionRepository();
        if (sessionRepository == null) {
            return List.of();
        }
        return sessionRepository.findPendingInvitesForRecipients(
            List.of(Objects.requireNonNull(participantUuid, "participantUuid cannot be null")),
            LocalDateTime.now()
        );
    }

    /**
     * Returns non-terminal trade sessions involving one participant UUID.
     *
     * @param participantUuid participant UUID
     * @return immutable non-terminal session list
     * @throws NullPointerException if {@code participantUuid} is {@code null}
     */
    public @NotNull List<RTradeSession> findNonTerminalSessions(final @NotNull UUID participantUuid) {
        final RRTradeSession sessionRepository = this.plugin.getTradeSessionRepository();
        if (sessionRepository == null) {
            return List.of();
        }
        return sessionRepository.findNonTerminalByParticipants(List.of(
            Objects.requireNonNull(participantUuid, "participantUuid cannot be null")
        ));
    }

    /**
     * Returns pending delivery rows for one recipient UUID.
     *
     * @param recipientUuid recipient UUID
     * @return immutable pending-delivery list
     * @throws NullPointerException if {@code recipientUuid} is {@code null}
     */
    public @NotNull List<RTradeDelivery> findPendingDeliveries(final @NotNull UUID recipientUuid) {
        final RRTradeDelivery deliveryRepository = this.plugin.getTradeDeliveryRepository();
        if (deliveryRepository == null) {
            return List.of();
        }
        return deliveryRepository.findPendingByRecipient(Objects.requireNonNull(recipientUuid, "recipientUuid cannot be null"));
    }

    /**
     * Returns per-currency server-bank snapshots for trade-tax administration.
     *
     * @return immutable per-currency server-bank snapshot list
     */
    public @NotNull List<ServerBankCurrencySnapshot> findServerBankEntries() {
        final RRServerBank serverBankRepository = this.plugin.getServerBankRepository();
        if (serverBankRepository == null) {
            return List.of();
        }

        final RServerBank serverBank = serverBankRepository.findGlobalBank();
        if (serverBank == null) {
            return List.of();
        }

        final Map<String, Double> balances = serverBank.getBalances();
        final Map<String, Integer> ledgerCounts = new java.util.HashMap<>();
        final Map<String, RServerBank.LedgerEntry> latestEntries = new java.util.HashMap<>();
        for (final RServerBank.LedgerEntry ledgerEntry : serverBank.getLedgerEntries()) {
            final String currencyId = normalizeCurrencyId(ledgerEntry.currencyType());
            ledgerCounts.merge(currencyId, 1, Integer::sum);

            final RServerBank.LedgerEntry latest = latestEntries.get(currencyId);
            if (latest == null || ledgerEntry.recordedAtEpochMilli() >= latest.recordedAtEpochMilli()) {
                latestEntries.put(currencyId, ledgerEntry);
            }
        }

        final Set<String> currencies = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        currencies.addAll(balances.keySet());
        currencies.addAll(ledgerCounts.keySet());
        if (currencies.isEmpty()) {
            return List.of();
        }

        final List<ServerBankCurrencySnapshot> snapshots = new java.util.ArrayList<>(currencies.size());
        for (final String currencyId : currencies) {
            snapshots.add(new ServerBankCurrencySnapshot(
                currencyId,
                balances.getOrDefault(currencyId, 0.0D),
                ledgerCounts.getOrDefault(currencyId, 0),
                latestEntries.get(currencyId)
            ));
        }
        return List.copyOf(snapshots);
    }

    /**
     * Withdraws one currency balance from the server bank and credits it to an admin player.
     *
     * @param actor player receiving withdrawn balance
     * @param currencyId currency identifier to withdraw
     * @return async amount successfully withdrawn and credited
     * @throws NullPointerException if any argument is {@code null}
     */
    public @NotNull CompletableFuture<Double> withdrawServerBankToPlayer(
        final @NotNull Player actor,
        final @NotNull String currencyId
    ) {
        final Player validatedActor = Objects.requireNonNull(actor, "actor cannot be null");
        final String normalizedCurrencyId = normalizeCurrencyId(currencyId);
        final RRServerBank serverBankRepository = this.plugin.getServerBankRepository();
        if (serverBankRepository == null || normalizedCurrencyId.isBlank()) {
            return CompletableFuture.completedFuture(0.0D);
        }

        return this.supplyAsync(() -> {
            final double withdrawnAmount = serverBankRepository.withdrawAll(
                normalizedCurrencyId,
                validatedActor.getUniqueId(),
                null,
                SERVER_BANK_ADMIN_WITHDRAW_NOTE
            );
            if (withdrawnAmount <= EPSILON) {
                return 0.0D;
            }

            if (this.depositCurrency(validatedActor, normalizedCurrencyId, withdrawnAmount)) {
                return withdrawnAmount;
            }

            serverBankRepository.deposit(
                normalizedCurrencyId,
                withdrawnAmount,
                validatedActor.getUniqueId(),
                null,
                SERVER_BANK_ADMIN_WITHDRAW_ROLLBACK_NOTE
            );
            return 0.0D;
        });
    }

    /**
     * Resolves one session by trade UUID.
     *
     * @param tradeUuid trade UUID
     * @return matching session, or {@code null} when not found
     * @throws NullPointerException if {@code tradeUuid} is {@code null}
     */
    public @Nullable RTradeSession findSession(final @NotNull UUID tradeUuid) {
        final RRTradeSession sessionRepository = this.plugin.getTradeSessionRepository();
        if (sessionRepository == null) {
            return null;
        }
        return sessionRepository.findByTradeUuid(Objects.requireNonNull(tradeUuid, "tradeUuid cannot be null"));
    }

    /**
     * Returns invite target UUIDs using local online players plus persisted RDS player rows.
     *
     * @param viewerUuid viewer UUID to exclude from the result
     * @param limit maximum number of target UUIDs to return
     * @return immutable target UUID list
     * @throws NullPointerException if {@code viewerUuid} is {@code null}
     */
    public @NotNull List<UUID> findTradeTargetUuids(
        final @NotNull UUID viewerUuid,
        final int limit
    ) {
        final UUID validatedViewerUuid = Objects.requireNonNull(viewerUuid, "viewerUuid cannot be null");
        final int validatedLimit = Math.max(1, limit);
        final Set<UUID> targets = new LinkedHashSet<>();

        for (final Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            final UUID onlineUuid = onlinePlayer.getUniqueId();
            if (!onlineUuid.equals(validatedViewerUuid)) {
                targets.add(onlineUuid);
            }
        }

        final RRTradeSession sessionRepository = this.plugin.getTradeSessionRepository();
        if (sessionRepository != null && targets.size() < validatedLimit) {
            final int fetchSize = Math.max(validatedLimit * 4, validatedLimit);
            for (final UUID knownUuid : sessionRepository.findRdrPlayerUuids(fetchSize)) {
                if (knownUuid.equals(validatedViewerUuid)) {
                    continue;
                }
                targets.add(knownUuid);
                if (targets.size() >= validatedLimit) {
                    break;
                }
            }
        }
        return List.copyOf(targets);
    }

    /**
     * Returns trade-target snapshots with proxy-aware presence state and server metadata.
     *
     * @param viewerUuid viewer UUID to exclude from the result
     * @param limit maximum number of targets to return
     * @return immutable trade-target snapshot list
     * @throws NullPointerException if {@code viewerUuid} is {@code null}
     */
    public @NotNull List<TradeTargetSnapshot> findTradeTargets(
        final @NotNull UUID viewerUuid,
        final int limit
    ) {
        final List<UUID> targetUuids = this.findTradeTargetUuids(viewerUuid, limit);
        if (targetUuids.isEmpty()) {
            return List.of();
        }

        final Map<UUID, PlayerPresenceSnapshot> livePresenceSnapshots = this.findPresenceSnapshots(targetUuids);
        final List<TradeTargetSnapshot> snapshots = new java.util.ArrayList<>(targetUuids.size());
        for (final UUID targetUuid : targetUuids) {
            final String targetName = this.resolvePlayerName(targetUuid);
            final String serverId = this.resolveServerId(targetUuid, OFFLINE_SERVER_ID, livePresenceSnapshots);
            final PresenceState presenceState = this.resolvePresenceState(targetUuid, OFFLINE_SERVER_ID, livePresenceSnapshots);
            snapshots.add(new TradeTargetSnapshot(targetUuid, targetName, presenceState, serverId));
        }
        return List.copyOf(snapshots);
    }

    /**
     * Requests transfer of the actor to their trade partner's current server when available.
     *
     * @param actor requesting player
     * @param tradeUuid trade UUID
     * @return async join-partner routing response
     * @throws NullPointerException if any argument is {@code null}
     */
    public @NotNull CompletableFuture<JoinPartnerResponse> requestJoinPartnerServer(
        final @NotNull Player actor,
        final @NotNull UUID tradeUuid
    ) {
        final Player validatedActor = Objects.requireNonNull(actor, "actor cannot be null");
        final UUID validatedTradeUuid = Objects.requireNonNull(tradeUuid, "tradeUuid cannot be null");
        final RRTradeSession sessionRepository = this.plugin.getTradeSessionRepository();
        if (sessionRepository == null || !this.isTradeProxyJoinActionAvailable()) {
            return CompletableFuture.completedFuture(new JoinPartnerResponse(JoinPartnerResult.UNAVAILABLE, OFFLINE_SERVER_ID));
        }

        return this.supplyAsync(() -> sessionRepository.findByTradeUuid(validatedTradeUuid))
            .thenCompose(session -> {
                if (session == null) {
                    return CompletableFuture.completedFuture(
                        new JoinPartnerResponse(JoinPartnerResult.MISSING, OFFLINE_SERVER_ID)
                    );
                }
                if (!session.hasParticipant(validatedActor.getUniqueId())) {
                    return CompletableFuture.completedFuture(
                        new JoinPartnerResponse(JoinPartnerResult.FORBIDDEN, OFFLINE_SERVER_ID)
                    );
                }

                final UUID counterpartyUuid = session.getCounterpartyUuid(validatedActor.getUniqueId());
                if (counterpartyUuid == null) {
                    return CompletableFuture.completedFuture(
                        new JoinPartnerResponse(JoinPartnerResult.MISSING, OFFLINE_SERVER_ID)
                    );
                }

                final String fallbackServerId = session.getLastKnownServerIdForParticipant(counterpartyUuid);
                final String partnerServerId = this.resolveServerId(counterpartyUuid, fallbackServerId, Map.of());
                if (OFFLINE_SERVER_ID.equalsIgnoreCase(partnerServerId)) {
                    return CompletableFuture.completedFuture(
                        new JoinPartnerResponse(JoinPartnerResult.PARTNER_UNAVAILABLE, OFFLINE_SERVER_ID)
                    );
                }

                final String localServerRouteId = this.plugin.getServerRouteId();
                if (partnerServerId.equalsIgnoreCase(localServerRouteId)) {
                    return CompletableFuture.completedFuture(
                        new JoinPartnerResponse(JoinPartnerResult.ALREADY_ON_SERVER, partnerServerId)
                    );
                }

                final ProxyTransferRequest transferRequest = new ProxyTransferRequest(
                    validatedActor.getUniqueId(),
                    localServerRouteId,
                    partnerServerId,
                    "",
                    Map.of(
                        "module", MODULE_ID,
                        "action", JOIN_PARTNER_ACTION_ID,
                        "trade_uuid", validatedTradeUuid.toString(),
                        "partner_uuid", counterpartyUuid.toString()
                    )
                );

                return this.plugin.getProxyService().requestPlayerTransfer(transferRequest)
                    .handle((success, throwable) -> {
                        if (throwable != null || !Boolean.TRUE.equals(success)) {
                            return new JoinPartnerResponse(JoinPartnerResult.PARTNER_UNAVAILABLE, partnerServerId);
                        }
                        return new JoinPartnerResponse(JoinPartnerResult.ROUTING, partnerServerId);
                    });
            });
    }

    /**
     * Creates a new trade invite from initiator to partner.
     *
     * @param initiator invite initiator
     * @param partner invited partner
     * @return async invite result
     * @throws NullPointerException if any argument is {@code null}
     */
    public @NotNull CompletableFuture<InviteResult> createInvite(
        final @NotNull Player initiator,
        final @NotNull Player partner
    ) {
        final Player validatedInitiator = Objects.requireNonNull(initiator, "initiator cannot be null");
        final Player validatedPartner = Objects.requireNonNull(partner, "partner cannot be null");
        return this.createInvite(validatedInitiator, validatedPartner.getUniqueId());
    }

    /**
     * Creates a new trade invite from initiator to a partner UUID.
     *
     * @param initiator invite initiator
     * @param partnerUuid invited partner UUID
     * @return async invite result
     * @throws NullPointerException if any argument is {@code null}
     */
    public @NotNull CompletableFuture<InviteResult> createInvite(
        final @NotNull Player initiator,
        final @NotNull UUID partnerUuid
    ) {
        return this.createInviteWithSession(initiator, partnerUuid).thenApply(InviteCreateResponse::result);
    }

    /**
     * Creates a new trade invite from initiator to partner and returns the created trade UUID on success.
     *
     * @param initiator invite initiator
     * @param partner invited partner
     * @return async invite response including result and created trade UUID
     * @throws NullPointerException if any argument is {@code null}
     */
    public @NotNull CompletableFuture<InviteCreateResponse> createInviteWithSession(
        final @NotNull Player initiator,
        final @NotNull Player partner
    ) {
        final Player validatedInitiator = Objects.requireNonNull(initiator, "initiator cannot be null");
        final Player validatedPartner = Objects.requireNonNull(partner, "partner cannot be null");
        return this.createInviteWithSession(validatedInitiator, validatedPartner.getUniqueId());
    }

    /**
     * Creates a new trade invite from initiator to a partner UUID and returns the created trade UUID on success.
     *
     * @param initiator invite initiator
     * @param partnerUuid invited partner UUID
     * @return async invite response including result and created trade UUID
     * @throws NullPointerException if any argument is {@code null}
     */
    public @NotNull CompletableFuture<InviteCreateResponse> createInviteWithSession(
        final @NotNull Player initiator,
        final @NotNull UUID partnerUuid
    ) {
        final Player validatedInitiator = Objects.requireNonNull(initiator, "initiator cannot be null");
        final UUID validatedPartnerUuid = Objects.requireNonNull(partnerUuid, "partnerUuid cannot be null");
        final ConfigSection config = this.plugin.getDefaultConfig();
        if (!config.isTradeEnabled()) {
            return CompletableFuture.completedFuture(new InviteCreateResponse(InviteResult.DISABLED, null));
        }
        if (validatedInitiator.getUniqueId().equals(validatedPartnerUuid)) {
            return CompletableFuture.completedFuture(new InviteCreateResponse(InviteResult.SELF_TARGET, null));
        }

        final RRTradeSession sessionRepository = this.plugin.getTradeSessionRepository();
        if (sessionRepository == null) {
            return CompletableFuture.completedFuture(new InviteCreateResponse(InviteResult.UNAVAILABLE, null));
        }

        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime cooldownUntil = this.inviteCooldowns.get(validatedInitiator.getUniqueId());
        if (cooldownUntil != null && now.isBefore(cooldownUntil)) {
            return CompletableFuture.completedFuture(new InviteCreateResponse(InviteResult.COOLDOWN, null));
        }

        return sessionRepository.createInviteAsync(
                validatedInitiator.getUniqueId(),
                validatedPartnerUuid,
                now.plusSeconds(config.getTradeInviteTimeoutSeconds())
            )
            .thenCompose(result -> {
                if (result.status() != RRTradeSession.InviteCreateStatus.CREATED || result.tradeUuid() == null) {
                    if (result.status() == RRTradeSession.InviteCreateStatus.SELF_TRADE) {
                        return CompletableFuture.completedFuture(new InviteCreateResponse(InviteResult.SELF_TARGET, null));
                    }
                    return CompletableFuture.completedFuture(new InviteCreateResponse(InviteResult.PARTICIPANT_BUSY, null));
                }

                this.inviteCooldowns.put(
                    validatedInitiator.getUniqueId(),
                    LocalDateTime.now().plusSeconds(config.getTradeInviteCooldownSeconds())
                );
                return this.refreshParticipantServerSnapshots(
                    result.tradeUuid(),
                    validatedInitiator.getUniqueId(),
                    validatedPartnerUuid,
                    this.plugin.getServerRouteId(),
                    OFFLINE_SERVER_ID,
                    this.plugin.getServerRouteId()
                ).thenApply(ignored -> new InviteCreateResponse(InviteResult.SUCCESS, result.tradeUuid()));
            });
    }

    /**
     * Accepts a pending trade invite.
     *
     * @param actor accepting player
     * @param tradeUuid trade UUID
     * @param expectedRevision expected session revision, or negative to skip stale checks
     * @return async session result
     * @throws NullPointerException if any non-primitive argument is {@code null}
     */
    public @NotNull CompletableFuture<SessionResult> acceptInvite(
        final @NotNull Player actor,
        final @NotNull UUID tradeUuid,
        final long expectedRevision
    ) {
        final RRTradeSession sessionRepository = this.plugin.getTradeSessionRepository();
        if (sessionRepository == null) {
            return CompletableFuture.completedFuture(SessionResult.UNAVAILABLE);
        }

        final UUID validatedTradeUuid = Objects.requireNonNull(tradeUuid, "tradeUuid cannot be null");
        final UUID actorUuid = Objects.requireNonNull(actor, "actor cannot be null").getUniqueId();
        return this.runSessionMutation(() -> sessionRepository
            .acceptInviteAsync(
                validatedTradeUuid,
                actorUuid,
                expectedRevision,
                LocalDateTime.now()
            ))
            .thenCompose(result -> {
                if (result != SessionResult.INVALID_STATE) {
                    return CompletableFuture.completedFuture(result);
                }

                // Treat stale invite acceptance as idempotent success if the session already advanced.
                return this.supplyAsync(() -> sessionRepository.findByTradeUuid(validatedTradeUuid))
                    .thenApply(session -> {
                        if (session == null) {
                            return SessionResult.MISSING;
                        }
                        if (!session.hasParticipant(actorUuid)) {
                            return SessionResult.FORBIDDEN;
                        }
                        return switch (session.getStatus()) {
                            case ACTIVE, COMPLETING, COMPLETED -> SessionResult.SUCCESS;
                            case EXPIRED -> SessionResult.EXPIRED;
                            default -> SessionResult.INVALID_STATE;
                        };
                    });
            })
            .thenCompose(result ->
                this.refreshTradeSessionSnapshotsIfSuccessful(validatedTradeUuid, result)
                    .thenApply(ignored -> result)
            );
    }

    /**
     * Cancels an active or invited trade session.
     *
     * @param actor canceling player
     * @param tradeUuid trade UUID
     * @param expectedRevision expected session revision, or negative to skip stale checks
     * @return async session result
     * @throws NullPointerException if any non-primitive argument is {@code null}
     */
    public @NotNull CompletableFuture<SessionResult> cancelSession(
        final @NotNull Player actor,
        final @NotNull UUID tradeUuid,
        final long expectedRevision
    ) {
        return this.runSessionMutation(() -> this.plugin.getTradeSessionRepository()
            .cancelSessionAsync(
                Objects.requireNonNull(tradeUuid, "tradeUuid cannot be null"),
                Objects.requireNonNull(actor, "actor cannot be null").getUniqueId(),
                expectedRevision
            ));
    }

    /**
     * Sets active-session ready state for one participant.
     *
     * @param actor requesting player
     * @param tradeUuid trade UUID
     * @param expectedRevision expected session revision, or negative to skip stale checks
     * @param ready replacement ready state
     * @return async session result
     * @throws NullPointerException if any non-primitive argument is {@code null}
     */
    public @NotNull CompletableFuture<SessionResult> setReady(
        final @NotNull Player actor,
        final @NotNull UUID tradeUuid,
        final long expectedRevision,
        final boolean ready
    ) {
        final RRTradeSession sessionRepository = this.plugin.getTradeSessionRepository();
        if (sessionRepository == null) {
            return CompletableFuture.completedFuture(SessionResult.UNAVAILABLE);
        }

        final UUID validatedTradeUuid = Objects.requireNonNull(tradeUuid, "tradeUuid cannot be null");
        final UUID actorUuid = Objects.requireNonNull(actor, "actor cannot be null").getUniqueId();
        return this.runSessionMutation(() -> sessionRepository
            .setActiveReadyAsync(
                validatedTradeUuid,
                actorUuid,
                expectedRevision,
                ready
            ))
            .thenCompose(result -> {
                if (result != SessionResult.INVALID_STATE || !ready) {
                    return CompletableFuture.completedFuture(result);
                }

                // Treat stale ACTIVE views as successful when the row has already advanced.
                return this.supplyAsync(() -> sessionRepository.findByTradeUuid(validatedTradeUuid))
                    .thenApply(session -> {
                        if (session == null) {
                            return SessionResult.MISSING;
                        }
                        if (!session.hasParticipant(actorUuid)) {
                            return SessionResult.FORBIDDEN;
                        }
                        return switch (session.getStatus()) {
                            case ACTIVE, COMPLETING -> SessionResult.SUCCESS;
                            case COMPLETED -> SessionResult.COMPLETED;
                            case EXPIRED -> SessionResult.EXPIRED;
                            default -> SessionResult.INVALID_STATE;
                        };
                    });
            })
            .thenCompose(result ->
                this.refreshTradeSessionSnapshotsIfSuccessful(validatedTradeUuid, result)
                    .thenApply(ignored -> result)
            );
    }

    /**
     * Confirms settlement during the completion phase.
     *
     * @param actor confirming player
     * @param tradeUuid trade UUID
     * @param expectedRevision expected session revision, or negative to skip stale checks
     * @return async session result
     * @throws NullPointerException if any non-primitive argument is {@code null}
     */
    public @NotNull CompletableFuture<SessionResult> confirmCompletion(
        final @NotNull Player actor,
        final @NotNull UUID tradeUuid,
        final long expectedRevision
    ) {
        final RRTradeSession sessionRepository = this.plugin.getTradeSessionRepository();
        if (sessionRepository == null) {
            return CompletableFuture.completedFuture(SessionResult.UNAVAILABLE);
        }

        final UUID validatedTradeUuid = Objects.requireNonNull(tradeUuid, "tradeUuid cannot be null");
        final UUID actorUuid = Objects.requireNonNull(actor, "actor cannot be null").getUniqueId();
        return this.supplyAsync(() -> sessionRepository.findByTradeUuid(validatedTradeUuid))
            .thenCompose(session -> {
                if (session == null) {
                    return CompletableFuture.completedFuture(SessionResult.MISSING);
                }
                if (!session.hasParticipant(actorUuid)) {
                    return CompletableFuture.completedFuture(SessionResult.FORBIDDEN);
                }
                if (session.getStatus() == TradeSessionStatus.COMPLETED) {
                    return CompletableFuture.completedFuture(SessionResult.COMPLETED);
                }
                if (session.getStatus() != TradeSessionStatus.COMPLETING) {
                    return CompletableFuture.completedFuture(SessionResult.INVALID_STATE);
                }

                final boolean partnerAlreadyConfirmed = session.isInitiator(actorUuid)
                    ? session.isPartnerReady()
                    : session.isInitiatorReady();
                if (!partnerAlreadyConfirmed) {
                    return this.runCompletionMutation(
                        sessionRepository,
                        validatedTradeUuid,
                        actorUuid,
                        expectedRevision
                    );
                }

                final TaxCollectionResult collectionResult = this.collectTradeTaxes(session);
                if (collectionResult.result() != SessionResult.SUCCESS) {
                    return CompletableFuture.completedFuture(collectionResult.result());
                }

                final long strictRevision = expectedRevision >= 0L ? expectedRevision : session.getRevision();
                return sessionRepository.confirmSettlementAsync(
                        validatedTradeUuid,
                        actorUuid,
                        strictRevision
                    )
                    .thenApply(result -> {
                        final SessionResult mappedResult = mapCompletionResult(result);
                        if (mappedResult != SessionResult.COMPLETED) {
                            this.refundCollectedTaxes(collectionResult.collectedCharges());
                            return mappedResult;
                        }

                        if (!this.depositCollectedTaxesToServerBank(
                            validatedTradeUuid,
                            collectionResult.collectedCharges()
                        )) {
                            this.refundCollectedTaxes(collectionResult.collectedCharges());
                            return SessionResult.COMPLETED;
                        }
                        return SessionResult.COMPLETED;
                    })
                    .thenCompose(result -> {
                        if (result != SessionResult.INVALID_STATE) {
                            return CompletableFuture.completedFuture(result);
                        }
                        return this.resolveCompletionInvalidState(sessionRepository, validatedTradeUuid, actorUuid);
                    });
            })
            .thenCompose(result -> {
                if (result != SessionResult.INVALID_STATE) {
                    return CompletableFuture.completedFuture(result);
                }
                return this.resolveCompletionInvalidState(sessionRepository, validatedTradeUuid, actorUuid);
            })
            .thenCompose(result ->
                this.refreshTradeSessionSnapshotsIfSuccessful(validatedTradeUuid, result)
                    .thenApply(ignored -> result)
            );
    }

    /**
     * Escrows the actor main-hand item into the next free offer slot.
     *
     * @param actor requesting player
     * @param tradeUuid trade UUID
     * @param expectedRevision expected session revision, or negative to skip stale checks
     * @return async session result
     * @throws NullPointerException if any non-primitive argument is {@code null}
     */
    public @NotNull CompletableFuture<SessionResult> addHeldItemOffer(
        final @NotNull Player actor,
        final @NotNull UUID tradeUuid,
        final long expectedRevision
    ) {
        final Player validatedActor = Objects.requireNonNull(actor, "actor cannot be null");
        final UUID validatedTradeUuid = Objects.requireNonNull(tradeUuid, "tradeUuid cannot be null");
        final RRTradeSession sessionRepository = this.plugin.getTradeSessionRepository();
        if (sessionRepository == null) {
            return CompletableFuture.completedFuture(SessionResult.UNAVAILABLE);
        }

        final ItemStack mainHandItem = validatedActor.getInventory().getItemInMainHand();
        if (mainHandItem == null || mainHandItem.isEmpty() || mainHandItem.getType().isAir()) {
            return CompletableFuture.completedFuture(SessionResult.NO_ITEM_IN_HAND);
        }
        final ItemStack offeredItem = mainHandItem.clone();
        validatedActor.getInventory().setItemInMainHand(null);

        return this.supplyAsync(() -> sessionRepository.findByTradeUuid(validatedTradeUuid))
            .thenCompose(session -> {
                if (session == null || !session.hasParticipant(validatedActor.getUniqueId())) {
                    this.restoreItemToActor(validatedActor, offeredItem, validatedTradeUuid);
                    return CompletableFuture.completedFuture(SessionResult.MISSING);
                }
                if (!canMutateOffers(session, validatedActor.getUniqueId())) {
                    this.restoreItemToActor(validatedActor, offeredItem, validatedTradeUuid);
                    return CompletableFuture.completedFuture(SessionResult.INVALID_STATE);
                }
                if (isItemOfferLocked(session, validatedActor.getUniqueId())) {
                    this.restoreItemToActor(validatedActor, offeredItem, validatedTradeUuid);
                    return CompletableFuture.completedFuture(SessionResult.INVALID_STATE);
                }

                final Map<Integer, ItemStack> offers = new java.util.HashMap<>(session.getOfferItemsForParticipant(validatedActor.getUniqueId()));
                final int maxSlots = this.plugin.getDefaultConfig().getTradeMaxOfferSlots();
                if (offers.size() >= maxSlots) {
                    this.restoreItemToActor(validatedActor, offeredItem, validatedTradeUuid);
                    return CompletableFuture.completedFuture(SessionResult.OFFER_FULL);
                }

                final int targetSlot = findFirstFreeOfferSlot(offers, maxSlots);
                if (targetSlot < 0) {
                    this.restoreItemToActor(validatedActor, offeredItem, validatedTradeUuid);
                    return CompletableFuture.completedFuture(SessionResult.OFFER_FULL);
                }
                offers.put(targetSlot, offeredItem);
                return sessionRepository.updateOfferItemsAsync(
                        validatedTradeUuid,
                        validatedActor.getUniqueId(),
                        expectedRevision,
                        offers
                    )
                    .thenApply(result -> {
                        final SessionResult mapped = mapMutationResult(result);
                        if (mapped != SessionResult.SUCCESS) {
                            this.restoreItemToActor(validatedActor, offeredItem, validatedTradeUuid);
                        }
                        return mapped;
                    });
            })
            .thenCompose(result ->
                this.refreshTradeSessionSnapshotsIfSuccessful(validatedTradeUuid, result)
                    .thenApply(ignored -> result)
            );
    }

    /**
     * Escrows an explicit item stack into one participant offer slot.
     *
     * @param actor requesting player
     * @param tradeUuid trade UUID
     * @param expectedRevision expected session revision, or negative to skip stale checks
     * @param offerSlot offer slot index to assign
     * @param offeredItem item stack to escrow in the target slot
     * @return async session result
     * @throws NullPointerException if any non-primitive argument is {@code null}
     */
    public @NotNull CompletableFuture<SessionResult> setItemOfferSlot(
        final @NotNull Player actor,
        final @NotNull UUID tradeUuid,
        final long expectedRevision,
        final int offerSlot,
        final @NotNull ItemStack offeredItem
    ) {
        final Player validatedActor = Objects.requireNonNull(actor, "actor cannot be null");
        final UUID validatedTradeUuid = Objects.requireNonNull(tradeUuid, "tradeUuid cannot be null");
        final ItemStack validatedItem = Objects.requireNonNull(offeredItem, "offeredItem cannot be null").clone();
        if (validatedItem.isEmpty() || validatedItem.getType().isAir()) {
            return CompletableFuture.completedFuture(SessionResult.NO_ITEM_IN_HAND);
        }

        final RRTradeSession sessionRepository = this.plugin.getTradeSessionRepository();
        if (sessionRepository == null) {
            return CompletableFuture.completedFuture(SessionResult.UNAVAILABLE);
        }

        return this.supplyAsync(() -> sessionRepository.findByTradeUuid(validatedTradeUuid))
            .thenCompose(session -> {
                if (session == null || !session.hasParticipant(validatedActor.getUniqueId())) {
                    return CompletableFuture.completedFuture(SessionResult.MISSING);
                }
                if (!canMutateOffers(session, validatedActor.getUniqueId())) {
                    return CompletableFuture.completedFuture(SessionResult.INVALID_STATE);
                }
                if (isItemOfferLocked(session, validatedActor.getUniqueId())) {
                    return CompletableFuture.completedFuture(SessionResult.INVALID_STATE);
                }
                final int maxSlots = this.plugin.getDefaultConfig().getTradeMaxOfferSlots();
                if (offerSlot < 0 || offerSlot >= maxSlots) {
                    return CompletableFuture.completedFuture(SessionResult.OFFER_FULL);
                }

                final Map<Integer, ItemStack> offers = new java.util.HashMap<>(session.getOfferItemsForParticipant(validatedActor.getUniqueId()));
                final ItemStack existing = offers.get(offerSlot);
                if (existing != null && !existing.isEmpty()) {
                    return CompletableFuture.completedFuture(SessionResult.INVALID_STATE);
                }
                offers.put(offerSlot, validatedItem.clone());

                return sessionRepository.updateOfferItemsAsync(
                        validatedTradeUuid,
                        validatedActor.getUniqueId(),
                        expectedRevision,
                        offers
                    )
                    .thenApply(TradeService::mapMutationResult);
            })
            .thenCompose(result ->
                this.refreshTradeSessionSnapshotsIfSuccessful(validatedTradeUuid, result)
                    .thenApply(ignored -> result)
            );
    }

    /**
     * Removes one offered item slot from escrow and returns it to the actor.
     *
     * @param actor requesting player
     * @param tradeUuid trade UUID
     * @param expectedRevision expected session revision, or negative to skip stale checks
     * @param offerSlot offer slot index to remove
     * @return async session result
     * @throws NullPointerException if any non-primitive argument is {@code null}
     */
    public @NotNull CompletableFuture<SessionResult> removeItemOfferSlot(
        final @NotNull Player actor,
        final @NotNull UUID tradeUuid,
        final long expectedRevision,
        final int offerSlot
    ) {
        final Player validatedActor = Objects.requireNonNull(actor, "actor cannot be null");
        final UUID validatedTradeUuid = Objects.requireNonNull(tradeUuid, "tradeUuid cannot be null");
        final RRTradeSession sessionRepository = this.plugin.getTradeSessionRepository();
        if (sessionRepository == null) {
            return CompletableFuture.completedFuture(SessionResult.UNAVAILABLE);
        }

        return this.supplyAsync(() -> sessionRepository.findByTradeUuid(validatedTradeUuid))
            .thenCompose(session -> {
                if (session == null || !session.hasParticipant(validatedActor.getUniqueId())) {
                    return CompletableFuture.completedFuture(SessionResult.MISSING);
                }
                if (!canMutateOffers(session, validatedActor.getUniqueId())) {
                    return CompletableFuture.completedFuture(SessionResult.INVALID_STATE);
                }
                if (isItemOfferLocked(session, validatedActor.getUniqueId())) {
                    return CompletableFuture.completedFuture(SessionResult.INVALID_STATE);
                }

                final Map<Integer, ItemStack> offers = new java.util.HashMap<>(session.getOfferItemsForParticipant(validatedActor.getUniqueId()));
                final ItemStack removedItem = offers.remove(offerSlot);
                if (removedItem == null || removedItem.isEmpty()) {
                    return CompletableFuture.completedFuture(SessionResult.MISSING);
                }

                return sessionRepository.updateOfferItemsAsync(
                        validatedTradeUuid,
                        validatedActor.getUniqueId(),
                        expectedRevision,
                        offers
                    )
                    .thenApply(result -> {
                        final SessionResult mapped = mapMutationResult(result);
                        if (mapped == SessionResult.SUCCESS) {
                            this.restoreItemToActor(validatedActor, removedItem, validatedTradeUuid);
                        }
                        return mapped;
                    });
            })
            .thenCompose(result ->
                this.refreshTradeSessionSnapshotsIfSuccessful(validatedTradeUuid, result)
                    .thenApply(ignored -> result)
            );
    }

    /**
     * Adjusts one escrowed currency offer by an explicit amount.
     *
     * <p>Left/add operations withdraw funds from the actor first. Right/remove operations refund the
     * applied delta and clamp the resulting offer to a floor of {@code 0}.</p>
     *
     * @param actor requesting player
     * @param tradeUuid trade UUID
     * @param expectedRevision expected session revision, or negative to skip stale checks
     * @param currencyId currency identifier to mutate
     * @param amount amount to add/remove (must be non-negative)
     * @param increase {@code true} to add, {@code false} to remove
     * @return async session result
     * @throws NullPointerException if any non-primitive argument is {@code null}
     */
    public @NotNull CompletableFuture<SessionResult> adjustCurrencyOffer(
        final @NotNull Player actor,
        final @NotNull UUID tradeUuid,
        final long expectedRevision,
        final @NotNull String currencyId,
        final double amount,
        final boolean increase
    ) {
        final Player validatedActor = Objects.requireNonNull(actor, "actor cannot be null");
        final UUID validatedTradeUuid = Objects.requireNonNull(tradeUuid, "tradeUuid cannot be null");
        final String normalizedCurrencyId = normalizeCurrencyId(currencyId);
        final double normalizedAmount = Math.max(0.0D, amount);
        if (normalizedCurrencyId.isBlank()) {
            return CompletableFuture.completedFuture(SessionResult.INVALID_STATE);
        }
        if (normalizedAmount <= EPSILON) {
            return CompletableFuture.completedFuture(SessionResult.SUCCESS);
        }
        if (!this.isCurrencyAvailableForTrading(normalizedCurrencyId)) {
            return CompletableFuture.completedFuture(SessionResult.UNAVAILABLE);
        }

        final RRTradeSession sessionRepository = this.plugin.getTradeSessionRepository();
        if (sessionRepository == null) {
            return CompletableFuture.completedFuture(SessionResult.UNAVAILABLE);
        }

        if (increase && !this.withdrawCurrency(validatedActor, normalizedCurrencyId, normalizedAmount)) {
            return CompletableFuture.completedFuture(SessionResult.INSUFFICIENT_FUNDS);
        }

        return this.supplyAsync(() -> sessionRepository.findByTradeUuid(validatedTradeUuid))
            .thenCompose(session -> {
                if (session == null || !session.hasParticipant(validatedActor.getUniqueId())) {
                    if (increase) {
                        this.depositCurrency(validatedActor, normalizedCurrencyId, normalizedAmount);
                    }
                    return CompletableFuture.completedFuture(SessionResult.MISSING);
                }
                if (!canMutateOffers(session, validatedActor.getUniqueId())) {
                    if (increase) {
                        this.depositCurrency(validatedActor, normalizedCurrencyId, normalizedAmount);
                    }
                    return CompletableFuture.completedFuture(SessionResult.INVALID_STATE);
                }

                final Map<String, Double> offers = new java.util.LinkedHashMap<>(
                    session.getOfferCurrencyForParticipant(validatedActor.getUniqueId())
                );
                final double currentAmount = offers.getOrDefault(normalizedCurrencyId, 0.0D);
                final double targetAmount = increase
                    ? currentAmount + normalizedAmount
                    : Math.max(0.0D, currentAmount - normalizedAmount);
                final double actualDelta = targetAmount - currentAmount;
                if (!increase && Math.abs(actualDelta) <= EPSILON) {
                    return CompletableFuture.completedFuture(SessionResult.SUCCESS);
                }

                if (targetAmount <= EPSILON) {
                    offers.remove(normalizedCurrencyId);
                } else {
                    offers.put(normalizedCurrencyId, targetAmount);
                }

                return sessionRepository.updateOfferCurrencyAsync(
                        validatedTradeUuid,
                        validatedActor.getUniqueId(),
                        expectedRevision,
                        offers
                    )
                    .thenApply(result -> {
                        final SessionResult mapped = mapMutationResult(result);
                        if (mapped != SessionResult.SUCCESS) {
                            if (increase) {
                                this.depositCurrency(validatedActor, normalizedCurrencyId, normalizedAmount);
                            }
                            return mapped;
                        }

                        if (!increase && actualDelta < -EPSILON) {
                            this.depositCurrency(validatedActor, normalizedCurrencyId, Math.abs(actualDelta));
                        }
                        return SessionResult.SUCCESS;
                    });
            })
            .thenCompose(result ->
                this.refreshTradeSessionSnapshotsIfSuccessful(validatedTradeUuid, result)
                    .thenApply(ignored -> result)
            );
    }

    /**
     * Adjusts escrowed Vault currency offer by one configured step.
     *
     * @param actor requesting player
     * @param tradeUuid trade UUID
     * @param expectedRevision expected session revision, or negative to skip stale checks
     * @param increase {@code true} to add one step, {@code false} to remove one step
     * @return async session result
     * @throws NullPointerException if any non-primitive argument is {@code null}
     */
    public @NotNull CompletableFuture<SessionResult> adjustVaultOffer(
        final @NotNull Player actor,
        final @NotNull UUID tradeUuid,
        final long expectedRevision,
        final boolean increase
    ) {
        return this.adjustCurrencyOffer(actor, tradeUuid, expectedRevision, VAULT_CURRENCY_ID, VAULT_STEP, increase);
    }

    /**
     * Claims one pending delivery row and applies payload to the actor.
     *
     * @param actor claiming player
     * @param deliveryId delivery row identifier
     * @return async claim result
     * @throws NullPointerException if any argument is {@code null}
     */
    public @NotNull CompletableFuture<SessionResult> claimDelivery(
        final @NotNull Player actor,
        final @NotNull Long deliveryId
    ) {
        final Player validatedActor = Objects.requireNonNull(actor, "actor cannot be null");
        final Long validatedDeliveryId = Objects.requireNonNull(deliveryId, "deliveryId cannot be null");
        final RRTradeDelivery deliveryRepository = this.plugin.getTradeDeliveryRepository();
        if (deliveryRepository == null) {
            return CompletableFuture.completedFuture(SessionResult.UNAVAILABLE);
        }

        return deliveryRepository.claimDeliveryAsync(validatedDeliveryId, validatedActor.getUniqueId())
            .thenApply(claimResult -> {
                if (claimResult.status() == RRTradeDelivery.ClaimStatus.MISSING) {
                    return SessionResult.MISSING;
                }
                if (claimResult.status() == RRTradeDelivery.ClaimStatus.FORBIDDEN) {
                    return SessionResult.FORBIDDEN;
                }
                if (claimResult.status() == RRTradeDelivery.ClaimStatus.ALREADY_CLAIMED) {
                    return SessionResult.INVALID_STATE;
                }

                this.applyClaimPayload(validatedActor, claimResult);
                return SessionResult.SUCCESS;
            });
    }

    /**
     * Expires overdue invite rows and returns the amount of affected rows.
     *
     * @return async expired-invite count
     */
    public @NotNull CompletableFuture<Integer> expireInvites() {
        final RRTradeSession sessionRepository = this.plugin.getTradeSessionRepository();
        if (sessionRepository == null) {
            return CompletableFuture.completedFuture(0);
        }
        return sessionRepository.expireInvitesAsync(LocalDateTime.now());
    }

    /**
     * Formats a currency amount for trade UI output.
     *
     * @param currencyId currency identifier
     * @param amount amount to format
     * @return formatted currency amount
     * @throws NullPointerException if {@code currencyId} is {@code null}
     */
    public @NotNull String formatCurrency(final @NotNull String currencyId, final double amount) {
        final String validatedCurrencyId = normalizeCurrencyId(currencyId);
        if (VAULT_CURRENCY_ID.equals(validatedCurrencyId)) {
            return this.plugin.formatVaultCurrency(amount);
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge == null) {
            return String.format(Locale.US, "%.2f %s", amount, validatedCurrencyId);
        }
        return String.format(Locale.US, "%.2f %s", amount, bridge.getCurrencyDisplayName(validatedCurrencyId));
    }

    /**
     * Resolves the display name for one currency identifier.
     *
     * @param currencyId currency identifier
     * @return display name for UI output
     * @throws NullPointerException if {@code currencyId} is {@code null}
     */
    public @NotNull String getCurrencyDisplayName(final @NotNull String currencyId) {
        final String validatedCurrencyId = normalizeCurrencyId(currencyId);
        if (VAULT_CURRENCY_ID.equals(validatedCurrencyId)) {
            return "Vault";
        }
        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge == null) {
            return validatedCurrencyId;
        }
        return bridge.getCurrencyDisplayName(validatedCurrencyId);
    }

    /**
     * Returns currently available currency identifiers for trade offers.
     *
     * @return immutable map of currency identifiers to display names
     */
    public @NotNull Map<String, String> getAvailableTradeCurrencies() {
        final Map<String, String> currencies = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (this.plugin.hasVaultEconomy()) {
            currencies.put(VAULT_CURRENCY_ID, "Vault");
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        if (bridge != null) {
            for (final String currencyId : this.findJExCurrencyIdentifiers()) {
                final String normalizedCurrencyId = normalizeCurrencyId(currencyId);
                if (normalizedCurrencyId.isBlank() || VAULT_CURRENCY_ID.equals(normalizedCurrencyId)) {
                    continue;
                }
                final String displayName = bridge.getCurrencyDisplayName(normalizedCurrencyId);
                currencies.put(normalizedCurrencyId, displayName);
            }
        }
        return Map.copyOf(currencies);
    }

    /**
     * Returns remaining invite cooldown seconds for one initiator.
     *
     * @param initiatorUuid initiator UUID
     * @return remaining cooldown seconds, or {@code 0} when no cooldown remains
     * @throws NullPointerException if {@code initiatorUuid} is {@code null}
     */
    public long getInviteCooldownRemainingSeconds(final @NotNull UUID initiatorUuid) {
        final LocalDateTime cooldownUntil = this.inviteCooldowns.get(Objects.requireNonNull(initiatorUuid, "initiatorUuid cannot be null"));
        if (cooldownUntil == null) {
            return 0L;
        }

        final long remaining = Duration.between(LocalDateTime.now(), cooldownUntil).getSeconds();
        return Math.max(0L, remaining);
    }

    /**
     * Returns whether trade target discovery should query proxy presence snapshots.
     *
     * @return {@code true} when trade proxy presence is enabled and available
     */
    public boolean isTradeProxyPresenceAvailable() {
        return this.plugin.getDefaultConfig().isTradeProxyPresenceEnabled()
            && this.plugin.getProxyService().isAvailable();
    }

    /**
     * Returns whether join-partner actions are enabled for trade UIs.
     *
     * @return {@code true} when join-partner actions are enabled and available
     */
    public boolean isTradeProxyJoinActionAvailable() {
        return this.plugin.getDefaultConfig().isTradeProxyJoinActionEnabled()
            && this.plugin.getProxyService().isAvailable();
    }

    /**
     * Returns presence snapshots for participant UUIDs when proxy presence is available.
     *
     * @param participantUuids participant UUID collection
     * @return immutable presence snapshot map
     * @throws NullPointerException if {@code participantUuids} is {@code null}
     */
    public @NotNull Map<UUID, PlayerPresenceSnapshot> findPresenceSnapshots(
        final @NotNull Collection<UUID> participantUuids
    ) {
        final List<UUID> participantSnapshot = List.copyOf(
            Objects.requireNonNull(participantUuids, "participantUuids cannot be null")
        );
        if (participantSnapshot.isEmpty() || !this.isTradeProxyPresenceAvailable()) {
            return Map.of();
        }

        try {
            return this.plugin.getProxyService().findPresence(participantSnapshot)
                .completeOnTimeout(Map.of(), PROXY_PRESENCE_LOOKUP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .exceptionally(throwable -> Map.of())
                .join();
        } catch (RuntimeException ignored) {
            return Map.of();
        }
    }

    /**
     * Resolves one participant server identifier from live proxy presence or fallback metadata.
     *
     * @param participantUuid participant UUID
     * @param fallbackServerId fallback server identifier
     * @param livePresenceSnapshots pre-fetched live presence snapshots
     * @return resolved participant server identifier
     * @throws NullPointerException if any non-nullable argument is {@code null}
     */
    public @NotNull String resolveServerId(
        final @NotNull UUID participantUuid,
        final @Nullable String fallbackServerId,
        final @NotNull Map<UUID, PlayerPresenceSnapshot> livePresenceSnapshots
    ) {
        final UUID validatedParticipantUuid = Objects.requireNonNull(participantUuid, "participantUuid cannot be null");
        final Map<UUID, PlayerPresenceSnapshot> snapshots = Objects.requireNonNull(
            livePresenceSnapshots,
            "livePresenceSnapshots cannot be null"
        );
        final String localServerRouteId = this.plugin.getServerRouteId();

        final Player onlinePlayer = Bukkit.getPlayer(validatedParticipantUuid);
        if (onlinePlayer != null && onlinePlayer.isOnline()) {
            return localServerRouteId;
        }

        final PlayerPresenceSnapshot snapshot = snapshots.get(validatedParticipantUuid);
        if (snapshot != null && snapshot.online()) {
            return normalizeServerId(snapshot.serverId(), OFFLINE_SERVER_ID);
        }
        if (!snapshots.isEmpty()) {
            return normalizeServerId(fallbackServerId, OFFLINE_SERVER_ID);
        }

        final Optional<PlayerPresenceSnapshot> liveSnapshot = this.findSinglePresenceSnapshot(validatedParticipantUuid);
        if (liveSnapshot.isPresent() && liveSnapshot.get().online()) {
            return normalizeServerId(liveSnapshot.get().serverId(), OFFLINE_SERVER_ID);
        }

        return normalizeServerId(fallbackServerId, OFFLINE_SERVER_ID);
    }

    /**
     * Resolves one participant presence state from live presence and fallback metadata.
     *
     * @param participantUuid participant UUID
     * @param fallbackServerId fallback server identifier
     * @param livePresenceSnapshots pre-fetched live presence snapshots
     * @return resolved participant presence state
     * @throws NullPointerException if any non-nullable argument is {@code null}
     */
    public @NotNull PresenceState resolvePresenceState(
        final @NotNull UUID participantUuid,
        final @Nullable String fallbackServerId,
        final @NotNull Map<UUID, PlayerPresenceSnapshot> livePresenceSnapshots
    ) {
        final String resolvedServerId = this.resolveServerId(participantUuid, fallbackServerId, livePresenceSnapshots);
        if (OFFLINE_SERVER_ID.equalsIgnoreCase(resolvedServerId)) {
            return PresenceState.OFFLINE;
        }
        return resolvedServerId.equalsIgnoreCase(this.plugin.getServerRouteId())
            ? PresenceState.LOCAL_ONLINE
            : PresenceState.REMOTE_ONLINE;
    }

    private @NotNull CompletableFuture<Void> refreshTradeSessionSnapshotsIfSuccessful(
        final @NotNull UUID tradeUuid,
        final @NotNull SessionResult result
    ) {
        if (result != SessionResult.SUCCESS
            && result != SessionResult.WAITING_FOR_PARTNER
            && result != SessionResult.COMPLETED) {
            return CompletableFuture.completedFuture(null);
        }

        final RRTradeSession sessionRepository = this.plugin.getTradeSessionRepository();
        if (sessionRepository == null) {
            return CompletableFuture.completedFuture(null);
        }

        final UUID validatedTradeUuid = Objects.requireNonNull(tradeUuid, "tradeUuid cannot be null");
        return this.supplyAsync(() -> sessionRepository.findByTradeUuid(validatedTradeUuid))
            .thenCompose(session -> {
                if (session == null) {
                    return CompletableFuture.completedFuture(null);
                }
                return this.refreshParticipantServerSnapshots(
                    validatedTradeUuid,
                    session.getInitiatorUuid(),
                    session.getPartnerUuid(),
                    session.getInitiatorLastKnownServerId(),
                    session.getPartnerLastKnownServerId(),
                    null
                );
            });
    }

    private @NotNull CompletableFuture<Void> refreshParticipantServerSnapshots(
        final @NotNull UUID tradeUuid,
        final @NotNull UUID initiatorUuid,
        final @NotNull UUID partnerUuid,
        final @Nullable String initiatorFallbackServerId,
        final @Nullable String partnerFallbackServerId,
        final @Nullable String originServerId
    ) {
        final RRTradeSession sessionRepository = this.plugin.getTradeSessionRepository();
        if (sessionRepository == null) {
            return CompletableFuture.completedFuture(null);
        }

        final Map<UUID, PlayerPresenceSnapshot> livePresenceSnapshots = this.findPresenceSnapshots(
            List.of(initiatorUuid, partnerUuid)
        );
        final String initiatorServerId = this.resolveServerId(
            initiatorUuid,
            initiatorFallbackServerId,
            livePresenceSnapshots
        );
        final String partnerServerId = this.resolveServerId(
            partnerUuid,
            partnerFallbackServerId,
            livePresenceSnapshots
        );
        final String normalizedOriginServerId = originServerId == null || originServerId.isBlank()
            ? null
            : originServerId.trim();

        return sessionRepository.refreshParticipantServerSnapshotsAsync(
                tradeUuid,
                initiatorServerId,
                partnerServerId,
                normalizedOriginServerId
            )
            .handle((ignored, throwable) -> null);
    }

    private @NotNull Optional<PlayerPresenceSnapshot> findSinglePresenceSnapshot(final @NotNull UUID participantUuid) {
        if (!this.isTradeProxyPresenceAvailable()) {
            return Optional.empty();
        }
        try {
            return this.plugin.getProxyService().findPresence(participantUuid)
                .completeOnTimeout(Optional.empty(), PROXY_PRESENCE_LOOKUP_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                .exceptionally(throwable -> Optional.empty())
                .join();
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private @NotNull String resolvePlayerName(final @NotNull UUID playerUuid) {
        final Player onlinePlayer = Bukkit.getPlayer(playerUuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }
        final var offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
        return offlinePlayer.getName() == null ? playerUuid.toString() : offlinePlayer.getName();
    }

    private boolean isCurrencyAvailableForTrading(final @NotNull String currencyId) {
        final String normalizedCurrencyId = normalizeCurrencyId(currencyId);
        if (normalizedCurrencyId.isBlank()) {
            return false;
        }
        if (VAULT_CURRENCY_ID.equals(normalizedCurrencyId)) {
            return this.plugin.hasVaultEconomy();
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        return bridge != null && bridge.hasCurrency(normalizedCurrencyId);
    }

    private boolean hasCurrencyBalance(
        final @NotNull OfflinePlayer player,
        final @NotNull String currencyId,
        final double amount
    ) {
        if (amount <= EPSILON) {
            return true;
        }

        final String normalizedCurrencyId = normalizeCurrencyId(currencyId);
        if (VAULT_CURRENCY_ID.equals(normalizedCurrencyId)) {
            return this.plugin.hasVaultFunds(player, amount);
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        return bridge != null
            && bridge.hasCurrency(normalizedCurrencyId)
            && bridge.has(player, normalizedCurrencyId, amount);
    }

    private boolean withdrawCurrency(
        final @NotNull Player player,
        final @NotNull String currencyId,
        final double amount
    ) {
        return this.withdrawCurrency((OfflinePlayer) player, currencyId, amount);
    }

    private boolean withdrawCurrency(
        final @NotNull OfflinePlayer player,
        final @NotNull String currencyId,
        final double amount
    ) {
        if (amount <= EPSILON) {
            return true;
        }

        final String normalizedCurrencyId = normalizeCurrencyId(currencyId);
        if (VAULT_CURRENCY_ID.equals(normalizedCurrencyId)) {
            return this.plugin.withdrawVault(player, amount);
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        return bridge != null
            && bridge.hasCurrency(normalizedCurrencyId)
            && safeJoin(bridge.withdraw(player, normalizedCurrencyId, amount));
    }

    private boolean depositCurrency(
        final @NotNull Player player,
        final @NotNull String currencyId,
        final double amount
    ) {
        return this.depositCurrency((OfflinePlayer) player, currencyId, amount);
    }

    private boolean depositCurrency(
        final @NotNull OfflinePlayer player,
        final @NotNull String currencyId,
        final double amount
    ) {
        if (amount <= EPSILON) {
            return true;
        }

        final String normalizedCurrencyId = normalizeCurrencyId(currencyId);
        if (VAULT_CURRENCY_ID.equals(normalizedCurrencyId)) {
            return this.plugin.depositVault(player, amount);
        }

        final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
        return bridge != null
            && bridge.hasCurrency(normalizedCurrencyId)
            && safeJoin(bridge.deposit(player, normalizedCurrencyId, amount));
    }

    private @NotNull TaxCollectionResult collectTradeTaxes(final @NotNull RTradeSession session) {
        final List<TaxCharge> taxCharges = this.resolveTradeTaxCharges(session);
        if (taxCharges.isEmpty()) {
            return new TaxCollectionResult(SessionResult.SUCCESS, List.of());
        }
        if (this.plugin.getServerBankRepository() == null) {
            return new TaxCollectionResult(SessionResult.UNAVAILABLE, List.of());
        }

        for (final TaxCharge taxCharge : taxCharges) {
            if (!this.isCurrencyAvailableForTrading(taxCharge.currencyId())) {
                return new TaxCollectionResult(SessionResult.UNAVAILABLE, List.of());
            }
        }

        for (final TaxCharge taxCharge : taxCharges) {
            final OfflinePlayer payer = Bukkit.getOfflinePlayer(taxCharge.payerUuid());
            if (!this.hasCurrencyBalance(payer, taxCharge.currencyId(), taxCharge.amount())) {
                return new TaxCollectionResult(SessionResult.INSUFFICIENT_FUNDS, List.of());
            }
        }

        final List<TaxCharge> collectedCharges = new java.util.ArrayList<>();
        for (final TaxCharge taxCharge : taxCharges) {
            final OfflinePlayer payer = Bukkit.getOfflinePlayer(taxCharge.payerUuid());
            if (!this.withdrawCurrency(payer, taxCharge.currencyId(), taxCharge.amount())) {
                this.refundCollectedTaxes(collectedCharges);
                return new TaxCollectionResult(SessionResult.INSUFFICIENT_FUNDS, List.of());
            }
            collectedCharges.add(taxCharge);
        }
        return new TaxCollectionResult(SessionResult.SUCCESS, List.copyOf(collectedCharges));
    }

    private @NotNull List<TaxCharge> resolveTradeTaxCharges(final @NotNull RTradeSession session) {
        final ConfigSection configuration = this.plugin.getDefaultConfig();
        if (!configuration.isTradeTaxationEnabled()) {
            return List.of();
        }

        final int initiatorItemCount = countOfferedItems(session.getInitiatorOfferItems());
        final int partnerItemCount = countOfferedItems(session.getPartnerOfferItems());
        final Map<String, Double> initiatorCurrency = session.getInitiatorOfferCurrency();
        final Map<String, Double> partnerCurrency = session.getPartnerOfferCurrency();

        final List<TaxCharge> charges = new java.util.ArrayList<>();
        for (final Map.Entry<String, ConfigSection.TradeTaxCurrencyDefinition> entry
            : configuration.getTradeTaxationCurrencies().entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }

            final String currencyId = normalizeCurrencyId(entry.getKey());
            final ConfigSection.TradeTaxCurrencyDefinition definition = entry.getValue();
            final double initiatorOfferAmount = Math.max(0.0D, initiatorCurrency.getOrDefault(currencyId, 0.0D));
            final double partnerOfferAmount = Math.max(0.0D, partnerCurrency.getOrDefault(currencyId, 0.0D));
            final double initiatorTax = definition.calculateTax(initiatorOfferAmount, initiatorItemCount);
            final double partnerTax = definition.calculateTax(partnerOfferAmount, partnerItemCount);

            if (initiatorTax > EPSILON) {
                charges.add(new TaxCharge(session.getInitiatorUuid(), currencyId, initiatorTax));
            }
            if (partnerTax > EPSILON) {
                charges.add(new TaxCharge(session.getPartnerUuid(), currencyId, partnerTax));
            }
        }
        return List.copyOf(charges);
    }

    private boolean depositCollectedTaxesToServerBank(
        final @NotNull UUID tradeUuid,
        final @NotNull List<TaxCharge> collectedCharges
    ) {
        if (collectedCharges.isEmpty()) {
            return true;
        }

        final RRServerBank serverBankRepository = this.plugin.getServerBankRepository();
        if (serverBankRepository == null) {
            return false;
        }

        final List<TaxCharge> depositedCharges = new java.util.ArrayList<>();
        for (final TaxCharge taxCharge : collectedCharges) {
            try {
                serverBankRepository.deposit(
                    taxCharge.currencyId(),
                    taxCharge.amount(),
                    taxCharge.payerUuid(),
                    tradeUuid,
                    TRADE_TAX_LEDGER_NOTE
                );
                depositedCharges.add(taxCharge);
            } catch (RuntimeException exception) {
                for (final TaxCharge rollbackCharge : depositedCharges) {
                    serverBankRepository.withdraw(
                        rollbackCharge.currencyId(),
                        rollbackCharge.amount(),
                        rollbackCharge.payerUuid(),
                        tradeUuid,
                        TRADE_TAX_ROLLBACK_NOTE
                    );
                }
                return false;
            }
        }
        return true;
    }

    private void refundCollectedTaxes(final @NotNull List<TaxCharge> collectedCharges) {
        for (final TaxCharge taxCharge : collectedCharges) {
            final OfflinePlayer payer = Bukkit.getOfflinePlayer(taxCharge.payerUuid());
            this.depositCurrency(payer, taxCharge.currencyId(), taxCharge.amount());
        }
    }

    private @NotNull CompletableFuture<SessionResult> runCompletionMutation(
        final @NotNull RRTradeSession sessionRepository,
        final @NotNull UUID tradeUuid,
        final @NotNull UUID actorUuid,
        final long expectedRevision
    ) {
        return sessionRepository.confirmSettlementAsync(tradeUuid, actorUuid, expectedRevision)
            .thenCompose(result -> {
                if (result != RRTradeSession.CompletionResult.INVALID_STATE) {
                    return CompletableFuture.completedFuture(mapCompletionResult(result));
                }
                return this.resolveCompletionInvalidState(sessionRepository, tradeUuid, actorUuid);
            });
    }

    private @NotNull CompletableFuture<SessionResult> resolveCompletionInvalidState(
        final @NotNull RRTradeSession sessionRepository,
        final @NotNull UUID tradeUuid,
        final @NotNull UUID actorUuid
    ) {
        return this.supplyAsync(() -> sessionRepository.findByTradeUuid(tradeUuid))
            .thenApply(session -> {
                if (session == null) {
                    return SessionResult.MISSING;
                }
                if (!session.hasParticipant(actorUuid)) {
                    return SessionResult.FORBIDDEN;
                }
                if (session.getStatus() == TradeSessionStatus.COMPLETED) {
                    return SessionResult.COMPLETED;
                }
                if (session.getStatus() == TradeSessionStatus.COMPLETING && isParticipantReady(session, actorUuid)) {
                    return SessionResult.WAITING_FOR_PARTNER;
                }
                if (session.getStatus() == TradeSessionStatus.EXPIRED) {
                    return SessionResult.EXPIRED;
                }
                return SessionResult.INVALID_STATE;
            });
    }

    private @NotNull CompletableFuture<SessionResult> runSessionMutation(
        final @NotNull Supplier<CompletableFuture<RRTradeSession.MutationResult>> mutationSupplier
    ) {
        final RRTradeSession sessionRepository = this.plugin.getTradeSessionRepository();
        if (sessionRepository == null) {
            return CompletableFuture.completedFuture(SessionResult.UNAVAILABLE);
        }
        return mutationSupplier.get().thenApply(TradeService::mapMutationResult);
    }

    private static @NotNull SessionResult mapMutationResult(final @NotNull RRTradeSession.MutationResult result) {
        return switch (result) {
            case SUCCESS -> SessionResult.SUCCESS;
            case MISSING -> SessionResult.MISSING;
            case FORBIDDEN -> SessionResult.FORBIDDEN;
            case INVALID_STATE -> SessionResult.INVALID_STATE;
            case STALE_REVISION -> SessionResult.STALE;
            case EXPIRED -> SessionResult.EXPIRED;
        };
    }

    private static @NotNull SessionResult mapCompletionResult(
        final @NotNull RRTradeSession.CompletionResult result
    ) {
        return switch (result) {
            case COMPLETED -> SessionResult.COMPLETED;
            case WAITING_FOR_PARTNER -> SessionResult.WAITING_FOR_PARTNER;
            case MISSING -> SessionResult.MISSING;
            case FORBIDDEN -> SessionResult.FORBIDDEN;
            case INVALID_STATE -> SessionResult.INVALID_STATE;
            case STALE_REVISION -> SessionResult.STALE;
        };
    }

    private static int findFirstFreeOfferSlot(final @NotNull Map<Integer, ItemStack> offers, final int maxSlots) {
        for (int slot = 0; slot < maxSlots; slot++) {
            final ItemStack existing = offers.get(slot);
            if (existing == null || existing.isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private static int countOfferedItems(final @NotNull Map<Integer, ItemStack> offeredItems) {
        int count = 0;
        for (final ItemStack itemStack : offeredItems.values()) {
            if (itemStack == null || itemStack.isEmpty() || itemStack.getType().isAir()) {
                continue;
            }
            count++;
        }
        return count;
    }

    private static @NotNull String normalizeCurrencyId(final @NotNull String currencyId) {
        return Objects.requireNonNull(currencyId, "currencyId cannot be null").trim().toLowerCase(Locale.ROOT);
    }

    private static @NotNull String normalizeServerId(
        final @Nullable String serverId,
        final @NotNull String fallbackServerId
    ) {
        if (serverId == null || serverId.isBlank()) {
            return fallbackServerId;
        }
        return serverId.trim();
    }

    private static boolean safeJoin(final @NotNull CompletableFuture<Boolean> future) {
        try {
            return Boolean.TRUE.equals(future.join());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private @NotNull List<String> findJExCurrencyIdentifiers() {
        try {
            final Class<?> adapterClass = Class.forName(JEX_ADAPTER_CLASS);
            final RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(adapterClass);
            if (registration == null) {
                return List.of();
            }

            final Object adapter = registration.getProvider();
            final Object currencyMap = adapter.getClass().getMethod("getAllCurrencies").invoke(adapter);
            if (!(currencyMap instanceof Map<?, ?> currencies)) {
                return List.of();
            }

            final Set<String> identifiers = new LinkedHashSet<>();
            for (final Object currency : currencies.values()) {
                if (currency == null) {
                    continue;
                }
                final Object identifierObject = currency.getClass().getMethod("getIdentifier").invoke(currency);
                if (identifierObject instanceof String identifierText && !identifierText.isBlank()) {
                    identifiers.add(normalizeCurrencyId(identifierText));
                }
            }
            return List.copyOf(identifiers);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private static boolean canMutateOffers(
        final @NotNull RTradeSession session,
        final @NotNull UUID actorUuid
    ) {
        final TradeSessionStatus status = session.getStatus();
        if (status == TradeSessionStatus.ACTIVE || status == TradeSessionStatus.COMPLETING) {
            return true;
        }
        return status == TradeSessionStatus.INVITED && session.isInitiator(actorUuid);
    }

    private static boolean isItemOfferLocked(
        final @NotNull RTradeSession session,
        final @NotNull UUID actorUuid
    ) {
        if (session.getStatus() == TradeSessionStatus.COMPLETING) {
            return true;
        }
        return isParticipantReady(session, actorUuid);
    }

    private static boolean isParticipantReady(
        final @NotNull RTradeSession session,
        final @NotNull UUID actorUuid
    ) {
        return session.isInitiator(actorUuid) ? session.isInitiatorReady() : session.isPartnerReady();
    }

    private void restoreItemToActor(
        final @NotNull Player actor,
        final @NotNull ItemStack itemStack,
        final @NotNull UUID tradeUuid
    ) {
        if (itemStack.isEmpty() || itemStack.getType().isAir()) {
            return;
        }

        this.runSync(() -> {
            final Map<Integer, ItemStack> leftovers = actor.getInventory().addItem(itemStack.clone());
            if (!leftovers.isEmpty()) {
                final RRTradeDelivery deliveryRepository = this.plugin.getTradeDeliveryRepository();
                if (deliveryRepository != null) {
                    final Map<Integer, ItemStack> payload = new java.util.HashMap<>();
                    int slot = 0;
                    for (final ItemStack leftover : leftovers.values()) {
                        if (leftover == null || leftover.isEmpty() || leftover.getType().isAir()) {
                            continue;
                        }
                        payload.put(slot++, leftover.clone());
                    }
                    if (!payload.isEmpty()) {
                        deliveryRepository.createPendingAsync(tradeUuid, actor.getUniqueId(), payload, Map.of());
                    }
                }
            }
        });
    }

    private void applyClaimPayload(
        final @NotNull Player player,
        final @NotNull RRTradeDelivery.ClaimResult claimResult
    ) {
        this.runSync(() -> {
            final Map<Integer, ItemStack> itemPayload = claimResult.itemPayload();
            final Map<String, Double> currencyPayload = claimResult.currencyPayload();
            final Map<Integer, ItemStack> leftoverItems = new java.util.HashMap<>();
            final Map<String, Double> failedCurrency = new java.util.LinkedHashMap<>();

            int slot = 0;
            for (final ItemStack itemStack : itemPayload.values()) {
                if (itemStack == null || itemStack.isEmpty() || itemStack.getType().isAir()) {
                    continue;
                }

                final Map<Integer, ItemStack> leftovers = player.getInventory().addItem(itemStack.clone());
                for (final ItemStack leftover : leftovers.values()) {
                    if (leftover == null || leftover.isEmpty() || leftover.getType().isAir()) {
                        continue;
                    }
                    leftoverItems.put(slot++, leftover.clone());
                }
            }

            for (final Map.Entry<String, Double> entry : currencyPayload.entrySet()) {
                final String currencyId = entry.getKey();
                final double amount = entry.getValue() == null ? 0.0D : Math.max(0.0D, entry.getValue());
                if (amount <= EPSILON || currencyId == null || currencyId.isBlank()) {
                    continue;
                }

                if (VAULT_CURRENCY_ID.equalsIgnoreCase(currencyId)) {
                    if (!this.plugin.depositVault(player, amount)) {
                        failedCurrency.put(VAULT_CURRENCY_ID, amount);
                    }
                    continue;
                }

                final JExEconomyBridge bridge = JExEconomyBridge.getBridge();
                if (bridge == null || !bridge.deposit(player, currencyId, amount).join()) {
                    failedCurrency.put(currencyId.trim().toLowerCase(Locale.ROOT), amount);
                }
            }

            if (leftoverItems.isEmpty() && failedCurrency.isEmpty()) {
                return;
            }

            final UUID tradeUuid = this.resolveTradeUuidForCompensation(claimResult.deliveryId());
            final RRTradeDelivery deliveryRepository = this.plugin.getTradeDeliveryRepository();
            if (tradeUuid != null && deliveryRepository != null) {
                deliveryRepository.createPendingAsync(tradeUuid, player.getUniqueId(), leftoverItems, failedCurrency);
            }
        });
    }

    private @Nullable UUID resolveTradeUuidForCompensation(final @Nullable Long deliveryId) {
        if (deliveryId == null) {
            return null;
        }

        final RRTradeDelivery deliveryRepository = this.plugin.getTradeDeliveryRepository();
        if (deliveryRepository == null) {
            return null;
        }
        return deliveryRepository.findById(deliveryId)
            .map(RTradeDelivery::getTradeUuid)
            .orElse(null);
    }

    private <T> @NotNull CompletableFuture<T> supplyAsync(final @NotNull Supplier<T> supplier) {
        final Supplier<T> validatedSupplier = Objects.requireNonNull(supplier, "supplier cannot be null");
        final ExecutorService executor = this.plugin.getExecutor();
        if (executor == null) {
            try {
                return CompletableFuture.completedFuture(validatedSupplier.get());
            } catch (RuntimeException exception) {
                return CompletableFuture.failedFuture(exception);
            }
        }
        return CompletableFuture.supplyAsync(validatedSupplier, executor);
    }

    private void runSync(final @NotNull Runnable runnable) {
        final Runnable validatedRunnable = Objects.requireNonNull(runnable, "runnable cannot be null");
        if (this.plugin.getScheduler() == null) {
            validatedRunnable.run();
            return;
        }
        this.plugin.getScheduler().runSync(validatedRunnable);
    }

    private record TaxCharge(
        @NotNull UUID payerUuid,
        @NotNull String currencyId,
        double amount
    ) {
    }

    private record TaxCollectionResult(
        @NotNull SessionResult result,
        @NotNull List<TaxCharge> collectedCharges
    ) {
    }
}
