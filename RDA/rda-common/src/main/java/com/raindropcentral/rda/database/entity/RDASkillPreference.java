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

import com.raindropcentral.rda.ActivationMode;
import com.raindropcentral.rda.SkillType;
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
 * Persistent player preference row for one skill's active-ability trigger mode.
 *
 * @author Codex
 * @since 1.2.0
 * @version 1.2.0
 */
@Entity
@Table(
    name = "rda_skill_preferences",
    uniqueConstraints = @UniqueConstraint(columnNames = {"player_id_fk", "skill_id"})
)
public class RDASkillPreference extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "player_id_fk", nullable = false)
    private RDAPlayer playerProfile;

    @Column(name = "skill_id", nullable = false, length = 32)
    private String skillId;

    @Column(name = "activation_mode", nullable = false, length = 32)
    private String activationMode;

    /**
     * Creates a preference row for the supplied player and skill.
     *
     * @param playerProfile owning player profile
     * @param skillType owning skill
     */
    public RDASkillPreference(final @NotNull RDAPlayer playerProfile, final @NotNull SkillType skillType) {
        this.playerProfile = Objects.requireNonNull(playerProfile, "playerProfile");
        this.skillId = Objects.requireNonNull(skillType, "skillType").getId();
        this.activationMode = ActivationMode.COMMAND.name();
    }

    /**
     * Constructor reserved for JPA hydration.
     */
    protected RDASkillPreference() {
    }

    /**
     * Returns the stable cache key for this preference row.
     *
     * @return player-skill cache key
     */
    public @NotNull String getCacheKey() {
        return this.playerProfile.getPlayerUuid() + ":" + this.skillId;
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
     * Returns the persisted skill identifier.
     *
     * @return persisted skill identifier
     */
    public @NotNull String getSkillId() {
        return this.skillId;
    }

    /**
     * Returns the persisted activation mode.
     *
     * @return persisted activation mode
     */
    public @NotNull String getActivationMode() {
        return this.activationMode;
    }

    /**
     * Updates the persisted activation mode.
     *
     * @param activationMode replacement activation mode
     */
    public void setActivationMode(final @NotNull String activationMode) {
        this.activationMode = Objects.requireNonNull(activationMode, "activationMode");
    }
}
