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

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Persistent membership row linking an RDA player profile to a party.
 *
 * @author Codex
 * @since 1.3.0
 * @version 1.3.0
 */
@Entity
@Table(
    name = "rda_party_members",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"player_id_fk"}),
        @UniqueConstraint(columnNames = {"party_id_fk", "player_id_fk"})
    }
)
public class RDAPartyMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "party_id_fk", nullable = false)
    private RDAParty party;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "player_id_fk", nullable = false)
    private RDAPlayer playerProfile;

    @Column(name = "joined_at", nullable = false)
    private long joinedAt;

    /**
     * Creates a new membership row.
     *
     * @param party owning party
     * @param playerProfile member profile
     */
    public RDAPartyMember(final @NotNull RDAParty party, final @NotNull RDAPlayer playerProfile) {
        this.party = Objects.requireNonNull(party, "party");
        this.playerProfile = Objects.requireNonNull(playerProfile, "playerProfile");
        this.joinedAt = System.currentTimeMillis();
    }

    /**
     * Constructor reserved for JPA hydration.
     */
    protected RDAPartyMember() {
    }

    /**
     * Returns the stable cache key for this membership row.
     *
     * @return player UUID cache key
     */
    public @NotNull String getCacheKey() {
        return this.playerProfile.getPlayerUuid().toString();
    }

    /**
     * Returns the owning party.
     *
     * @return owning party
     */
    public @NotNull RDAParty getParty() {
        return Objects.requireNonNull(this.party, "party");
    }

    /**
     * Returns the member profile.
     *
     * @return member profile
     */
    public @NotNull RDAPlayer getPlayerProfile() {
        return Objects.requireNonNull(this.playerProfile, "playerProfile");
    }

    /**
     * Returns the join timestamp in epoch milliseconds.
     *
     * @return join timestamp
     */
    public long getJoinedAt() {
        return Math.max(0L, this.joinedAt);
    }
}
