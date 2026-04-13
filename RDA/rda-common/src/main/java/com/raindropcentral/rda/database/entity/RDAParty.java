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

package com.raindropcentral.rda.database.entity;

import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Persistent party root row for RDA players.
 *
 * @author Codex
 * @since 1.3.0
 * @version 1.3.0
 */
@Entity
@Table(name = "rda_parties")
public class RDAParty extends BaseEntity {

    @Column(name = "party_uuid", nullable = false, unique = true)
    @Convert(converter = UUIDConverter.class)
    private UUID partyUuid;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "leader_player_id_fk", nullable = false)
    private RDAPlayer leaderProfile;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    /**
     * Creates a party row with the supplied leader.
     *
     * @param leaderProfile party leader profile
     */
    public RDAParty(final @NotNull RDAPlayer leaderProfile) {
        this.partyUuid = UUID.randomUUID();
        this.leaderProfile = Objects.requireNonNull(leaderProfile, "leaderProfile");
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Constructor reserved for JPA hydration.
     */
    protected RDAParty() {
    }

    /**
     * Returns the stable party UUID.
     *
     * @return stable party UUID
     */
    public @NotNull UUID getPartyUuid() {
        return Objects.requireNonNull(this.partyUuid, "partyUuid");
    }

    /**
     * Returns the current leader profile.
     *
     * @return leader profile
     */
    public @NotNull RDAPlayer getLeaderProfile() {
        return Objects.requireNonNull(this.leaderProfile, "leaderProfile");
    }

    /**
     * Updates the current leader profile.
     *
     * @param leaderProfile replacement leader profile
     */
    public void setLeaderProfile(final @NotNull RDAPlayer leaderProfile) {
        this.leaderProfile = Objects.requireNonNull(leaderProfile, "leaderProfile");
    }

    /**
     * Returns the creation timestamp in epoch milliseconds.
     *
     * @return creation timestamp
     */
    public long getCreatedAtMillis() {
        return Math.max(0L, this.createdAt);
    }
}
