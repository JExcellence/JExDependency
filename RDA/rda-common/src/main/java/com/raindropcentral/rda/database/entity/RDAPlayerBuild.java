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

import com.raindropcentral.rda.ManaDisplayMode;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Persistent build-state row for one RDA player.
 *
 * <p>The row stores unspent points, shared mana state, HUD preferences, and the next moment a
 * manual respec becomes available.</p>
 *
 * @author Codex
 * @since 1.2.0
 * @version 1.2.0
 */
@Entity
@Table(name = "rda_player_builds")
public class RDAPlayerBuild extends BaseEntity {

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "player_id_fk", nullable = false, unique = true)
    private RDAPlayer playerProfile;

    @Column(name = "unspent_points", nullable = false)
    private int unspentPoints;

    @Column(name = "current_mana", nullable = false)
    private double currentMana;

    @Column(name = "mana_display_mode", nullable = false, length = 32)
    private String manaDisplayMode;

    @Column(name = "respec_available_at_epoch_ms", nullable = false)
    private long respecAvailableAtEpochMillis;

    /**
     * Creates a build row for the supplied player profile.
     *
     * @param playerProfile owning player profile
     */
    public RDAPlayerBuild(final @NotNull RDAPlayer playerProfile) {
        this.playerProfile = Objects.requireNonNull(playerProfile, "playerProfile");
        this.manaDisplayMode = ManaDisplayMode.ACTION_BAR.name();
    }

    /**
     * Constructor reserved for JPA hydration.
     */
    protected RDAPlayerBuild() {
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
     * Returns the stable cache key for this build row.
     *
     * @return player UUID string cache key
     */
    public @NotNull String getCacheKey() {
        return this.playerProfile.getPlayerUuid().toString();
    }

    /**
     * Returns the currently unspent build points.
     *
     * @return unspent build points
     */
    public int getUnspentPoints() {
        return this.unspentPoints;
    }

    /**
     * Updates the currently unspent build points.
     *
     * @param unspentPoints replacement unspent-point count
     */
    public void setUnspentPoints(final int unspentPoints) {
        this.unspentPoints = Math.max(0, unspentPoints);
    }

    /**
     * Returns the current shared mana value.
     *
     * @return current mana
     */
    public double getCurrentMana() {
        return this.currentMana;
    }

    /**
     * Updates the current shared mana value.
     *
     * @param currentMana replacement mana value
     */
    public void setCurrentMana(final double currentMana) {
        this.currentMana = Math.max(0.0D, currentMana);
    }

    /**
     * Returns the persisted mana HUD mode.
     *
     * @return persisted mana HUD mode
     */
    public @NotNull String getManaDisplayMode() {
        return this.manaDisplayMode;
    }

    /**
     * Updates the persisted mana HUD mode.
     *
     * @param manaDisplayMode replacement mana HUD mode
     */
    public void setManaDisplayMode(final @NotNull String manaDisplayMode) {
        this.manaDisplayMode = Objects.requireNonNull(manaDisplayMode, "manaDisplayMode");
    }

    /**
     * Returns the next respec-available timestamp in epoch milliseconds.
     *
     * @return next respec-available timestamp
     */
    public long getRespecAvailableAtEpochMillis() {
        return this.respecAvailableAtEpochMillis;
    }

    /**
     * Updates the next respec-available timestamp in epoch milliseconds.
     *
     * @param respecAvailableAtEpochMillis replacement respec timestamp
     */
    public void setRespecAvailableAtEpochMillis(final long respecAvailableAtEpochMillis) {
        this.respecAvailableAtEpochMillis = Math.max(0L, respecAvailableAtEpochMillis);
    }
}
