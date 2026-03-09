package com.raindropcentral.rdr.database.repository;

import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import com.raindropcentral.rdr.database.entity.RTradeDelivery;
import com.raindropcentral.rdr.database.entity.RTradeSession;
import com.raindropcentral.rdr.database.entity.TradeSessionStatus;
import de.jexcellence.hibernate.repository.BaseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository for DB-first trade-session lifecycle and escrow settlement transitions.
 *
 * <p>All mutating operations use pessimistic row locks and optional expected-revision checks to
 * prevent cross-server race conditions.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
public class RRTradeSession extends BaseRepository<RTradeSession, Long> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RRTradeSession.class);

    private static final EnumSet<TradeSessionStatus> NON_TERMINAL_STATUSES = EnumSet.of(
        TradeSessionStatus.INVITED,
        TradeSessionStatus.ACTIVE,
        TradeSessionStatus.COMPLETING
    );

    /**
     * Result of creating a trade invite session.
     *
     * @param status invite creation status
     * @param tradeUuid created trade UUID, or {@code null} when invite creation failed
     */
    public record InviteCreateResult(
        @NotNull InviteCreateStatus status,
        @Nullable UUID tradeUuid
    ) {
    }

    /**
     * Invite creation outcomes.
     */
    public enum InviteCreateStatus {
        /**
         * Invite row was created.
         */
        CREATED,
        /**
         * Initiator attempted to invite themselves.
         */
        SELF_TRADE,
        /**
         * The participant pair already has a non-terminal session.
         */
        PARTICIPANT_BUSY
    }

    /**
     * Generic mutation outcomes for invite/session lifecycle operations.
     */
    public enum MutationResult {
        /**
         * Mutation completed successfully.
         */
        SUCCESS,
        /**
         * Trade session row was not found.
         */
        MISSING,
        /**
         * Requesting actor is not a valid participant for the target session.
         */
        FORBIDDEN,
        /**
         * Session is in a state that does not allow the requested mutation.
         */
        INVALID_STATE,
        /**
         * Session revision no longer matches the client-side snapshot.
         */
        STALE_REVISION,
        /**
         * Invite has already expired.
         */
        EXPIRED
    }

    /**
     * Completion outcomes for final settlement confirmation.
     */
    public enum CompletionResult {
        /**
         * Actor confirmation was saved and the trade waits for the partner confirmation.
         */
        WAITING_FOR_PARTNER,
        /**
         * Both confirmations were present and settlement deliveries were created.
         */
        COMPLETED,
        /**
         * Completion failed because session row does not exist.
         */
        MISSING,
        /**
         * Completion failed because actor is not a participant.
         */
        FORBIDDEN,
        /**
         * Completion failed because session is not in {@link TradeSessionStatus#COMPLETING}.
         */
        INVALID_STATE,
        /**
         * Completion failed due to stale revision.
         */
        STALE_REVISION
    }

    /**
     * Creates a repository for {@link RTradeSession} entities.
     *
     * @param executorService executor used for async repository operations
     * @param entityManagerFactory entity manager factory used for persistence operations
     * @throws NullPointerException if any argument is {@code null}
     */
    public RRTradeSession(
        final @NotNull ExecutorService executorService,
        final @NotNull EntityManagerFactory entityManagerFactory
    ) {
        super(executorService, entityManagerFactory, RTradeSession.class);
    }

    /**
     * Creates a new trade invite when no non-terminal session exists for the same participant pair.
     *
     * @param initiatorUuid inviter UUID
     * @param partnerUuid invited partner UUID
     * @param inviteExpiresAt invite expiration timestamp
     * @return async invite creation result
     * @throws NullPointerException if any argument is {@code null}
     */
    public @NotNull CompletableFuture<InviteCreateResult> createInviteAsync(
        final @NotNull UUID initiatorUuid,
        final @NotNull UUID partnerUuid,
        final @NotNull LocalDateTime inviteExpiresAt
    ) {
        return CompletableFuture.supplyAsync(
            () -> this.createInvite(initiatorUuid, partnerUuid, inviteExpiresAt),
            getExecutorService()
        );
    }

    /**
     * Finds a trade session by trade UUID.
     *
     * @param tradeUuid trade UUID to look up
     * @return matching trade session, or {@code null} when none exists
     * @throws NullPointerException if {@code tradeUuid} is {@code null}
     */
    public @Nullable RTradeSession findByTradeUuid(final @NotNull UUID tradeUuid) {
        final UUID validatedTradeUuid = Objects.requireNonNull(tradeUuid, "tradeUuid cannot be null");
        return this.executeInTransaction(entityManager -> entityManager.createQuery(
                "select session from RTradeSession session where session.tradeUuid = :tradeUuid",
                RTradeSession.class
            )
            .setParameter("tradeUuid", validatedTradeUuid)
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .orElse(null));
    }

    /**
     * Finds pending invites for all supplied recipient UUIDs.
     *
     * @param recipientUuids recipient UUIDs currently online on this server
     * @param now comparison timestamp for invite expiry
     * @return immutable pending-invite list ordered by newest update first
     * @throws NullPointerException if any argument is {@code null}
     */
    public @NotNull List<RTradeSession> findPendingInvitesForRecipients(
        final @NotNull List<UUID> recipientUuids,
        final @NotNull LocalDateTime now
    ) {
        final List<UUID> validatedRecipients = List.copyOf(Objects.requireNonNull(recipientUuids, "recipientUuids cannot be null"));
        final LocalDateTime validatedNow = Objects.requireNonNull(now, "now cannot be null");
        if (validatedRecipients.isEmpty()) {
            return List.of();
        }

        return List.copyOf(this.executeInTransaction(entityManager -> entityManager.createQuery(
                "select session from RTradeSession session "
                    + "where session.status = :status "
                    + "and session.partnerUuid in :recipientUuids "
                    + "and session.inviteExpiresAt > :now "
                    + "order by session.updatedAt desc",
                RTradeSession.class
            )
            .setParameter("status", TradeSessionStatus.INVITED)
            .setParameter("recipientUuids", validatedRecipients)
            .setParameter("now", validatedNow)
            .getResultList()));
    }

    /**
     * Finds non-terminal sessions where any supplied participant is involved.
     *
     * @param participantUuids participant UUIDs to match
     * @return immutable non-terminal session snapshot ordered by newest update first
     * @throws NullPointerException if {@code participantUuids} is {@code null}
     */
    public @NotNull List<RTradeSession> findNonTerminalByParticipants(final @NotNull List<UUID> participantUuids) {
        final List<UUID> validatedParticipants = List.copyOf(Objects.requireNonNull(participantUuids, "participantUuids cannot be null"));
        if (validatedParticipants.isEmpty()) {
            return List.of();
        }

        return List.copyOf(this.executeInTransaction(entityManager -> entityManager.createQuery(
                "select session from RTradeSession session "
                    + "where session.status in :statuses "
                    + "and (session.initiatorUuid in :participants or session.partnerUuid in :participants) "
                    + "order by session.updatedAt desc",
                RTradeSession.class
            )
            .setParameter("statuses", NON_TERMINAL_STATUSES)
            .setParameter("participants", validatedParticipants)
            .getResultList()));
    }

    /**
     * Finds sessions by trade UUID list.
     *
     * @param tradeUuids trade UUID list
     * @return immutable matching session list
     * @throws NullPointerException if {@code tradeUuids} is {@code null}
     */
    public @NotNull List<RTradeSession> findByTradeUuids(final @NotNull List<UUID> tradeUuids) {
        final List<UUID> validatedTradeUuids = List.copyOf(Objects.requireNonNull(tradeUuids, "tradeUuids cannot be null"));
        if (validatedTradeUuids.isEmpty()) {
            return List.of();
        }

        return List.copyOf(this.executeInTransaction(entityManager -> entityManager.createQuery(
                "select session from RTradeSession session where session.tradeUuid in :tradeUuids",
                RTradeSession.class
            )
            .setParameter("tradeUuids", validatedTradeUuids)
            .getResultList()));
    }

    /**
     * Returns UUID entries from {@code rdr_players} for cross-server trade target discovery.
     *
     * <p>This method intentionally reads from the RDR player table using a native query so trade
     * target lists can include players outside the local Bukkit online set.</p>
     *
     * @param limit maximum number of UUID rows to return
     * @return immutable UUID list ordered by query result order
     */
    public @NotNull List<UUID> findRdrPlayerUuids(final int limit) {
        final int validatedLimit = Math.max(1, limit);
        return List.copyOf(this.executeInTransaction(entityManager -> {
            final List<?> rawRows;
            try {
                rawRows = entityManager.createNativeQuery(
                        "select player_uuid from rdr_players where player_uuid is not null"
                    )
                    .setMaxResults(validatedLimit)
                    .getResultList();
            } catch (RuntimeException exception) {
                LOGGER.warn("Failed to load trade targets from rdr_players table", exception);
                return List.<UUID>of();
            }

            final Set<UUID> uniqueUuids = new LinkedHashSet<>();
            for (final Object rawRow : rawRows) {
                final UUID parsedUuid = parseUuid(rawRow);
                if (parsedUuid != null) {
                    uniqueUuids.add(parsedUuid);
                }
            }
            return new ArrayList<>(uniqueUuids);
        }));
    }

    /**
     * Accepts an invite and transitions session state to {@link TradeSessionStatus#ACTIVE}.
     *
     * @param tradeUuid trade UUID to mutate
     * @param partnerUuid accepting partner UUID
     * @param expectedRevision expected row revision, or negative to skip stale checks
     * @param now comparison timestamp for invite expiry
     * @return async mutation result
     * @throws NullPointerException if any non-primitive argument is {@code null}
     */
    public @NotNull CompletableFuture<MutationResult> acceptInviteAsync(
        final @NotNull UUID tradeUuid,
        final @NotNull UUID partnerUuid,
        final long expectedRevision,
        final @NotNull LocalDateTime now
    ) {
        return CompletableFuture.supplyAsync(
            () -> this.acceptInvite(tradeUuid, partnerUuid, expectedRevision, now),
            getExecutorService()
        );
    }

    /**
     * Terminates a trade session as canceled by a participant.
     *
     * @param tradeUuid trade UUID to mutate
     * @param actorUuid participant UUID requesting cancellation
     * @param expectedRevision expected row revision, or negative to skip stale checks
     * @return async cancellation result
     * @throws NullPointerException if any argument is {@code null}
     */
    public @NotNull CompletableFuture<MutationResult> cancelSessionAsync(
        final @NotNull UUID tradeUuid,
        final @NotNull UUID actorUuid,
        final long expectedRevision
    ) {
        return CompletableFuture.supplyAsync(
            () -> this.cancelSession(tradeUuid, actorUuid, expectedRevision),
            getExecutorService()
        );
    }

    /**
     * Expires all pending invites whose expiration timestamp has passed.
     *
     * @param now comparison timestamp
     * @return async count of expired invite rows
     * @throws NullPointerException if {@code now} is {@code null}
     */
    public @NotNull CompletableFuture<Integer> expireInvitesAsync(final @NotNull LocalDateTime now) {
        return CompletableFuture.supplyAsync(
            () -> this.expireInvites(now),
            getExecutorService()
        );
    }

    /**
     * Replaces participant item offers and resets both ready flags.
     *
     * @param tradeUuid trade UUID to mutate
     * @param actorUuid participant UUID performing the mutation
     * @param expectedRevision expected row revision, or negative to skip stale checks
     * @param offerItems replacement item-offer map
     * @return async mutation result
     * @throws NullPointerException if any non-primitive argument is {@code null}
     */
    public @NotNull CompletableFuture<MutationResult> updateOfferItemsAsync(
        final @NotNull UUID tradeUuid,
        final @NotNull UUID actorUuid,
        final long expectedRevision,
        final @NotNull Map<Integer, ItemStack> offerItems
    ) {
        return CompletableFuture.supplyAsync(
            () -> this.updateOfferItems(tradeUuid, actorUuid, expectedRevision, offerItems),
            getExecutorService()
        );
    }

    /**
     * Replaces participant currency offers and resets both ready flags.
     *
     * @param tradeUuid trade UUID to mutate
     * @param actorUuid participant UUID performing the mutation
     * @param expectedRevision expected row revision, or negative to skip stale checks
     * @param offerCurrency replacement currency-offer map
     * @return async mutation result
     * @throws NullPointerException if any non-primitive argument is {@code null}
     */
    public @NotNull CompletableFuture<MutationResult> updateOfferCurrencyAsync(
        final @NotNull UUID tradeUuid,
        final @NotNull UUID actorUuid,
        final long expectedRevision,
        final @NotNull Map<String, Double> offerCurrency
    ) {
        return CompletableFuture.supplyAsync(
            () -> this.updateOfferCurrency(tradeUuid, actorUuid, expectedRevision, offerCurrency),
            getExecutorService()
        );
    }

    /**
     * Sets participant ready state while session is active.
     *
     * @param tradeUuid trade UUID to mutate
     * @param actorUuid participant UUID setting ready state
     * @param expectedRevision expected row revision, or negative to skip stale checks
     * @param ready replacement ready state for this participant
     * @return async mutation result
     * @throws NullPointerException if any non-primitive argument is {@code null}
     */
    public @NotNull CompletableFuture<MutationResult> setActiveReadyAsync(
        final @NotNull UUID tradeUuid,
        final @NotNull UUID actorUuid,
        final long expectedRevision,
        final boolean ready
    ) {
        return CompletableFuture.supplyAsync(
            () -> this.setActiveReady(tradeUuid, actorUuid, expectedRevision, ready),
            getExecutorService()
        );
    }

    /**
     * Confirms settlement during {@link TradeSessionStatus#COMPLETING}.
     *
     * @param tradeUuid trade UUID to mutate
     * @param actorUuid participant UUID confirming completion
     * @param expectedRevision expected row revision, or negative to skip stale checks
     * @return async completion result
     * @throws NullPointerException if any argument is {@code null}
     */
    public @NotNull CompletableFuture<CompletionResult> confirmSettlementAsync(
        final @NotNull UUID tradeUuid,
        final @NotNull UUID actorUuid,
        final long expectedRevision
    ) {
        return CompletableFuture.supplyAsync(
            () -> this.confirmSettlement(tradeUuid, actorUuid, expectedRevision),
            getExecutorService()
        );
    }

    private @NotNull InviteCreateResult createInvite(
        final @NotNull UUID initiatorUuid,
        final @NotNull UUID partnerUuid,
        final @NotNull LocalDateTime inviteExpiresAt
    ) {
        final UUID validatedInitiatorUuid = Objects.requireNonNull(initiatorUuid, "initiatorUuid cannot be null");
        final UUID validatedPartnerUuid = Objects.requireNonNull(partnerUuid, "partnerUuid cannot be null");
        final LocalDateTime validatedInviteExpiresAt = Objects.requireNonNull(inviteExpiresAt, "inviteExpiresAt cannot be null");
        if (validatedInitiatorUuid.equals(validatedPartnerUuid)) {
            return new InviteCreateResult(InviteCreateStatus.SELF_TRADE, null);
        }

        return this.executeInTransaction(entityManager -> {
            final List<RTradeSession> existingPairSessions = entityManager.createQuery(
                    "select session from RTradeSession session "
                        + "where session.status in :statuses "
                        + "and ("
                        + "(session.initiatorUuid = :initiatorUuid and session.partnerUuid = :partnerUuid) "
                        + "or "
                        + "(session.initiatorUuid = :partnerUuid and session.partnerUuid = :initiatorUuid)"
                        + ")",
                    RTradeSession.class
                )
                .setParameter("statuses", NON_TERMINAL_STATUSES)
                .setParameter("initiatorUuid", validatedInitiatorUuid)
                .setParameter("partnerUuid", validatedPartnerUuid)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
            if (!existingPairSessions.isEmpty()) {
                return new InviteCreateResult(InviteCreateStatus.PARTICIPANT_BUSY, null);
            }

            final RTradeSession session = new RTradeSession(
                validatedInitiatorUuid,
                validatedPartnerUuid,
                validatedInviteExpiresAt
            );
            entityManager.persist(session);
            return new InviteCreateResult(InviteCreateStatus.CREATED, session.getTradeUuid());
        });
    }

    private @NotNull MutationResult acceptInvite(
        final @NotNull UUID tradeUuid,
        final @NotNull UUID partnerUuid,
        final long expectedRevision,
        final @NotNull LocalDateTime now
    ) {
        final UUID validatedTradeUuid = Objects.requireNonNull(tradeUuid, "tradeUuid cannot be null");
        final UUID validatedPartnerUuid = Objects.requireNonNull(partnerUuid, "partnerUuid cannot be null");
        final LocalDateTime validatedNow = Objects.requireNonNull(now, "now cannot be null");
        return this.executeInTransaction(entityManager -> {
            final RTradeSession session = this.findTradeForUpdate(entityManager, validatedTradeUuid);
            if (session == null) {
                return MutationResult.MISSING;
            }
            if (!matchesExpectedRevision(session, expectedRevision)) {
                return MutationResult.STALE_REVISION;
            }
            if (!session.getPartnerUuid().equals(validatedPartnerUuid)) {
                return MutationResult.FORBIDDEN;
            }
            if (session.getStatus() != TradeSessionStatus.INVITED) {
                return MutationResult.INVALID_STATE;
            }
            if (session.isInviteExpired(validatedNow)) {
                session.setStatus(TradeSessionStatus.EXPIRED);
                persistRefundDeliveries(entityManager, session);
                session.clearEscrowPayload();
                session.clearReadyFlags();
                return MutationResult.EXPIRED;
            }

            session.setStatus(TradeSessionStatus.ACTIVE);
            session.clearReadyFlags();
            return MutationResult.SUCCESS;
        });
    }

    private @NotNull MutationResult cancelSession(
        final @NotNull UUID tradeUuid,
        final @NotNull UUID actorUuid,
        final long expectedRevision
    ) {
        final UUID validatedTradeUuid = Objects.requireNonNull(tradeUuid, "tradeUuid cannot be null");
        final UUID validatedActorUuid = Objects.requireNonNull(actorUuid, "actorUuid cannot be null");
        return this.executeInTransaction(entityManager -> {
            final RTradeSession session = this.findTradeForUpdate(entityManager, validatedTradeUuid);
            if (session == null) {
                return MutationResult.MISSING;
            }
            if (!matchesExpectedRevision(session, expectedRevision)) {
                return MutationResult.STALE_REVISION;
            }
            if (!session.hasParticipant(validatedActorUuid)) {
                return MutationResult.FORBIDDEN;
            }
            if (!NON_TERMINAL_STATUSES.contains(session.getStatus())) {
                return MutationResult.INVALID_STATE;
            }

            persistRefundDeliveries(entityManager, session);
            session.setStatus(TradeSessionStatus.CANCELED);
            session.clearEscrowPayload();
            session.clearReadyFlags();
            return MutationResult.SUCCESS;
        });
    }

    private int expireInvites(final @NotNull LocalDateTime now) {
        final LocalDateTime validatedNow = Objects.requireNonNull(now, "now cannot be null");
        return this.executeInTransaction(entityManager -> {
            final List<RTradeSession> expiredInvites = entityManager.createQuery(
                    "select session from RTradeSession session "
                        + "where session.status = :status and session.inviteExpiresAt <= :now",
                    RTradeSession.class
                )
                .setParameter("status", TradeSessionStatus.INVITED)
                .setParameter("now", validatedNow)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();

            for (final RTradeSession session : expiredInvites) {
                persistRefundDeliveries(entityManager, session);
                session.setStatus(TradeSessionStatus.EXPIRED);
                session.clearEscrowPayload();
                session.clearReadyFlags();
            }
            return expiredInvites.size();
        });
    }

    private @NotNull MutationResult updateOfferItems(
        final @NotNull UUID tradeUuid,
        final @NotNull UUID actorUuid,
        final long expectedRevision,
        final @NotNull Map<Integer, ItemStack> offerItems
    ) {
        final UUID validatedTradeUuid = Objects.requireNonNull(tradeUuid, "tradeUuid cannot be null");
        final UUID validatedActorUuid = Objects.requireNonNull(actorUuid, "actorUuid cannot be null");
        final Map<Integer, ItemStack> validatedOfferItems = Map.copyOf(Objects.requireNonNull(offerItems, "offerItems cannot be null"));
        return this.executeInTransaction(entityManager -> {
            final RTradeSession session = this.findTradeForUpdate(entityManager, validatedTradeUuid);
            if (session == null) {
                return MutationResult.MISSING;
            }
            if (!matchesExpectedRevision(session, expectedRevision)) {
                return MutationResult.STALE_REVISION;
            }
            if (!session.hasParticipant(validatedActorUuid)) {
                return MutationResult.FORBIDDEN;
            }
            if (!isOfferMutationAllowed(session, validatedActorUuid)) {
                return MutationResult.INVALID_STATE;
            }

            session.applyOfferItems(validatedActorUuid, validatedOfferItems);
            return MutationResult.SUCCESS;
        });
    }

    private @NotNull MutationResult updateOfferCurrency(
        final @NotNull UUID tradeUuid,
        final @NotNull UUID actorUuid,
        final long expectedRevision,
        final @NotNull Map<String, Double> offerCurrency
    ) {
        final UUID validatedTradeUuid = Objects.requireNonNull(tradeUuid, "tradeUuid cannot be null");
        final UUID validatedActorUuid = Objects.requireNonNull(actorUuid, "actorUuid cannot be null");
        final Map<String, Double> validatedOfferCurrency = Map.copyOf(Objects.requireNonNull(offerCurrency, "offerCurrency cannot be null"));
        return this.executeInTransaction(entityManager -> {
            final RTradeSession session = this.findTradeForUpdate(entityManager, validatedTradeUuid);
            if (session == null) {
                return MutationResult.MISSING;
            }
            if (!matchesExpectedRevision(session, expectedRevision)) {
                return MutationResult.STALE_REVISION;
            }
            if (!session.hasParticipant(validatedActorUuid)) {
                return MutationResult.FORBIDDEN;
            }
            if (!isOfferMutationAllowed(session, validatedActorUuid)) {
                return MutationResult.INVALID_STATE;
            }

            session.applyOfferCurrency(validatedActorUuid, validatedOfferCurrency);
            return MutationResult.SUCCESS;
        });
    }

    private @NotNull MutationResult setActiveReady(
        final @NotNull UUID tradeUuid,
        final @NotNull UUID actorUuid,
        final long expectedRevision,
        final boolean ready
    ) {
        final UUID validatedTradeUuid = Objects.requireNonNull(tradeUuid, "tradeUuid cannot be null");
        final UUID validatedActorUuid = Objects.requireNonNull(actorUuid, "actorUuid cannot be null");
        return this.executeInTransaction(entityManager -> {
            final RTradeSession session = this.findTradeForUpdate(entityManager, validatedTradeUuid);
            if (session == null) {
                return MutationResult.MISSING;
            }
            if (!matchesExpectedRevision(session, expectedRevision)) {
                return MutationResult.STALE_REVISION;
            }
            if (!session.hasParticipant(validatedActorUuid)) {
                return MutationResult.FORBIDDEN;
            }
            if (session.getStatus() != TradeSessionStatus.ACTIVE) {
                return MutationResult.INVALID_STATE;
            }

            session.setReady(validatedActorUuid, ready);
            if (session.areBothReady()) {
                session.setStatus(TradeSessionStatus.COMPLETING);
                session.clearReadyFlags();
            }
            return MutationResult.SUCCESS;
        });
    }

    private @NotNull CompletionResult confirmSettlement(
        final @NotNull UUID tradeUuid,
        final @NotNull UUID actorUuid,
        final long expectedRevision
    ) {
        final UUID validatedTradeUuid = Objects.requireNonNull(tradeUuid, "tradeUuid cannot be null");
        final UUID validatedActorUuid = Objects.requireNonNull(actorUuid, "actorUuid cannot be null");
        return this.executeInTransaction(entityManager -> {
            final RTradeSession session = this.findTradeForUpdate(entityManager, validatedTradeUuid);
            if (session == null) {
                return CompletionResult.MISSING;
            }
            if (!matchesExpectedRevision(session, expectedRevision)) {
                return CompletionResult.STALE_REVISION;
            }
            if (!session.hasParticipant(validatedActorUuid)) {
                return CompletionResult.FORBIDDEN;
            }
            if (session.getStatus() == TradeSessionStatus.COMPLETED) {
                return CompletionResult.COMPLETED;
            }
            if (session.getStatus() != TradeSessionStatus.COMPLETING) {
                return CompletionResult.INVALID_STATE;
            }

            session.setReady(validatedActorUuid, true);
            if (!session.areBothReady()) {
                return CompletionResult.WAITING_FOR_PARTNER;
            }

            persistSettlementDeliveries(entityManager, session);
            session.setStatus(TradeSessionStatus.COMPLETED);
            session.clearReadyFlags();
            session.clearEscrowPayload();
            return CompletionResult.COMPLETED;
        });
    }

    private static boolean matchesExpectedRevision(
        final @NotNull RTradeSession session,
        final long expectedRevision
    ) {
        return expectedRevision < 0L || session.getRevision() == expectedRevision;
    }

    private static boolean isOfferMutationAllowed(
        final @NotNull RTradeSession session,
        final @NotNull UUID actorUuid
    ) {
        final TradeSessionStatus status = session.getStatus();
        if (status == TradeSessionStatus.ACTIVE || status == TradeSessionStatus.COMPLETING) {
            return true;
        }
        return status == TradeSessionStatus.INVITED && session.isInitiator(actorUuid);
    }

    private static @Nullable UUID parseUuid(final @Nullable Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof UUID value) {
            return value;
        }
        if (rawValue instanceof byte[] bytes) {
            return parseUuidFromBytes(bytes);
        }

        final String uuidText = rawValue.toString();
        if (uuidText.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(uuidText.trim());
        } catch (IllegalArgumentException ignored) {
            return parseUuidFromCompactHex(uuidText);
        }
    }

    private static @Nullable UUID parseUuidFromBytes(final @NotNull byte[] bytes) {
        if (bytes.length == 16) {
            final ByteBuffer buffer = ByteBuffer.wrap(bytes);
            return new UUID(buffer.getLong(), buffer.getLong());
        }

        final String textValue = new String(bytes, java.nio.charset.StandardCharsets.UTF_8).trim();
        if (textValue.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(textValue);
        } catch (IllegalArgumentException ignored) {
            return parseUuidFromCompactHex(textValue);
        }
    }

    private static @Nullable UUID parseUuidFromCompactHex(final @NotNull String value) {
        final String compact = value.trim().replace("-", "");
        if (compact.length() != 32) {
            return null;
        }
        for (int index = 0; index < compact.length(); index++) {
            final char character = compact.charAt(index);
            final boolean isHex = (character >= '0' && character <= '9')
                || (character >= 'a' && character <= 'f')
                || (character >= 'A' && character <= 'F');
            if (!isHex) {
                return null;
            }
        }

        final String dashed = compact.substring(0, 8)
            + "-"
            + compact.substring(8, 12)
            + "-"
            + compact.substring(12, 16)
            + "-"
            + compact.substring(16, 20)
            + "-"
            + compact.substring(20);
        try {
            return UUID.fromString(dashed);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private @Nullable RTradeSession findTradeForUpdate(
        final @NotNull EntityManager entityManager,
        final @NotNull UUID tradeUuid
    ) {
        return entityManager.createQuery(
                "select session from RTradeSession session where session.tradeUuid = :tradeUuid",
                RTradeSession.class
            )
            .setParameter("tradeUuid", tradeUuid)
            .setLockMode(LockModeType.PESSIMISTIC_WRITE)
            .setMaxResults(1)
            .getResultStream()
            .findFirst()
            .orElse(null);
    }

    private static void persistSettlementDeliveries(
        final @NotNull EntityManager entityManager,
        final @NotNull RTradeSession session
    ) {
        persistDelivery(
            entityManager,
            session.getTradeUuid(),
            session.getInitiatorUuid(),
            session.getPartnerOfferItems(),
            session.getPartnerOfferCurrency()
        );
        persistDelivery(
            entityManager,
            session.getTradeUuid(),
            session.getPartnerUuid(),
            session.getInitiatorOfferItems(),
            session.getInitiatorOfferCurrency()
        );
    }

    private static void persistRefundDeliveries(
        final @NotNull EntityManager entityManager,
        final @NotNull RTradeSession session
    ) {
        persistDelivery(
            entityManager,
            session.getTradeUuid(),
            session.getInitiatorUuid(),
            session.getInitiatorOfferItems(),
            session.getInitiatorOfferCurrency()
        );
        persistDelivery(
            entityManager,
            session.getTradeUuid(),
            session.getPartnerUuid(),
            session.getPartnerOfferItems(),
            session.getPartnerOfferCurrency()
        );
    }

    private static void persistDelivery(
        final @NotNull EntityManager entityManager,
        final @NotNull UUID tradeUuid,
        final @NotNull UUID recipientUuid,
        final @NotNull Map<Integer, ItemStack> itemPayload,
        final @NotNull Map<String, Double> currencyPayload
    ) {
        if (itemPayload.isEmpty() && currencyPayload.isEmpty()) {
            return;
        }
        entityManager.persist(new RTradeDelivery(tradeUuid, recipientUuid, itemPayload, currencyPayload));
    }
}
