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

package com.raindropcentral.rdt.database.entity;

import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

/**
 * Persistent ledger entry for town lifecycle events.
 *
 * <p>Used to track player joins, leaves, and role changes as an append-only timeline.</p>
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@Entity
@Table(name = "town_ledger_entries")
@SuppressWarnings({
        "unused",
        "JpaDataSourceORMInspection"
})
/**
 * Represents the TownLedgerEntry API type.
 */
public class TownLedgerEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "town_id", nullable = false)
    private RTown town;

    @Column(name = "player_uuid", nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID playerUuid;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "detail", nullable = true, length = 512)
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private long occurredAt;

    /** Required by Hibernate. */
    protected TownLedgerEntry() {
    }

    /**
     * Creates a new town ledger entry.
     *
     * @param town owning town
     * @param playerUuid target player UUID
     * @param eventType event type value
     * @param detail optional detail payload
     */
    public TownLedgerEntry(
            final @NonNull RTown town,
            final @NonNull UUID playerUuid,
            final @NonNull String eventType,
            final @Nullable String detail
    ) {
        this.town = town;
        this.playerUuid = playerUuid;
        this.eventType = eventType.trim().toUpperCase(Locale.ROOT);
        this.detail = detail;
        this.occurredAt = System.currentTimeMillis();
    }

    /**
     * Returns the entry player UUID.
     *
     * @return player UUID
     */
    public UUID getPlayerUuid() {
        return this.playerUuid;
    }

    /**
     * Returns the normalized event type.
     *
     * @return event type
     */
    public String getEventType() {
        return this.eventType;
    }

    /**
     * Returns the optional detail payload.
     *
     * @return detail, or {@code null}
     */
    public @Nullable String getDetail() {
        return this.detail;
    }

    /**
     * Returns the event epoch-millis timestamp.
     *
     * @return event timestamp
     */
    public long getOccurredAt() {
        return this.occurredAt;
    }
}
