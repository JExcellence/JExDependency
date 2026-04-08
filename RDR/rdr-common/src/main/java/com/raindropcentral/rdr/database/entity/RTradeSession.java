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

package com.raindropcentral.rdr.database.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raindropcentral.rplatform.database.converter.ItemStackSlotMapConverter;
import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Persisted DB-first trade session used for cross-server escrow trading.
 *
 * <p>Each row is the single source of truth for both participants, including item offers, currency
 * offers, readiness flags, and lifecycle state transitions.</p>
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
@Entity
@Table(
    name = "rdr_trade_session",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_rdr_trade_session_trade_uuid",
            columnNames = {"trade_uuid"}
        )
    }
)
/**
 * Represents the RTradeSession API type.
 */
public class RTradeSession extends BaseEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger(RTradeSession.class);
    private static final TypeReference<Map<String, Double>> CURRENCY_TYPE = new TypeReference<>() {};
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String OFFLINE_SERVER_ID = "offline";
    private static final String UNKNOWN_SERVER_ID = "unknown";

    @Convert(converter = UUIDConverter.class)
    @Column(name = "trade_uuid", nullable = false, unique = true)
    private UUID tradeUuid;

    @Convert(converter = UUIDConverter.class)
    @Column(name = "initiator_uuid", nullable = false)
    private UUID initiatorUuid;

    @Convert(converter = UUIDConverter.class)
    @Column(name = "partner_uuid", nullable = false)
    private UUID partnerUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TradeSessionStatus status;

    @Convert(converter = ItemStackSlotMapConverter.class)
    @Column(name = "initiator_offer_items", nullable = false, columnDefinition = "LONGTEXT")
    private Map<Integer, ItemStack> initiatorOfferItems = new HashMap<>();

    @Convert(converter = ItemStackSlotMapConverter.class)
    @Column(name = "partner_offer_items", nullable = false, columnDefinition = "LONGTEXT")
    private Map<Integer, ItemStack> partnerOfferItems = new HashMap<>();

    @Column(name = "initiator_offer_currency_json", nullable = false, columnDefinition = "LONGTEXT")
    private String initiatorOfferCurrencyJson = "{}";

    @Column(name = "partner_offer_currency_json", nullable = false, columnDefinition = "LONGTEXT")
    private String partnerOfferCurrencyJson = "{}";

    @Column(name = "initiator_ready", nullable = false)
    private boolean initiatorReady;

    @Column(name = "partner_ready", nullable = false)
    private boolean partnerReady;

    @Version
    @Column(name = "revision", nullable = false)
    private long revision;

    @Column(name = "invite_expires_at", nullable = false)
    private LocalDateTime inviteExpiresAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "initiator_last_known_server_id", nullable = false, length = 64)
    private String initiatorLastKnownServerId = OFFLINE_SERVER_ID;

    @Column(name = "partner_last_known_server_id", nullable = false, length = 64)
    private String partnerLastKnownServerId = OFFLINE_SERVER_ID;

    @Column(name = "origin_server_id", nullable = false, length = 64)
    private String originServerId = UNKNOWN_SERVER_ID;

    /**
     * Creates a new invited trade session.
     *
     * @param initiatorUuid UUID of the inviting player
     * @param partnerUuid UUID of the invited player
     * @param inviteExpiresAt invite expiration timestamp
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if both participants are the same UUID
     */
    public RTradeSession(
        final @NotNull UUID initiatorUuid,
        final @NotNull UUID partnerUuid,
        final @NotNull LocalDateTime inviteExpiresAt
    ) {
        final UUID validatedInitiator = Objects.requireNonNull(initiatorUuid, "initiatorUuid cannot be null");
        final UUID validatedPartner = Objects.requireNonNull(partnerUuid, "partnerUuid cannot be null");
        if (validatedInitiator.equals(validatedPartner)) {
            throw new IllegalArgumentException("initiator and partner cannot be the same player");
        }

        this.tradeUuid = UUID.randomUUID();
        this.initiatorUuid = validatedInitiator;
        this.partnerUuid = validatedPartner;
        this.status = TradeSessionStatus.INVITED;
        this.initiatorOfferItems = new HashMap<>();
        this.partnerOfferItems = new HashMap<>();
        this.initiatorOfferCurrencyJson = "{}";
        this.partnerOfferCurrencyJson = "{}";
        this.initiatorReady = false;
        this.partnerReady = false;
        this.inviteExpiresAt = Objects.requireNonNull(inviteExpiresAt, "inviteExpiresAt cannot be null");
        this.updatedAt = LocalDateTime.now();
        this.initiatorLastKnownServerId = OFFLINE_SERVER_ID;
        this.partnerLastKnownServerId = OFFLINE_SERVER_ID;
        this.originServerId = UNKNOWN_SERVER_ID;
    }

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected RTradeSession() {}

    /**
     * Returns the immutable session trade UUID.
     *
     * @return trade UUID
     */
    public @NotNull UUID getTradeUuid() {
        return this.tradeUuid;
    }

    /**
     * Returns the initiator player UUID.
     *
     * @return initiator UUID
     */
    public @NotNull UUID getInitiatorUuid() {
        return this.initiatorUuid;
    }

    /**
     * Returns the invited partner player UUID.
     *
     * @return partner UUID
     */
    public @NotNull UUID getPartnerUuid() {
        return this.partnerUuid;
    }

    /**
     * Returns the current lifecycle state.
     *
     * @return session status
     */
    public @NotNull TradeSessionStatus getStatus() {
        return this.status == null ? TradeSessionStatus.INVITED : this.status;
    }

    /**
     * Replaces the lifecycle state.
     *
     * @param status replacement status
     * @throws NullPointerException if {@code status} is {@code null}
     */
    public void setStatus(final @NotNull TradeSessionStatus status) {
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.touch();
    }

    /**
     * Returns a defensive copy of the initiator item-offer map.
     *
     * @return sparse slot-indexed initiator item offer map
     */
    public @NotNull Map<Integer, ItemStack> getInitiatorOfferItems() {
        return cloneItemMap(this.initiatorOfferItems);
    }

    /**
     * Returns a defensive copy of the partner item-offer map.
     *
     * @return sparse slot-indexed partner item offer map
     */
    public @NotNull Map<Integer, ItemStack> getPartnerOfferItems() {
        return cloneItemMap(this.partnerOfferItems);
    }

    /**
     * Replaces the initiator item-offer map.
     *
     * @param offerItems replacement sparse slot-indexed item map
     * @throws NullPointerException if {@code offerItems} is {@code null}
     */
    public void setInitiatorOfferItems(final @NotNull Map<Integer, ItemStack> offerItems) {
        this.initiatorOfferItems = cloneItemMap(offerItems);
        this.touch();
    }

    /**
     * Replaces the partner item-offer map.
     *
     * @param offerItems replacement sparse slot-indexed item map
     * @throws NullPointerException if {@code offerItems} is {@code null}
     */
    public void setPartnerOfferItems(final @NotNull Map<Integer, ItemStack> offerItems) {
        this.partnerOfferItems = cloneItemMap(offerItems);
        this.touch();
    }

    /**
     * Returns a defensive copy of the initiator currency offer payload.
     *
     * @return normalized currency map keyed by currency identifier
     */
    public @NotNull Map<String, Double> getInitiatorOfferCurrency() {
        return this.parseCurrencyPayload(this.initiatorOfferCurrencyJson);
    }

    /**
     * Returns a defensive copy of the partner currency offer payload.
     *
     * @return normalized currency map keyed by currency identifier
     */
    public @NotNull Map<String, Double> getPartnerOfferCurrency() {
        return this.parseCurrencyPayload(this.partnerOfferCurrencyJson);
    }

    /**
     * Replaces the initiator currency offer payload.
     *
     * @param offerCurrency replacement currency map keyed by currency identifier
     * @throws NullPointerException if {@code offerCurrency} is {@code null}
     */
    public void setInitiatorOfferCurrency(final @NotNull Map<String, Double> offerCurrency) {
        this.initiatorOfferCurrencyJson = this.serializeCurrencyPayload(offerCurrency);
        this.touch();
    }

    /**
     * Replaces the partner currency offer payload.
     *
     * @param offerCurrency replacement currency map keyed by currency identifier
     * @throws NullPointerException if {@code offerCurrency} is {@code null}
     */
    public void setPartnerOfferCurrency(final @NotNull Map<String, Double> offerCurrency) {
        this.partnerOfferCurrencyJson = this.serializeCurrencyPayload(offerCurrency);
        this.touch();
    }

    /**
     * Returns whether the initiator currently has the ready/confirm flag set.
     *
     * @return {@code true} when initiator is marked ready
     */
    public boolean isInitiatorReady() {
        return this.initiatorReady;
    }

    /**
     * Returns whether the partner currently has the ready/confirm flag set.
     *
     * @return {@code true} when partner is marked ready
     */
    public boolean isPartnerReady() {
        return this.partnerReady;
    }

    /**
     * Sets the initiator ready flag.
     *
     * @param initiatorReady replacement initiator-ready state
     */
    public void setInitiatorReady(final boolean initiatorReady) {
        this.initiatorReady = initiatorReady;
        this.touch();
    }

    /**
     * Sets the partner ready flag.
     *
     * @param partnerReady replacement partner-ready state
     */
    public void setPartnerReady(final boolean partnerReady) {
        this.partnerReady = partnerReady;
        this.touch();
    }

    /**
     * Returns the row revision used for optimistic stale-update checks.
     *
     * @return revision value
     */
    public long getRevision() {
        return this.revision;
    }

    /**
     * Returns the invite expiration timestamp.
     *
     * @return invite expiry timestamp
     */
    public @NotNull LocalDateTime getInviteExpiresAt() {
        return this.inviteExpiresAt;
    }

    /**
     * Sets the invite expiration timestamp.
     *
     * @param inviteExpiresAt replacement invite expiry
     * @throws NullPointerException if {@code inviteExpiresAt} is {@code null}
     */
    public void setInviteExpiresAt(final @NotNull LocalDateTime inviteExpiresAt) {
        this.inviteExpiresAt = Objects.requireNonNull(inviteExpiresAt, "inviteExpiresAt cannot be null");
        this.touch();
    }

    /**
     * Returns the last-updated timestamp.
     *
     * @return update timestamp
     */
    public @NotNull LocalDateTime getUpdatedAt() {
        return this.updatedAt;
    }

    /**
     * Returns the initiator's last-known server snapshot.
     *
     * @return normalized initiator last-known server route ID
     */
    public @NotNull String getInitiatorLastKnownServerId() {
        return normalizeServerId(this.initiatorLastKnownServerId, OFFLINE_SERVER_ID);
    }

    /**
     * Returns the partner's last-known server snapshot.
     *
     * @return normalized partner last-known server route ID
     */
    public @NotNull String getPartnerLastKnownServerId() {
        return normalizeServerId(this.partnerLastKnownServerId, OFFLINE_SERVER_ID);
    }

    /**
     * Returns the server route where this trade was originally created.
     *
     * @return normalized origin server route ID
     */
    public @NotNull String getOriginServerId() {
        return normalizeServerId(this.originServerId, UNKNOWN_SERVER_ID);
    }

    /**
     * Returns one participant's last-known server snapshot.
     *
     * @param participantUuid participant UUID
     * @return normalized last-known server route ID
     * @throws NullPointerException if {@code participantUuid} is {@code null}
     * @throws IllegalArgumentException if the participant is not part of this session
     */
    public @NotNull String getLastKnownServerIdForParticipant(final @NotNull UUID participantUuid) {
        final UUID validatedParticipantUuid = Objects.requireNonNull(participantUuid, "participantUuid cannot be null");
        if (!this.hasParticipant(validatedParticipantUuid)) {
            throw new IllegalArgumentException("participant does not belong to this trade session");
        }
        return this.isInitiator(validatedParticipantUuid)
            ? this.getInitiatorLastKnownServerId()
            : this.getPartnerLastKnownServerId();
    }

    /**
     * Sets one participant's last-known server snapshot.
     *
     * @param participantUuid participant UUID
     * @param serverId replacement server route ID
     * @throws NullPointerException if {@code participantUuid} is {@code null}
     * @throws IllegalArgumentException if the participant is not part of this session
     */
    public void setLastKnownServerIdForParticipant(
        final @NotNull UUID participantUuid,
        final @Nullable String serverId
    ) {
        final UUID validatedParticipantUuid = Objects.requireNonNull(participantUuid, "participantUuid cannot be null");
        if (!this.hasParticipant(validatedParticipantUuid)) {
            throw new IllegalArgumentException("participant does not belong to this trade session");
        }

        if (this.isInitiator(validatedParticipantUuid)) {
            this.initiatorLastKnownServerId = normalizeServerId(serverId, OFFLINE_SERVER_ID);
        } else {
            this.partnerLastKnownServerId = normalizeServerId(serverId, OFFLINE_SERVER_ID);
        }
        this.touch();
    }

    /**
     * Sets the origin server route ID.
     *
     * @param originServerId replacement origin server route ID
     */
    public void setOriginServerId(final @Nullable String originServerId) {
        this.originServerId = normalizeServerId(originServerId, UNKNOWN_SERVER_ID);
        this.touch();
    }

    /**
     * Refreshes both participant last-known server snapshots and optionally updates origin metadata.
     *
     * @param initiatorServerId initiator server snapshot
     * @param partnerServerId partner server snapshot
     * @param originServerId optional origin server snapshot
     */
    public void refreshParticipantServerSnapshots(
        final @Nullable String initiatorServerId,
        final @Nullable String partnerServerId,
        final @Nullable String originServerId
    ) {
        this.initiatorLastKnownServerId = normalizeServerId(initiatorServerId, OFFLINE_SERVER_ID);
        this.partnerLastKnownServerId = normalizeServerId(partnerServerId, OFFLINE_SERVER_ID);
        if (originServerId != null && !originServerId.isBlank()) {
            this.originServerId = normalizeServerId(originServerId, UNKNOWN_SERVER_ID);
        } else if (this.originServerId == null || this.originServerId.isBlank()) {
            this.originServerId = UNKNOWN_SERVER_ID;
        }
        this.touch();
    }

    /**
     * Returns whether this session contains the supplied player UUID.
     *
     * @param playerUuid player UUID to test
     * @return {@code true} when the player is the initiator or partner
     * @throws NullPointerException if {@code playerUuid} is {@code null}
     */
    public boolean hasParticipant(final @NotNull UUID playerUuid) {
        final UUID validatedPlayerUuid = Objects.requireNonNull(playerUuid, "playerUuid cannot be null");
        return validatedPlayerUuid.equals(this.initiatorUuid) || validatedPlayerUuid.equals(this.partnerUuid);
    }

    /**
     * Returns the opposite participant UUID for the supplied participant.
     *
     * @param participantUuid participant UUID to resolve
     * @return opposite participant UUID, or {@code null} when not part of this session
     * @throws NullPointerException if {@code participantUuid} is {@code null}
     */
    public @Nullable UUID getCounterpartyUuid(final @NotNull UUID participantUuid) {
        final UUID validatedParticipantUuid = Objects.requireNonNull(participantUuid, "participantUuid cannot be null");
        if (validatedParticipantUuid.equals(this.initiatorUuid)) {
            return this.partnerUuid;
        }
        if (validatedParticipantUuid.equals(this.partnerUuid)) {
            return this.initiatorUuid;
        }
        return null;
    }

    /**
     * Returns whether the supplied player UUID is the initiator.
     *
     * @param playerUuid player UUID to test
     * @return {@code true} when this player is the initiator
     * @throws NullPointerException if {@code playerUuid} is {@code null}
     */
    public boolean isInitiator(final @NotNull UUID playerUuid) {
        return Objects.requireNonNull(playerUuid, "playerUuid cannot be null").equals(this.initiatorUuid);
    }

    /**
     * Returns whether the invite is expired at the supplied timestamp.
     *
     * @param now timestamp to test
     * @return {@code true} when invite expiration has passed
     * @throws NullPointerException if {@code now} is {@code null}
     */
    public boolean isInviteExpired(final @NotNull LocalDateTime now) {
        return !Objects.requireNonNull(now, "now cannot be null").isBefore(this.inviteExpiresAt);
    }

    /**
     * Returns whether both participants currently have ready/confirm flags set.
     *
     * @return {@code true} when both participants are marked ready
     */
    public boolean areBothReady() {
        return this.initiatorReady && this.partnerReady;
    }

    /**
     * Clears both ready flags.
     */
    public void clearReadyFlags() {
        this.initiatorReady = false;
        this.partnerReady = false;
        this.touch();
    }

    /**
     * Applies an item offer update for the supplied participant.
     *
     * @param participantUuid participant UUID updating the offer
     * @param offerItems replacement item-offer map
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if the participant is not part of this session
     */
    public void applyOfferItems(
        final @NotNull UUID participantUuid,
        final @NotNull Map<Integer, ItemStack> offerItems
    ) {
        final UUID validatedParticipantUuid = Objects.requireNonNull(participantUuid, "participantUuid cannot be null");
        if (!this.hasParticipant(validatedParticipantUuid)) {
            throw new IllegalArgumentException("participant does not belong to this trade session");
        }

        if (this.isInitiator(validatedParticipantUuid)) {
            this.initiatorOfferItems = cloneItemMap(offerItems);
        } else {
            this.partnerOfferItems = cloneItemMap(offerItems);
        }

        this.clearReadyFlags();
        if (this.status == TradeSessionStatus.COMPLETING) {
            this.status = TradeSessionStatus.ACTIVE;
        }
        this.touch();
    }

    /**
     * Applies a currency offer update for the supplied participant.
     *
     * @param participantUuid participant UUID updating the offer
     * @param offerCurrency replacement currency-offer map
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if the participant is not part of this session
     */
    public void applyOfferCurrency(
        final @NotNull UUID participantUuid,
        final @NotNull Map<String, Double> offerCurrency
    ) {
        final UUID validatedParticipantUuid = Objects.requireNonNull(participantUuid, "participantUuid cannot be null");
        if (!this.hasParticipant(validatedParticipantUuid)) {
            throw new IllegalArgumentException("participant does not belong to this trade session");
        }

        if (this.isInitiator(validatedParticipantUuid)) {
            this.initiatorOfferCurrencyJson = this.serializeCurrencyPayload(offerCurrency);
        } else {
            this.partnerOfferCurrencyJson = this.serializeCurrencyPayload(offerCurrency);
        }

        this.clearReadyFlags();
        if (this.status == TradeSessionStatus.COMPLETING) {
            this.status = TradeSessionStatus.ACTIVE;
        }
        this.touch();
    }

    /**
     * Sets the ready flag for the supplied participant.
     *
     * @param participantUuid participant UUID whose ready state should change
     * @param ready replacement ready state
     * @throws NullPointerException if {@code participantUuid} is {@code null}
     * @throws IllegalArgumentException if the participant is not part of this session
     */
    public void setReady(
        final @NotNull UUID participantUuid,
        final boolean ready
    ) {
        final UUID validatedParticipantUuid = Objects.requireNonNull(participantUuid, "participantUuid cannot be null");
        if (!this.hasParticipant(validatedParticipantUuid)) {
            throw new IllegalArgumentException("participant does not belong to this trade session");
        }

        if (this.isInitiator(validatedParticipantUuid)) {
            this.initiatorReady = ready;
        } else {
            this.partnerReady = ready;
        }
        this.touch();
    }

    /**
     * Returns the current item-offer map for the supplied participant.
     *
     * @param participantUuid participant UUID to resolve
     * @return participant item-offer map
     * @throws NullPointerException if {@code participantUuid} is {@code null}
     * @throws IllegalArgumentException if the participant is not part of this session
     */
    public @NotNull Map<Integer, ItemStack> getOfferItemsForParticipant(final @NotNull UUID participantUuid) {
        final UUID validatedParticipantUuid = Objects.requireNonNull(participantUuid, "participantUuid cannot be null");
        if (!this.hasParticipant(validatedParticipantUuid)) {
            throw new IllegalArgumentException("participant does not belong to this trade session");
        }
        return this.isInitiator(validatedParticipantUuid)
            ? this.getInitiatorOfferItems()
            : this.getPartnerOfferItems();
    }

    /**
     * Returns the current currency-offer map for the supplied participant.
     *
     * @param participantUuid participant UUID to resolve
     * @return participant currency-offer map
     * @throws NullPointerException if {@code participantUuid} is {@code null}
     * @throws IllegalArgumentException if the participant is not part of this session
     */
    public @NotNull Map<String, Double> getOfferCurrencyForParticipant(final @NotNull UUID participantUuid) {
        final UUID validatedParticipantUuid = Objects.requireNonNull(participantUuid, "participantUuid cannot be null");
        if (!this.hasParticipant(validatedParticipantUuid)) {
            throw new IllegalArgumentException("participant does not belong to this trade session");
        }
        return this.isInitiator(validatedParticipantUuid)
            ? this.getInitiatorOfferCurrency()
            : this.getPartnerOfferCurrency();
    }

    /**
     * Returns whether this session has any escrow payload still assigned.
     *
     * @return {@code true} when any item or currency offer exists
     */
    public boolean hasEscrowPayload() {
        return !this.getInitiatorOfferItems().isEmpty()
            || !this.getPartnerOfferItems().isEmpty()
            || !this.getInitiatorOfferCurrency().isEmpty()
            || !this.getPartnerOfferCurrency().isEmpty();
    }

    /**
     * Clears all escrow offer payloads from both participants.
     */
    public void clearEscrowPayload() {
        this.initiatorOfferItems = new HashMap<>();
        this.partnerOfferItems = new HashMap<>();
        this.initiatorOfferCurrencyJson = "{}";
        this.partnerOfferCurrencyJson = "{}";
        this.touch();
    }
    
    @PrePersist
    @PreUpdate
    private void beforePersist() {
        this.touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    private @NotNull Map<String, Double> parseCurrencyPayload(final @Nullable String payload) {
        if (payload == null || payload.isBlank()) {
            return new LinkedHashMap<>();
        }

        try {
            final Map<String, Double> rawPayload = OBJECT_MAPPER.readValue(payload, CURRENCY_TYPE);
            return normalizeCurrencyPayload(rawPayload);
        } catch (IOException exception) {
            LOGGER.error("Failed to parse trade currency payload: {}", payload, exception);
            return new LinkedHashMap<>();
        }
    }

    private @NotNull String serializeCurrencyPayload(final @NotNull Map<String, Double> payload) {
        final Map<String, Double> normalizedPayload = normalizeCurrencyPayload(payload);
        try {
            return OBJECT_MAPPER.writeValueAsString(normalizedPayload);
        } catch (IOException exception) {
            LOGGER.error("Failed to serialize trade currency payload", exception);
            throw new RuntimeException("Failed to serialize trade currency payload", exception);
        }
    }

    private static @NotNull Map<Integer, ItemStack> cloneItemMap(final @Nullable Map<Integer, ItemStack> input) {
        final Map<Integer, ItemStack> copy = new HashMap<>();
        if (input == null || input.isEmpty()) {
            return copy;
        }

        for (final Map.Entry<Integer, ItemStack> entry : input.entrySet()) {
            final Integer slot = entry.getKey();
            final ItemStack itemStack = entry.getValue();
            if (slot == null || slot < 0 || itemStack == null || itemStack.isEmpty()) {
                continue;
            }
            copy.put(slot, itemStack.clone());
        }
        return copy;
    }

    private static @NotNull Map<String, Double> normalizeCurrencyPayload(final @Nullable Map<String, Double> payload) {
        final Map<String, Double> normalized = new LinkedHashMap<>();
        if (payload == null || payload.isEmpty()) {
            return normalized;
        }

        for (final Map.Entry<String, Double> entry : payload.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }

            final String currencyId = entry.getKey().trim().toLowerCase(Locale.ROOT);
            final double amount = entry.getValue() == null ? 0.0D : Math.max(0.0D, entry.getValue());
            if (amount <= 0.0D) {
                continue;
            }
            normalized.put(currencyId, amount);
        }
        return normalized;
    }

    private static @NotNull String normalizeServerId(
        final @Nullable String serverId,
        final @NotNull String fallback
    ) {
        if (serverId == null || serverId.isBlank()) {
            return fallback;
        }
        return serverId.trim();
    }
}
