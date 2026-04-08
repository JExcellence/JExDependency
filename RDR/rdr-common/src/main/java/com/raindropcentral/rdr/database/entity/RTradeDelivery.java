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
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
 * Persisted trade payout row that recipients can claim from any server.
 *
 * @author ItsRainingHP
 * @since 5.0.0
 * @version 5.0.0
 */
@Entity
@Table(name = "rdr_trade_delivery")
public class RTradeDelivery extends BaseEntity {

    private static final Logger LOGGER = LoggerFactory.getLogger(RTradeDelivery.class);
    private static final TypeReference<Map<String, Double>> CURRENCY_TYPE = new TypeReference<>() {};
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Convert(converter = UUIDConverter.class)
    @Column(name = "trade_uuid", nullable = false)
    private UUID tradeUuid;

    @Convert(converter = UUIDConverter.class)
    @Column(name = "recipient_uuid", nullable = false)
    private UUID recipientUuid;

    @Convert(converter = ItemStackSlotMapConverter.class)
    @Column(name = "item_payload", nullable = false, columnDefinition = "LONGTEXT")
    private Map<Integer, ItemStack> itemPayload = new HashMap<>();

    @Column(name = "currency_payload", nullable = false, columnDefinition = "LONGTEXT")
    private String currencyPayloadJson = "{}";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TradeDeliveryStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Version
    @Column(name = "revision", nullable = false)
    private long revision;

    /**
     * Creates a pending trade delivery row.
     *
     * @param tradeUuid trade UUID this payout belongs to
     * @param recipientUuid recipient player UUID
     * @param itemPayload item payload to claim
     * @param currencyPayload currency payload to claim
     * @throws NullPointerException if any non-null argument is {@code null}
     */
    public RTradeDelivery(
        final @NotNull UUID tradeUuid,
        final @NotNull UUID recipientUuid,
        final @NotNull Map<Integer, ItemStack> itemPayload,
        final @NotNull Map<String, Double> currencyPayload
    ) {
        this.tradeUuid = Objects.requireNonNull(tradeUuid, "tradeUuid cannot be null");
        this.recipientUuid = Objects.requireNonNull(recipientUuid, "recipientUuid cannot be null");
        this.itemPayload = cloneItemPayload(itemPayload);
        this.currencyPayloadJson = serializeCurrencyPayload(currencyPayload);
        this.status = TradeDeliveryStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected RTradeDelivery() {}

    /**
     * Returns the trade UUID this delivery belongs to.
     *
     * @return trade UUID
     */
    public @NotNull UUID getTradeUuid() {
        return this.tradeUuid;
    }

    /**
     * Returns the recipient UUID for this delivery.
     *
     * @return recipient UUID
     */
    public @NotNull UUID getRecipientUuid() {
        return this.recipientUuid;
    }

    /**
     * Returns a defensive copy of the pending item payload.
     *
     * @return sparse slot-indexed item payload
     */
    public @NotNull Map<Integer, ItemStack> getItemPayload() {
        return cloneItemPayload(this.itemPayload);
    }

    /**
     * Returns a defensive copy of the pending currency payload.
     *
     * @return normalized currency payload map
     */
    public @NotNull Map<String, Double> getCurrencyPayload() {
        return parseCurrencyPayload(this.currencyPayloadJson);
    }

    /**
     * Replaces the pending item payload.
     *
     * @param itemPayload replacement sparse slot-indexed payload
     * @throws NullPointerException if {@code itemPayload} is {@code null}
     */
    public void setItemPayload(final @NotNull Map<Integer, ItemStack> itemPayload) {
        this.itemPayload = cloneItemPayload(itemPayload);
    }

    /**
     * Replaces the pending currency payload.
     *
     * @param currencyPayload replacement currency payload map
     * @throws NullPointerException if {@code currencyPayload} is {@code null}
     */
    public void setCurrencyPayload(final @NotNull Map<String, Double> currencyPayload) {
        this.currencyPayloadJson = serializeCurrencyPayload(currencyPayload);
    }

    /**
     * Returns the claim status for this delivery.
     *
     * @return delivery claim status
     */
    public @NotNull TradeDeliveryStatus getStatus() {
        return this.status == null ? TradeDeliveryStatus.PENDING : this.status;
    }

    /**
     * Returns the creation timestamp.
     *
     * @return creation timestamp
     */
    public @NotNull LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    /**
     * Returns the claim timestamp.
     *
     * @return claim timestamp, or {@code null} while pending
     */
    public @Nullable LocalDateTime getClaimedAt() {
        return this.claimedAt;
    }

    /**
     * Marks this delivery as claimed.
     *
     * @param claimedAt claim timestamp to persist
     * @throws NullPointerException if {@code claimedAt} is {@code null}
     */
    public void markClaimed(final @NotNull LocalDateTime claimedAt) {
        this.status = TradeDeliveryStatus.CLAIMED;
        this.claimedAt = Objects.requireNonNull(claimedAt, "claimedAt cannot be null");
    }

    /**
     * Returns whether this delivery has no item or currency payload.
     *
     * @return {@code true} when both payloads are empty
     */
    public boolean isEmpty() {
        return this.getItemPayload().isEmpty() && this.getCurrencyPayload().isEmpty();
    }

    /**
     * Returns the optimistic revision field value.
     *
     * @return delivery revision
     */
    public long getRevision() {
        return this.revision;
    }

    private static @NotNull Map<Integer, ItemStack> cloneItemPayload(final @Nullable Map<Integer, ItemStack> payload) {
        final Map<Integer, ItemStack> clone = new HashMap<>();
        if (payload == null || payload.isEmpty()) {
            return clone;
        }

        for (final Map.Entry<Integer, ItemStack> entry : payload.entrySet()) {
            final Integer slot = entry.getKey();
            final ItemStack itemStack = entry.getValue();
            if (slot == null || slot < 0 || itemStack == null || itemStack.isEmpty()) {
                continue;
            }
            clone.put(slot, itemStack.clone());
        }
        return clone;
    }

    private static @NotNull Map<String, Double> parseCurrencyPayload(final @Nullable String payload) {
        if (payload == null || payload.isBlank()) {
            return new LinkedHashMap<>();
        }

        try {
            final Map<String, Double> rawPayload = OBJECT_MAPPER.readValue(payload, CURRENCY_TYPE);
            return normalizeCurrencyPayload(rawPayload);
        } catch (IOException exception) {
            LOGGER.error("Failed to parse trade delivery currency payload: {}", payload, exception);
            return new LinkedHashMap<>();
        }
    }

    private static @NotNull String serializeCurrencyPayload(final @NotNull Map<String, Double> payload) {
        final Map<String, Double> normalizedPayload = normalizeCurrencyPayload(payload);
        try {
            return OBJECT_MAPPER.writeValueAsString(normalizedPayload);
        } catch (IOException exception) {
            LOGGER.error("Failed to serialize trade delivery currency payload", exception);
            throw new RuntimeException("Failed to serialize trade delivery currency payload", exception);
        }
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
}
