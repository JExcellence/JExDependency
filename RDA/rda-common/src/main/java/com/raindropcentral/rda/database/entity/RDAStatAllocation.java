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

import com.raindropcentral.rda.CoreStatType;
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
 * Persistent point allocation row for one player and one core stat.
 *
 * @author Codex
 * @since 1.2.0
 * @version 1.2.0
 */
@Entity
@Table(
    name = "rda_stat_allocations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"player_id_fk", "stat_id"})
)
public class RDAStatAllocation extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "player_id_fk", nullable = false)
    private RDAPlayer playerProfile;

    @Column(name = "stat_id", nullable = false, length = 32)
    private String statId;

    @Column(name = "allocated_points", nullable = false)
    private int allocatedPoints;

    /**
     * Creates an allocation row for the supplied player and stat.
     *
     * @param playerProfile owning player profile
     * @param coreStatType allocated stat
     */
    public RDAStatAllocation(final @NotNull RDAPlayer playerProfile, final @NotNull CoreStatType coreStatType) {
        this.playerProfile = Objects.requireNonNull(playerProfile, "playerProfile");
        this.statId = Objects.requireNonNull(coreStatType, "coreStatType").getId();
    }

    /**
     * Constructor reserved for JPA hydration.
     */
    protected RDAStatAllocation() {
    }

    /**
     * Returns the stable cache key for this allocation row.
     *
     * @return player-stat cache key
     */
    public @NotNull String getCacheKey() {
        return this.playerProfile.getPlayerUuid() + ":" + this.statId;
    }

    /**
     * Returns the owning player profile.
     *
     * @return owning player profile
     */
    public @NotNull RDAPlayer getPlayerProfile() {
        return this.playerProfile;
    }

    /**
     * Returns the persisted core-stat identifier.
     *
     * @return persisted stat identifier
     */
    public @NotNull String getStatId() {
        return this.statId;
    }

    /**
     * Returns the allocated point count.
     *
     * @return allocated point count
     */
    public int getAllocatedPoints() {
        return this.allocatedPoints;
    }

    /**
     * Updates the allocated point count.
     *
     * @param allocatedPoints replacement point count
     */
    public void setAllocatedPoints(final int allocatedPoints) {
        this.allocatedPoints = Math.max(0, allocatedPoints);
    }
}
