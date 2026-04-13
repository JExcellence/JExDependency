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
 * Child skill progression row persisted for each player and skill pair.
 *
 * @author Codex
 * @since 1.1.0
 * @version 1.1.0
 */
@Entity
@Table(
    name = "rda_skill_states",
    uniqueConstraints = @UniqueConstraint(columnNames = {"player_id_fk", "skill_id"})
)
public class RDASkillState extends BaseEntity {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "player_id_fk", nullable = false)
    private RDAPlayer playerProfile;

    @Column(name = "skill_id", nullable = false, length = 32)
    private String skillId;

    @Column(name = "skill_xp", nullable = false)
    private long xp;

    @Column(name = "skill_level", nullable = false)
    private int level;

    @Column(name = "skill_prestige", nullable = false)
    private int prestige;

    /**
     * Creates a skill state row for the supplied player and skill.
     *
     * @param playerProfile owning player profile
     * @param skillType owning skill type
     */
    public RDASkillState(final @NotNull RDAPlayer playerProfile, final @NotNull SkillType skillType) {
        this.playerProfile = Objects.requireNonNull(playerProfile, "playerProfile");
        this.skillId = Objects.requireNonNull(skillType, "skillType").getId();
    }

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected RDASkillState() {
    }

    /**
     * Returns the stable cache key for this child state.
     *
     * @return cache key
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
     * Returns the persisted skill id.
     *
     * @return skill id
     */
    public @NotNull String getSkillId() {
        return this.skillId;
    }

    /**
     * Returns the current XP carried inside the current level.
     *
     * @return current level XP
     */
    public long getXp() {
        return this.xp;
    }

    /**
     * Updates the current XP carried inside the current level.
     *
     * @param xp XP to store
     */
    public void setXp(final long xp) {
        this.xp = Math.max(0L, xp);
    }

    /**
     * Returns the internal skill level.
     *
     * @return internal skill level
     */
    public int getLevel() {
        return this.level;
    }

    /**
     * Updates the internal skill level.
     *
     * @param level level to store
     */
    public void setLevel(final int level) {
        this.level = Math.max(0, level);
    }

    /**
     * Returns the completed prestige count.
     *
     * @return completed prestige count
     */
    public int getPrestige() {
        return this.prestige;
    }

    /**
     * Updates the completed prestige count.
     *
     * @param prestige prestige count to store
     */
    public void setPrestige(final int prestige) {
        this.prestige = Math.max(0, prestige);
    }
}
